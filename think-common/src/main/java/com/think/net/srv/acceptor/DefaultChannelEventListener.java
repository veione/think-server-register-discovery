package com.think.net.srv.acceptor;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认通道事件实现
 *
 * @author veione
 */
public class DefaultChannelEventListener implements ChannelEventListener {
    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelEventListener.class);

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        logger.info("客户端已连接: {} {}", remoteAddr, channel);
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        logger.warn("客户端已关闭: {} {}", remoteAddr, channel);
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        logger.warn("客户端通信发生异常: {} {}", remoteAddr, channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        logger.warn("客户端通信空闲: {} {}", remoteAddr, channel);
    }
}
