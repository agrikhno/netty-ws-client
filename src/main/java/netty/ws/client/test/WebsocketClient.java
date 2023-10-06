package netty.ws.client.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelOption.SO_RCVBUF;

public class WebsocketClient {

    private final EventLoopGroup group;
    private final ScheduledExecutorService executorService;
    private final String room;
    private final URI uri;
    private final int port;
    private final String userId;


    public WebsocketClient(EventLoopGroup group, ScheduledExecutorService executorService, String room, URI uri, int port) {
        this.group = group;
        this.executorService = executorService;
        this.room = room;
        this.uri = uri;
        this.port = port;
        this.userId = UUID.randomUUID().toString();
    }

    public void connect() {
        try {
            final DefaultHttpHeaders headers = new DefaultHttpHeaders();
            final WebSocketClientHandler handler = new WebSocketClientHandler(
                    userId,
                    WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, headers),
                    this.room
            );

            final Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.option(SO_RCVBUF, 64 * 1024);
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            final Channel channel = clientBootstrap.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();

            executorService.scheduleAtFixedRate(() -> channel.writeAndFlush(
                            new TextWebSocketFrame(
                                    "{\"type\":\"MESSAGE\", \"source\":{\"id\":\"" + userId + "\", \"msg\":\"" + UUID.randomUUID() + " Traditional Java stacks were engineered for monolithic applications with long startup times and large memory requirements in a world where the cloud, containers, and Kubernetes did not exist. Java frameworks needed to evolve to meet the needs of this new world. Quarkus was created to enable Java developers to create applications for a modern, cloud-native world. Quarkus is a Kubernetes-native Java framework tailored for GraalVM and HotSpot, crafted from best-of-breed Java libraries and standards. The goal is to make Java the leading platform in Kubernetes and serverless environments while offering developers a framework to address a wider range of distributed application architectures.Developers are critical to the success of almost every organization and they need the tools to build cloud-native applications quickly and efficiently. Quarkus provides a frictionless development experience through a combination of tools, libraries, extensions, and more. Quarkus makes developers more efficient with tools to improve the inner loop development cycle while in dev mode.Quarkus was built from the ground up for Kubernetes making it easy to deploy applications without having to understand all of the complexities of the platform. Quarkus allows developers to automatically generate Kubernetes resources including building and deploying container images without having to manually create YAML files.Quarkus is designed to seamlessly combine the familiar imperative style code and the non-blocking, reactive style when developing applications. This is helpful for both Java developers who are used to working with the imperative model and don’t want to switch things up, and those working with a cloud-native/reactive approach. The Quarkus development model can adapt itself to whatever app you’re developingAs much processing as possible is done at build time; thus, your application only contains the classes used at runtime. In traditional frameworks, all the classes required to perform the initial application deployment hang around for the application’s life, even though they are only used once. With Quarkus, they are not even loaded into the production JVM! Quarkus does not stop here. During the build-time processing, it prepares the initialization of all components used by your application. It results in less memory usage and faster startup time as all metadata processing has already been done.\"}}"
                            )
                    ),
                    5000,
                    ThreadLocalRandom.current().nextInt(500, 1000),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
