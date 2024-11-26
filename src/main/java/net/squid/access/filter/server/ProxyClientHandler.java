package net.squid.access.filter.server;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;

public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private final String id;
    private static Logger logger = LoggerFactory.getLogger(ProxyClientHandler.class);
    private Channel clientChannel;
    private Channel remoteChannel;
    protected static final SecureRandom rnd = new SecureRandom();
    protected static final Provider SECURITY_PROVIDER = new BouncyCastleProvider();
    private static HashMap<String, SSLContext> sslCache = new HashMap<>();
    
    static {
    	System.setProperty("javax.net.debug", "ssl");
    }

    private HttpConnectHeader header = new HttpConnectHeader();

    public ProxyClientHandler(String id) {
        this.id = id;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    	logger.debug("Channel active {}", id);
    	clientChannel = ctx.channel();
    	ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	if (header.isComplete()) {
    		// DLP on incoming message?
            remoteChannel.writeAndFlush(msg); // just forward
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        header.digest(in);
                
        if (!header.isComplete()) {
            in.release();
            return;
        }

        logger.info("ChannelRead {} => {}", id, header);
        clientChannel.config().setAutoRead(false); // disable AutoRead until remote connection is ready

        if (header.isTls()) { // if https, respond 200 to create tunnel
            clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection established\r\n\r\n".getBytes()));
            clientChannel.pipeline().addFirst(clientSslHandler(header.getHost()));
        }

        Bootstrap b = new Bootstrap();
        b.group(clientChannel.eventLoop()) // use the same EventLoop
                .channel(clientChannel.getClass())
                .handler(new ProxyTargetHandler(id, clientChannel));
        ChannelFuture f = b.connect(header.getHost(), header.getPort());
        remoteChannel = f.channel();

        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
            	clientChannel.config().setAutoRead(true); // connection is ready, enable AutoRead
                if (!header.isTls()) { // forward header and remaining bytes
                    remoteChannel.write(header.getByteBuf());
                }
                else {
                	remoteChannel.pipeline().addFirst(targetSslHandler(header));
                }
                remoteChannel.writeAndFlush(in);
            } else {
                in.release();
                clientChannel.close();
            }
        });
    }

    private SslHandler targetSslHandler(HttpConnectHeader header) {
    	try {
    		SSLEngine engine = SSLContext.getDefault().createSSLEngine(header.getHost(), header.getPort());
    		engine.setUseClientMode(true);
    		return new SslHandler(engine);
    	}
    	catch (Exception e) {
    		throw new RuntimeException("SSL target error", e);
    	}
	}

    private SslHandler clientSslHandler(String host) {
    	SSLContext ctx = null;
    	synchronized(sslCache) {
    		ctx = sslCache.computeIfAbsent(host, key -> clientSslContext(host));    		
    	}
    	SSLEngine engine = ctx.createSSLEngine();
		engine.setUseClientMode(false);
    	return new SslHandler(engine);
    }
    
	private SSLContext clientSslContext(String host) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);
			AtomicReference<PrivateKey> privateKeyRef = new AtomicReference<>();
			logger.info("Generating TLS cert for "+host);
			X509Certificate cert = generateCertificate(host, privateKeyRef);
			ks.setCertificateEntry(host, cert);
			char[] pwd = randomPassword();
			ks.setKeyEntry(host, privateKeyRef.get(), pwd, new Certificate[] { cert });
			kmf.init(ks, pwd);
			ctx.init(kmf.getKeyManagers(), null, rnd);
			logger.info("TLS cert for "+host+" generated");
			return ctx;
		}
		catch (Exception e) {
    		throw new RuntimeException("SSL server error", e);
    	}
	}
	
	private char[] randomPassword() {
		char[] pwd = new char[rnd.nextInt(10)+16];
		for (int i=0; i < pwd.length; i++) {
			pwd[i] = (char) rnd.nextInt(256);
		}
		return pwd;
	}

	protected X509Certificate generateCertificateWithPrivateKey(String host, PublicKey publicKey, PrivateKey privateKey) throws Exception {
		X500Name cn = new X500Name("cn="+host);
        BigInteger serialNumber = new BigInteger(64, rnd);
        OffsetDateTime now = OffsetDateTime.now();
        Date startDate = Date.from(now.toInstant());
        Date endDate = Date.from(now.plusYears(1).toInstant());
        X509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(cn, serialNumber, 
        	startDate, endDate, cn, publicKey);
        int keyUsageFlags = X509KeyUsage.keyEncipherment | X509KeyUsage.dataEncipherment |
            X509KeyUsage.nonRepudiation | X509KeyUsage.digitalSignature;
        v3CertGen.addExtension(new ASN1ObjectIdentifier("2.5.29.15"), true,
            new X509KeyUsage(keyUsageFlags));
        
        ContentSigner sigGen = new JcaContentSignerBuilder("sha256WithRSAEncryption").build(privateKey);
        X509CertificateHolder certHolder = v3CertGen.build(sigGen);
        return new JcaX509CertificateConverter().setProvider(SECURITY_PROVIDER)
        	.getCertificate(certHolder);
	}
	
	public X509Certificate generateCertificate(String host, AtomicReference<PrivateKey> privateKeyRef) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096, rnd);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();        
        X509Certificate cert = generateCertificateWithPrivateKey(host, keyPair.getPublic(), keyPair.getPrivate());
        privateKeyRef.set(keyPair.getPrivate());
        return cert;
	}	

	@Override
    public void channelInactive(ChannelHandlerContext ctx) {
    	logger.debug("Channel inactive {}", id);
        flushAndClose(remoteChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("Channel exception occured in "+id, e);
        flushAndClose(clientChannel);
    }

    private void flushAndClose(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
