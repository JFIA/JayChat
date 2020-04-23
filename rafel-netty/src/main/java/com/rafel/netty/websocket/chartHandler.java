package com.rafel.netty.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;


// TextWebSocketFrame是在netty中为websocket处理文本的对象，frame是消息的载体
public class chartHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 用于记录和管理所有客户端的channel
    private static ChannelGroup clients=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

        // 接受客户端穿的消息
        String text = textWebSocketFrame.text();
        System.out.println(text);

        for (Channel client : clients) {
            client.writeAndFlush(new TextWebSocketFrame("服务器接收到消息："+ LocalDateTime.now()+"消息为："+text));

        }

    }

    /***
     *
     * @param channelHandlerContext
     * 当客户端连接服务器后，获取客户端channel并且放到channelGroup管理
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {

        clients.add(channelHandlerContext.channel());
    }
}
