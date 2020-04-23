package com.rafel.netty;


import com.rafel.netty.websocket.WSServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
// 需要监听Springboot，当整个容器加载完成，netty就可以启动了
// 监听容器刷新事件
public class NettyBooster implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {

            try {
                WSServer.getInstance().start();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }
}
