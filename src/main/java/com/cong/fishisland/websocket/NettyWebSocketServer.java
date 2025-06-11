package com.cong.fishisland.websocket;

import com.cong.fishisland.websocket.handler.HttpHeadersHandler;
import com.cong.fishisland.websocket.handler.NettyWebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Netty Web 套接字服务器
 *
 * @author cong
 * @date 2023/10/27
 */
@Slf4j
@Configuration
public class NettyWebSocketServer {
    public static final int WEB_SOCKET_PORT = 8090;
    // 创建线程池执行器
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(8);

    /**
     * 启动 ws server
     *
     * @throws InterruptedException 中断异常
     */
    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }

    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        Future<?> future = bossGroup.shutdownGracefully();
        Future<?> future1 = workerGroup.shutdownGracefully();
        future.syncUninterruptibly();
        future1.syncUninterruptibly();
        log.info("关闭 ws server 成功");
    }

    /**
     * 启动
     *
     * @throws InterruptedException 中断异常
     */
    public void run() throws InterruptedException {
        // 服务器启动引导对象
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 为 bossGroup 添加 日志处理器
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //30秒客户端没有向服务器发送心跳则关闭连接
                        pipeline.addLast(new IdleStateHandler(30, 0, 0));
                        // 因为使用http协议，所以需要使用http的编码器，解码器
                        pipeline.addLast(new HttpServerCodec());
                        // 以块方式写，添加 chunkedWriter 处理器 支持大数据流
                        pipeline.addLast(new ChunkedWriteHandler());
                        //对http消息做聚合操作，产生两个对象 FullHttpRequest、FullHttpResponse
                        pipeline.addLast(new HttpObjectAggregator(8192));
                        //保存用户ip
                        pipeline.addLast(new HttpHeadersHandler());
                        //websocket
                        pipeline.addLast(new WebSocketServerProtocolHandler("/"));
                        // 自定义handler ，处理业务逻辑
                        pipeline.addLast(new NettyWebSocketServerHandler());
                    }
                });
        // 启动服务器，监听端口，阻塞直到启动成功
        serverBootstrap.bind(WEB_SOCKET_PORT).sync();
        log.info("Netty启动成功");
    }

}
