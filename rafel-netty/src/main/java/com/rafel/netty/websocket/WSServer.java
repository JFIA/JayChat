package com.rafel.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

@Component
public class WSServer {

    private static class SingletonWSServer {
        private static final WSServer instance = new WSServer();
    }

    public static WSServer getInstance() {
        return SingletonWSServer.instance;
    }

    private EventLoopGroup fatherGroup;
    private EventLoopGroup childGroup;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture channelFuture;

    public WSServer() {

        fatherGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(fatherGroup, childGroup).childHandler(new WSServerInit()).channel(NioServerSocketChannel.class);
    }

    public void start(){

        this.channelFuture=serverBootstrap.bind(8089);

        System.err.println("netty start");

    }

}
