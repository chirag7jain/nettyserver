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
    private Worker worker;
    private Thread thread;

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
        this.worker = new Worker();
        this.thread = new Thread(worker);
    }

    /**
     * Starts the server
     */
    public void startServer() {
        this.thread.start();
    }

    public void stopServer() {
        this.worker.stopServer();
    }

    private class Worker implements Runnable {
        private EventLoopGroup group;
        private ChannelFuture channelFuture;

        @Override
        public void run() {
            this.group = new NioEventLoopGroup(numThreads);

            try {
                ServerBootstrap serverBootstrap;
                RequestHandler requestHandler;

                serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(group);
                serverBootstrap.channel(NioServerSocketChannel.class);
                serverBootstrap.localAddress(new InetSocketAddress("::", port));

                requestHandler = new RequestHandler(responseManager, logger);

                serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(requestHandler);
                    }
                });

                this.channelFuture = serverBootstrap.bind().sync();
                this.channelFuture.channel().closeFuture().sync();
            }
            catch(Exception e){
                logger.info(String.format("Unknown failure %s", e.getMessage()));
            }
            finally {
                try {
                    this.group.shutdownGracefully().sync();
                }
                catch (InterruptedException e) {
                    logger.info(String.format("Error shutting down %s", e.getMessage()));
                }

            }
        }

        private void stopServer() {
            this.group.shutdownGracefully();
            this.channelFuture.channel().close();
        }
    }
}
