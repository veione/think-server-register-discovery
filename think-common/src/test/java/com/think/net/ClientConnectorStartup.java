package com.think.net;

import com.think.net.User;
import com.think.net.client.connector.DefaultCommonClientConnector;
import com.think.net.common.Message;
import com.think.net.srv.acceptor.DefaultCommonSrvAcceptor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.think.net.common.NettyCommonProtocol.REQUEST;

/**
 * @author BazingaLyn
 * @description 客户链接端 启动类
 * @time 2016年7月22日14:53:32
 * @modifytime
 */
public class ClientConnectorStartup {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCommonSrvAcceptor.class);

    public static void main(String[] args) {

        DefaultCommonClientConnector clientConnector = new DefaultCommonClientConnector();
        Channel channel = clientConnector.connect(20011, "127.0.0.1");
        User user = new User(1, "dubbo");
        Message message = new Message();
        message.sign(REQUEST);
        message.data(user);
        //获取到channel发送双方规定的message格式的信息
        channel.writeAndFlush(message).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.info("send fail,reason is {}", future.cause().getMessage());
                }
            }
        });
        //防止对象处理发生异常的情况
        DefaultCommonClientConnector.MessageNonAck msgNonAck = new DefaultCommonClientConnector.MessageNonAck(message, channel);
        clientConnector.addNeedAckMessageInfo(msgNonAck);
    }

}
