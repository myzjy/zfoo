package com.zfoo.net.core.gateway;

import com.zfoo.net.core.AbstractServer;
import com.zfoo.net.handler.GatewayRouteHandler;
import com.zfoo.net.handler.codec.json.JsonWebSocketCodecHandler;
import com.zfoo.net.handler.codec.websocket.WebSocketCodecHandler;
import com.zfoo.net.handler.idle.ServerIdleHandler;
import com.zfoo.net.session.Session;
import com.zfoo.protocol.IPacket;
import com.zfoo.protocol.util.IOUtils;
import com.zfoo.util.net.HostAndPort;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.lang.Nullable;

import java.util.function.BiFunction;

/**
 * @author zjy
 * @version 1.0
 * @since 2023/4/19 17 32
 */
public class JsonWebsocketGatewayServer extends AbstractServer<SocketChannel> {
    private BiFunction<Session, IPacket, Boolean> packetFilter;

    public JsonWebsocketGatewayServer(HostAndPort host,@Nullable BiFunction<Session, IPacket, Boolean> packetFilter) {
        super(host);
        this.packetFilter = packetFilter;
    }
    @Override
    protected void initChannel(SocketChannel channel) {
        // 编解码 http 请求
        channel.pipeline().addLast(new HttpServerCodec(8 * IOUtils.BYTES_PER_KB, 16 * IOUtils.BYTES_PER_KB, 16 * IOUtils.BYTES_PER_KB));
        // 聚合解码 HttpRequest/HttpContent/LastHttpContent 到 FullHttpRequest
        // 保证接收的 Http 请求的完整性
        channel.pipeline().addLast(new HttpObjectAggregator(16 * IOUtils.BYTES_PER_MB));
        // 处理其他的 WebSocketFrame
        channel.pipeline().addLast(new WebSocketServerProtocolHandler("/websocket"));
        // 写文件内容，支持异步发送大的码流，一般用于发送文件流
        channel.pipeline().addLast(new ChunkedWriteHandler());
        // 编解码WebSocketFrame二进制协议
        channel.pipeline().addLast(new JsonWebSocketCodecHandler());
        channel.pipeline().addLast(new GatewayRouteHandler(packetFilter));
    }
}
