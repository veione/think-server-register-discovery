package com.think.gate.tcp.logic;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * 逻辑服通道工厂
 *
 * @author veione
 */
public class LogicServerChannelFactory extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(4, 4, 4, 4, 4));
        pipeline.addLast(new LengthFieldPrepender(2));

    }
}
