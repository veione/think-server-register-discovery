package com.think.net.srv.acceptor;

import io.netty.channel.Channel;

/**
 * 链接事件监听器接口
 *
 * @author veione
 */
public interface ChannelEventListener {
    void onChannelConnect(final String remoteAddr, final Channel channel);

    void onChannelClose(final String remoteAddr, final Channel channel);


    void onChannelException(final String remoteAddr, final Channel channel);


    void onChannelIdle(final String remoteAddr, final Channel channel);
}
