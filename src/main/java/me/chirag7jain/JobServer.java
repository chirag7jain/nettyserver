package me.chirag7jain;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.chirag7jain.Response.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

import me.chirag7jain.Response.ResponseManager;

public class JobServer {
    private int port;
    private ResponseManager responseManager;
    private Logger logger;
    private int numThreads;

    /**
     * @param port - Server port
     * @param numThreads - No of threads for Worker
     * @param responseManager - Response Manager
     */
    public JobServer(int port, int numThreads, ResponseManager responseManager) {
        this.port = port;
        this.numThreads = numThreads;
        this.responseManager = responseManager;
        this.logger = LogManager.getLogger();
    }

    /**
     * Starts the server
     */
    public void startServer() {
        EventLoopGroup group;

        group = new NioEventLoopGroup(this.numThreads);

        try {
            ServerBootstrap serverBootstrap;
            RequestHandler requestHandler;
            ChannelFuture channelFuture;

            serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(group);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.localAddress(new InetSocketAddress("::", this.port));

            requestHandler = new RequestHandler(this.responseManager, this.logger);

            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(requestHandler);
                }
            });

            channelFuture = serverBootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
        }
        catch(Exception e){
            this.logger.info(String.format("Unknown failure %s", e.getMessage()));
        }
        finally {
            try {
                group.shutdownGracefully().sync();
            }
            catch (InterruptedException e) {
                this.logger.info(String.format("Error shutting down %s", e.getMessage()));
            }

        }

    }


}
