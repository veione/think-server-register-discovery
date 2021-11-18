package com.think.gate.tcp;

import com.think.net.IServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * TCP服务器
 *
 * @author veione
 */
public class TcpServer extends Thread implements IServer {
    private final int port;
    private final ChannelInitializer<SocketChannel> initializer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TcpServer(int port, ChannelInitializer<SocketChannel> initializer) {
        this.port = port;
        this.initializer = initializer;
    }

    @Override
    public void startServer() {
        start();
    }


    @Override
    public void run() {
        ServerBootstrap b = new ServerBootstrap();
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class)
                .childHandler(initializer);

        Channel channel = b.bind(this.port).channel();
        channel.closeFuture().syncUninterruptibly();
    }

    @Override
    public void stopServer() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
