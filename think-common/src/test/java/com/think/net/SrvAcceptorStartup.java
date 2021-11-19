package com.think.net;

import com.think.net.srv.acceptor.DefaultCommonSrvAcceptor;

public class SrvAcceptorStartup {

    public static void main(String[] args) throws InterruptedException {

        DefaultCommonSrvAcceptor defaultCommonSrvAcceptor = new DefaultCommonSrvAcceptor("server", 20011, null);
        defaultCommonSrvAcceptor.start();

    }

}
