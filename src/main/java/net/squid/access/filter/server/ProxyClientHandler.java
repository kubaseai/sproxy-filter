package net.squid.access.filter.server;

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

public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private final String id;
    private static Logger logger = LoggerFactory.getLogger(ProxyClientHandler.class);
    private Channel clientChannel;
    private Channel remoteChannel;

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

        if (header.isHttps()) { // if https, respond 200 to create tunnel
            clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection established\r\n\r\n".getBytes()));
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
                if (!header.isHttps()) { // forward header and remaining bytes
                    remoteChannel.write(header.getByteBuf());
                }
                remoteChannel.writeAndFlush(in);
            } else {
                in.release();
                clientChannel.close();
            }
        });
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
