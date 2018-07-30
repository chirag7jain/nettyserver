package me.chirag7jain.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class RequestHandler extends ChannelInboundHandlerAdapter {
    private final ResponseManager responseManager;
    private final Logger logger;
    private final AttributeKey<StringBuilder> dataKey;

    public RequestHandler(ResponseManager responseManager, Logger logger) {
        this.responseManager = responseManager;
        this.logger = logger;
        this.dataKey = AttributeKey.valueOf("dataBuffer");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf;
        String hostAddress;
        StringBuilder dataBuffer;
        boolean allocBuf;

        dataBuffer = ctx.attr(dataKey).get();
        allocBuf = dataBuffer == null;

        if (allocBuf) {
            dataBuffer = new StringBuilder();
        }

        byteBuf = (ByteBuf) msg;
        dataBuffer.append(byteBuf.toString(CharsetUtil.UTF_8));
        hostAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        if (byteBuf.readableBytes() < 0) {
            this.logger.info(String.format("Data received from %s", hostAddress));
        }

        if (allocBuf) {
            ctx.attr(dataKey).set(dataBuffer);
        }

        ctx.channel().read();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        String data;

        data = ctx.attr(dataKey).get().toString();

        if (data.length() > 0) {
            String reply, hostAddress;

            hostAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            logger.info(String.format("Data received %s from %s", data, hostAddress));
            reply = this.responseManager.reply(data);

            if (reply != null) {
                ctx.writeAndFlush(Unpooled.copiedBuffer(reply, CharsetUtil.UTF_8));
            }
        }

        ctx.attr(dataKey).set(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.logger.info(String.format("Received Exception %s", cause.getMessage()));
        ctx.close();
    }

}
