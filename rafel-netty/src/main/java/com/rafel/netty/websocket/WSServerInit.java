package com.rafel.netty.websocket;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WSServerInit extends ChannelInitializer<SocketChannel> {


    protected void initChannel(SocketChannel socketChannel) throws Exception {

        ChannelPipeline pipeline = socketChannel.pipeline();

        // websocket基于http协议
        pipeline.addLast(new HttpServerCodec());

        // 对写大数据流的支持
        pipeline.addLast(new ChunkedWriteHandler());

        // 对httpMessage进行聚合，聚合成FullHttpRequest或response，几乎在netty编程中都会使用到
        pipeline.addLast(new HttpObjectAggregator(1024 * 64));

        // ===========以上是用于支持http协议=================== //

        // websocket服务器处理的协议，用于指定给客户端连接访问的路由/ws
        // 本handler会处理一些复杂的事，比如握手工作 pingpong心跳，对于websocket来讲都是以frames传输
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        // 自定义助手类，读取用户消息并且对用户的消息进行相应的处理，处理完毕发给相应的客户端
        pipeline.addLast("ChartHandler", new chartHandler());

    }
}
