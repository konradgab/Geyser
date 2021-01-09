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
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.handler.TestServerEventHandler;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.geysermc.helper.TestHelper.startBedrockClient;

public class PerformanceTest {
    private static final int WARM_UP_ITERATIONS = 10;
    private static final int TEST_ITERATIONS = 10;

    private final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Map<BedrockPacket, List<Long>> warmUpDirectClientConnectionTimes = new HashMap<>();
    private final Map<BedrockPacket, List<Long>> directClientConnectionTimes = new HashMap<>();
    private final Map<BedrockPacket, List<Long>> geyserClientConnectionTimes = new HashMap<>();

    private final Map<BedrockPacket, Long> clientPackets = new LinkedHashMap<>();

    @Before
    public void setUp() {

    }

    @Test
    public void directClientConnection() throws Exception {
        Map<BedrockPacket, Long> sendPacket = new HashMap<>();

        TextPacket example = new TextPacket();
        example.setXuid("0");
        example.setSourceName("Test");
        example.setMessage("Test");
        example.setType(TextPacket.Type.CHAT);

        directClientConnectionTimes.put(example, new ArrayList<>());
        warmUpDirectClientConnectionTimes.put(example, new ArrayList<>());

        // WARM UP
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {

            BedrockServer server = new BedrockServer(new InetSocketAddress("0.0.0.0", 19132));
            server.setHandler(new TestServerEventHandler(sendPacket, warmUpDirectClientConnectionTimes));

            server.bind().join();

            BedrockClient client = startBedrockClient();

            InetSocketAddress connectionAddress = new InetSocketAddress("127.0.0.1", 19132);
            client.connect(connectionAddress).join().setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
            client.getSession().setLogging(false);

            while (server.getRakNet().getSessionCount() != 1) {
                Thread.sleep(200);
            }

//        for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
//            sendPacket.put(entry.getKey(), System.currentTimeMillis());
//            client.getSession().sendPacket(entry.getKey());
//
//            Thread.sleep(entry.getValue());
//        }

            sendPacket.put(example, System.currentTimeMillis());

            client.getSession().sendPacketImmediately(example);

            while (!sendPacket.isEmpty()) {
                Thread.sleep(100);
            }

            client.close();
            server.close();
        }

        sendPacket.clear();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BedrockServer server = new BedrockServer(new InetSocketAddress("0.0.0.0", 19132));
            server.setHandler(new TestServerEventHandler(sendPacket, directClientConnectionTimes));

            server.bind().join();

            BedrockClient client = startBedrockClient();

            InetSocketAddress connectionAddress = new InetSocketAddress("127.0.0.1", 19132);
            client.connect(connectionAddress).join().setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
            client.getSession().setLogging(false);

            while (server.getRakNet().getSessionCount() != 1) {
                Thread.sleep(200);
            }

//        for (Map.Entry<BedrockPacket, Long> entry : clientPackets.entrySet()) {
//            sendPacket.put(entry.getKey(), System.currentTimeMillis());
//            client.getSession().sendPacket(entry.getKey());
//
//            Thread.sleep(entry.getValue());
//        }

            sendPacket.put(example, System.currentTimeMillis());

            client.getSession().sendPacketImmediately(example);

            while (!sendPacket.isEmpty()) {
                Thread.sleep(100);
            }

            client.close();
            server.close();
        }

        System.out.println(warmUpDirectClientConnectionTimes);
        System.out.println(directClientConnectionTimes);
    }


}
