package netty.ws.client.test;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketClientFactory {

    public static void main(String[] args) throws Exception {
        final URI uri = new URI("ws://127.0.0.1:8080/wstest");
        final int port = 8080;

        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
        final EventLoopGroup group = new NioEventLoopGroup();
        final List<WebsocketClient> websocketClientList = new ArrayList<>();

        /* TEST CASE VARS
         *
         * roomCount - total room count in test
         * clientsPerRoom - number of clients connected the room in the next step
         * nextClientPeriodInMs - time interval after which the next {clientsPerRoom} clients will connect
         *
         */
        final int roomCount = 10;
        final int clientsPerRoom = 5;
        final int nextClientPeriodInMs = 15000;

        final AtomicLong atomicLong = new AtomicLong();

        executorService.scheduleAtFixedRate(() -> {
                    for (int i = 1; i <= roomCount; i++) {
                        for (int k = 0; k < clientsPerRoom; k++) {
                            final WebsocketClient websocketClient = new WebsocketClient(group, executorService, "room_" + i, uri, port);
                            websocketClientList.add(websocketClient);
                            websocketClient.connect();
                        }
                    }

                    final long totalClientsPerRoom = atomicLong.incrementAndGet() * clientsPerRoom;
                    System.out.println(LocalDateTime.now() + " Clients per room connected: " + totalClientsPerRoom +  " Total clients connected: " + roomCount * totalClientsPerRoom);
                },
                0,
                nextClientPeriodInMs,
                TimeUnit.MILLISECONDS);
    }
}
