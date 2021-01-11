/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.packetlib.Server;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.AuthData;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.platform.standalone.GeyserStandaloneBootstrap;
import org.geysermc.util.adapter.PerformanceServerAdapter;
import org.geysermc.util.handler.TestServerEventHandler;
import org.geysermc.util.runnable.TestSpigotRunnable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.geysermc.util.helper.TestHelper.startBedrockClient;
import static org.geysermc.util.helper.TestHelper.startGeyser;
import static org.geysermc.util.helper.TestHelper.startJavaServer;

public class PerformanceTest {
    private final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final int WARM_UP_ITERATIONS = 1;
    private static final int TEST_ITERATIONS = 5;

    private final List<Long> warmUpDirectClientConnectionTimes = new ArrayList<>();
    private final List<Long> warmUpConnectionViaGeyserTimes = new ArrayList<>();
    private final List<Long> directClientConnectionTimes = new ArrayList<>();
    private final List<Long> connectionViaGeyserTimes = new ArrayList<>();

    private static Map<BedrockPacket, Long> clientPackets = new LinkedHashMap<>();

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void setUp() throws InterruptedException {
        TestSpigotRunnable runnable = new TestSpigotRunnable();
        Thread spigotThread = new Thread(runnable, "spigot");
        spigotThread.start();

        new Thread(() -> GeyserStandaloneBootstrap.main(new String[]{"--nogui"}), "geyser").start();

        while (GeyserConnector.getInstance() == null) {
            Thread.sleep(1000);
        }

        while (GeyserConnector.getInstance().getPlayers().size() != 1) {
            Thread.sleep(1000);
        }

        while (GeyserConnector.getInstance().getPlayers().size() == 1) {
            Thread.sleep(1000);
        }

        GeyserConnector.getInstance().shutdown();

        try {
            runnable.getWriter().write("stop\n");
            runnable.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(runnable.isWorking()) {
            Thread.sleep(200);
        }

        clientPackets = new LinkedHashMap<>(PacketTranslatorRegistry.clientPackets);

        System.out.println(clientPackets.size());
    }

    @Test
    public void directClientConnection() throws Exception {
        BedrockServer server = new BedrockServer(new InetSocketAddress("0.0.0.0", 19132));
        TestServerEventHandler handler = new TestServerEventHandler();
        server.setHandler(handler);

        server.bind().join();

        BedrockClient client = startBedrockClient();

        InetSocketAddress connectionAddress = new InetSocketAddress("127.0.0.1", 19132);
        client.connect(connectionAddress).join().setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
        client.getSession().setLogging(false);

        while (server.getRakNet().getSessionCount() != 1) {
            Thread.sleep(10);
        }

        // WARM UP
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            System.out.println("Warm-up " + i);

            long start = System.nanoTime();

            long counter = 0;

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                counter++;
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }

            while (counter != handler.getPacketHandler().getCounter()) {

            }

            long end = System.nanoTime();

            warmUpDirectClientConnectionTimes.add(end - start);
        }

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            System.out.println(i);

            long start = System.nanoTime();

            long counter = 0;

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                counter++;
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }

            while (counter != handler.getPacketHandler().getCounter()) {

            }

            long end = System.nanoTime();

            directClientConnectionTimes.add(end - start);
        }

        client.close();
        server.close();

        System.out.println(warmUpDirectClientConnectionTimes.stream().mapToLong(Long::longValue).average());
        System.out.println(directClientConnectionTimes.stream().mapToLong(Long::longValue).average());
    }

    @Test
    public void connectionViaGeyser() throws IOException, InterruptedException {
        Server javaServer = startJavaServer();

        PerformanceServerAdapter adapter = new PerformanceServerAdapter();

        javaServer.addListener(adapter);

        javaServer.bind();

        AtomicReference<GeyserSession> session = new AtomicReference<>();

        GeyserConnector connector = startGeyser(session);

        BedrockClient client = startBedrockClient();

        InetSocketAddress connectionAddress = new InetSocketAddress("127.0.0.1", 19132);
        client.connect(connectionAddress).join().setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
        client.getSession().setLogging(false);

        while (session.get() == null) {
            Thread.sleep(200);
        }

        session.get().setAuthData(new AuthData("TestSession", UUID.randomUUID(), "0"));
        session.get().setClientData(JSON_MAPPER.readValue("{\"LanguageCode\":\"en_us\", \"DeviceOS\": \"ANDROID\"}", BedrockClientData.class));

        ResourcePackClientResponsePacket packet1 = new ResourcePackClientResponsePacket();
        packet1.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        client.getSession().sendPacket(packet1);


        while (session.get().getRemoteServer() == null) {
            Thread.sleep(200);
        }

        session.get().authenticate("Test");

        while (!connector.getPlayers().contains(session.get())) {
            Thread.sleep(200);
        }

        // WARM UP
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            System.out.println("Warm-up " + i);

            long start = System.nanoTime();

            long counter = 0;

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                counter++;
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }

            while (counter != adapter.getCounter()) {

            }

            long end = System.nanoTime();

            warmUpConnectionViaGeyserTimes.add(end - start);

        }

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            System.out.println(i);

            long start = System.nanoTime();

            long counter = 0;

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                counter++;
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }


            while (counter != adapter.getCounter()) {

            }

            long end = System.nanoTime();

            connectionViaGeyserTimes.add(end - start);
        }

        client.close();
        javaServer.close();
        connector.shutdown();

        System.out.println(warmUpConnectionViaGeyserTimes.stream().mapToLong(Long::longValue).average());
        System.out.println(connectionViaGeyserTimes.stream().mapToLong(Long::longValue).average());
    }
}

