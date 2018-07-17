package me.chirag7jain.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class RequestHandler extends ChannelInboundHandlerAdapter {
    private ResponseManager responseManager;
    private Logger logger;

    public RequestHandler(ResponseManager responseManager, Logger logger) {
        this.responseManager = responseManager;
        this.logger = logger;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf;
        String data, hostAddress;

        byteBuf = (ByteBuf) msg;
        data = byteBuf.toString(CharsetUtil.UTF_8);
        hostAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        if (!data.isEmpty()) {
            String reply;

            this.logger.info(String.format("Data received %s from %s", data, hostAddress));
            reply = this.responseManager.reply(data);

            if (reply != null) {
                ctx.write(Unpooled.copiedBuffer(reply, CharsetUtil.UTF_8));
            }
        }
        else {
            logger.info(String.format("NO Data received from %s", hostAddress));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.logger.info(String.format("Received Exception %s", cause.getMessage()));
        ctx.close();
    }

}
