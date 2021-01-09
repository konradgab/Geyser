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

package org.geysermc.utils.helper;

import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.nukkitx.protocol.bedrock.BedrockClient;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserJacksonConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.utils.mock.TestConfiguration;
import org.geysermc.utils.mock.TestLogger;
import org.geysermc.utils.mock.TestConnectorServerEventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHelper {

    public static Server startJavaServer() {
        SessionService sessionService = new SessionService();
        sessionService.setProxy(Proxy.NO_PROXY);

        Server server = new Server("0.0.0.0", 25565, MinecraftProtocol.class, new TcpSessionFactory());
        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);

        return server;
    }

    public static GeyserConnector startGeyser(AtomicReference<GeyserSession> session) throws IOException, InterruptedException {
        GeyserJacksonConfiguration configuration = new TestConfiguration();
        configuration.getRemote().setAddress("127.0.0.1");

        CommandManager commandManager = mock(CommandManager.class);
        GeyserLogger logger = new TestLogger();

        GeyserBootstrap bootstrap = mock(GeyserBootstrap.class, CALLS_REAL_METHODS);
        when(bootstrap.getGeyserConfig()).thenReturn(configuration);
        when(bootstrap.getGeyserLogger()).thenReturn(logger);
        when(bootstrap.getGeyserCommandManager()).thenReturn(commandManager);

        Path testPath = Paths.get("testData");
        Files.createDirectories(testPath);
        when(bootstrap.getConfigFolder()).thenReturn(testPath);

        GeyserConnector connector = GeyserConnector.start(PlatformType.STANDALONE, bootstrap);

        while (connector.getMetrics() == null) {
            Thread.sleep(1000);
        }

        IGeyserPingPassthrough pingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        when(connector.getBootstrap().getGeyserPingPassthrough()).thenReturn(pingPassthrough);

        TestConnectorServerEventHandler testConnectorServerEventHandler = new TestConnectorServerEventHandler(connector, session::set);
        connector.getBedrockServer().setHandler(testConnectorServerEventHandler);

        return connector;
    }

    public static BedrockClient startBedrockClient() {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", ThreadLocalRandom.current().nextInt(20000, 60000));
        BedrockClient client = new BedrockClient(address);

        client.bind().join();

        return client;
    }

}
