package net.squid.access.filter.server;

import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.squid.access.filter.entities.Config;

@Component
public class ProxyServer implements Runnable {
	
	private final ProxyChannelInitializer channelInitializer;
	private final Config cfg;
	
	public ProxyServer(ProxyChannelInitializer chInitializer, Config config) {
		this.channelInitializer = chInitializer;
		this.cfg = config;
		if (cfg.getServerPort() > 0) {
			new Thread(this).start();
		}
	}

	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(channelInitializer)
             .childOption(ChannelOption.AUTO_READ, false)
             .bind(cfg.getServerPort()).sync().channel().closeFuture().sync();
        }
        catch (InterruptedException ie) {}
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
	}
}
