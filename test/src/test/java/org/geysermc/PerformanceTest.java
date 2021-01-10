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
import com.nukkitx.protocol.bedrock.Bedrock;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.platform.standalone.GeyserStandaloneBootstrap;
import org.geysermc.util.handler.TestServerEventHandler;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.geysermc.util.helper.TestHelper.startBedrockClient;

public class PerformanceTest {
    private static final int WARM_UP_ITERATIONS = 3;
    private static final int TEST_ITERATIONS = 10;

    private final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Map<BedrockPacket, List<Long>> warmUpDirectClientConnectionTimes = new HashMap<>();
    private final Map<BedrockPacket, List<Long>> directClientConnectionTimes = new HashMap<>();
    private final Map<BedrockPacket, List<Long>> warmUpGeyserClientConnectionTimes = new HashMap<>();
    private final Map<BedrockPacket, List<Long>> geyserClientConnectionTimes = new HashMap<>();

    private Map<BedrockPacket, Long> clientPackets = new LinkedHashMap<>();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws InterruptedException {
        SpigotRunnable runnable = new SpigotRunnable();
        Thread spigotThread = new Thread(runnable, "spigot");
        spigotThread.start();

        new Thread(() -> GeyserStandaloneBootstrap.main(new String[] {"--nogui"}), "geyser").start();

        while (GeyserConnector.getInstance() == null) {
            Thread.sleep(1000);
        }

        while(GeyserConnector.getInstance().getPlayers().size() != 1) {
            Thread.sleep(1000);
        }

        while(GeyserConnector.getInstance().getPlayers().size() == 1) {
            Thread.sleep(1000);
        }

        GeyserConnector.getInstance().shutdown();

        try {
            runnable.getWriter().write("stop\n");
            runnable.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        clientPackets = PacketTranslatorRegistry.clientPackets;

        for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
            warmUpDirectClientConnectionTimes.put(entry.getKey(), new ArrayList<>());
            directClientConnectionTimes.put(entry.getKey(), new ArrayList<>());
        }

        System.out.println(clientPackets);
    }

    @Test
    public void directClientConnection() throws Exception {
        Map<BedrockPacket, Long> sendPacket = new HashMap<>();

        BedrockServer server = new BedrockServer(new InetSocketAddress("0.0.0.0", 19132));
        server.setHandler(new TestServerEventHandler(sendPacket, warmUpDirectClientConnectionTimes));

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
            System.out.println("Warmpup " + i);

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                sendPacket.put(entry.getKey(), System.currentTimeMillis());
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }

            while (!sendPacket.isEmpty()) {
                Thread.sleep(10);
            }
        }

        server.setHandler(new TestServerEventHandler(sendPacket, directClientConnectionTimes));

        client.close();

        client = startBedrockClient();
        client.connect(connectionAddress).join().setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
        client.getSession().setLogging(false);

        while (server.getRakNet().getSessionCount() != 1) {
            Thread.sleep(10);
        }

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            System.out.println(i);

            for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
                sendPacket.put(entry.getKey(), System.currentTimeMillis());
                client.getSession().sendPacket(entry.getKey());

                Thread.sleep(entry.getValue());
            }

            while (!sendPacket.isEmpty()) {
                Thread.sleep(10);
            }
        }

        client.close();
        server.close();

        System.out.println(warmUpDirectClientConnectionTimes);
        System.out.println(directClientConnectionTimes);
    }
}

@Getter
class SpigotRunnable implements Runnable {
    private BufferedWriter writer;

    @Override
    public void run() {
        try {
            Process proc = Runtime.getRuntime().exec("java -jar paper-1.16.4.jar nogui", null, new File("/Users/extollite/Documents/GitHub/Geyser-test/test/spigot"));
            writer =  new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            new BufferedReader(new InputStreamReader(proc.getInputStream())).lines().forEach(s -> System.out.println("[SPIGOT] "+s));
            proc.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
