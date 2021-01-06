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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.nukkitx.protocol.bedrock.BedrockClient;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.configuration.GeyserJacksonConfiguration;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegrationTest {

    @Test
    public void passFromClientToServer() {
        InetSocketAddress address = new InetSocketAddress(0);
        BedrockClient client = new BedrockClient(address);

        startJavaServer();

        GeyserBootstrap bootstrap = mock(GeyserBootstrap.class);
        GeyserLogger logger = new TestLogger();
        GeyserJacksonConfiguration configuration = mock(GeyserJacksonConfiguration.class, CALLS_REAL_METHODS);

        when(bootstrap.getGeyserConfig()).thenReturn(configuration);
        when(bootstrap.getGeyserLogger()).thenReturn(logger);

        GeyserConnector connector = GeyserConnector.start(PlatformType.STANDALONE, bootstrap);

        client.bind().join();
        client.connect(connector.getBedrockServer().getBindAddress());
    }

    private void startJavaServer() {
        SessionService sessionService = new SessionService();
        sessionService.setProxy(Proxy.NO_PROXY);

        Server server = new Server("127.0.0.1", 25565, MinecraftProtocol.class, new TcpSessionFactory());
        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(
                new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                new PlayerInfo(100, 0, new GameProfile[0]),
                Component.text("Hello world!"),
                null
        ));

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);
        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                System.out.println("Server closed.");
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(PacketReceivedEvent event) {
                        if(event.getPacket() instanceof ClientChatPacket) {
                            ClientChatPacket packet = event.getPacket();
                            GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
                            System.out.println(profile.getName() + ": " + packet.getMessage());

                            Component msg = Component.text("Hello, ")
                                    .color(NamedTextColor.GREEN)
                                    .append(Component.text(profile.getName())
                                            .color(NamedTextColor.AQUA)
                                            .decorate(TextDecoration.UNDERLINED))
                                    .append(Component.text("!")
                                            .color(NamedTextColor.GREEN));

                            event.getSession().send(new ServerChatPacket(msg));
                        }
                    }
                });
            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
                MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
                if(protocol.getSubProtocol() == SubProtocol.GAME) {
                    System.out.println("Closing server.");
                    event.getServer().close(false);
                }
            }
        });

        server.bind();
    }

    static class TestLogger implements GeyserLogger, CommandSender {
        private boolean colored = true;

        @Override
        public void severe(String message) {
            System.err.printf(printConsole(ChatColor.DARK_RED + message, colored));
        }

        @Override
        public void severe(String message, Throwable error) {
            System.err.printf(printConsole(ChatColor.DARK_RED + message, colored), error);
        }

        @Override
        public void error(String message) {
            System.err.println(printConsole(ChatColor.RED + message, colored));
        }

        @Override
        public void error(String message, Throwable error) {
            System.err.printf(printConsole(ChatColor.RED + message, colored), error);
        }

        @Override
        public void warning(String message) {
            System.err.println(printConsole(ChatColor.YELLOW + message, colored));
        }

        @Override
        public void info(String message) {
            System.err.println(printConsole(ChatColor.RESET + ChatColor.BOLD + message, colored));
        }

        @Override
        public void debug(String message) {
            System.err.println(printConsole(ChatColor.GRAY + message, colored));
        }

        public static String printConsole(String message, boolean colors) {
            return colors ? ChatColor.toANSI(message + ChatColor.RESET) : ChatColor.stripColors(message + ChatColor.RESET);
        }

        @Override
        public void setDebug(boolean debug) {

        }

        public boolean isDebug() {
            return true;
        }

        @Override
        public String getName() {
            return "CONSOLE";
        }

        @Override
        public void sendMessage(String message) {
            info(message);
        }

        @Override
        public boolean isConsole() {
            return true;
        }
    }


}
