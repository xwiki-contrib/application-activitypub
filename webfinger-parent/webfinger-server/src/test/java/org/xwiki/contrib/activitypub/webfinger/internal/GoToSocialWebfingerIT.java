/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.activitypub.webfinger.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.contrib.activitypub.webfinger.entities.Link;
import org.xwiki.contrib.activitypub.webfinger.internal.json.DefaultWebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.internal.json.ObjectMapperConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Federation interoperability test against a real, containerized <a href="https://gotosocial.org">GoToSocial</a>
 * server. It exercises the actual {@link DefaultWebfingerClient} (HTTP + real JSON parsing) against the live server,
 * so it fails when GoToSocial changes its WebFinger contract in a way that breaks discovery. The server tag is pinned
 * and Renovate-managed (see {@code .github/renovate.json5}) so a partner upgrade re-runs this test and surfaces drift.
 * <p>
 * The container is driven through the {@code docker} CLI rather than Testcontainers: the Testcontainers client that is
 * new enough for a modern Docker daemon conflicts with the dependency versions managed by the XWiki 12.10 platform.
 * The test self-skips when Docker is not available, so a Docker-less build stays green.
 * <p>
 * GoToSocial requires the request {@code Host} header to equal its configured host and only serves on the bare host
 * (no port), while the server listens on another address. We reconcile the two by registering an {@code http}
 * protocol whose socket factory always dials the server address, leaving the {@code Host: localhost} header (which
 * the client derives from the default port 80) untouched. The server address is the container IP when it is reachable,
 * which is the case on CI where the build itself runs in a container next to GoToSocial and the loopback interface is
 * the build container's own. Otherwise (e.g. Docker Desktop), it is the loopback address with the mapped host port.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultWebfingerJsonParser.class, ObjectMapperConfiguration.class })
class GoToSocialWebfingerIT
{
    private static final String IMAGE =
        System.getProperty("gotosocial.image", "superseriousbusiness/gotosocial:0.17.0");

    private static final String HOST = "localhost";

    private static final String USERNAME = "testuser";

    private static final Pattern PORT_PATTERN = Pattern.compile(":(\\d+)\\s*$");

    private static final int GTS_PORT = 8080;

    private static final int CONNECT_TIMEOUT_MS = 2000;

    private static String containerId;

    private static Protocol originalHttpProtocol;

    @InjectMockComponents
    private DefaultWebfingerClient client;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @BeforeAll
    static void startServer() throws Exception
    {
        assumeTrue(dockerAvailable(), "Docker is not available, skipping GoToSocial federation test.");

        // Pull separately: when the image is missing, "docker run" prints the pull progress before the container id.
        ProcessResult pull = run(600, "docker", "pull", "-q", IMAGE);
        assertTrue(pull.exitCode == 0, "Failed to pull " + IMAGE + ": " + pull.output);

        // Start GoToSocial in HTTP mode on an SQLite database, publishing its port to a random host port. The id is
        // the last output line since Docker may print warnings before it.
        containerId = lastLine(docker("run", "-d", "-p", String.valueOf(GTS_PORT),
            "-e", "GTS_HOST=" + HOST,
            "-e", "GTS_PROTOCOL=http",
            "-e", "GTS_PORT=" + GTS_PORT,
            "-e", "GTS_DB_TYPE=sqlite",
            "-e", "GTS_DB_ADDRESS=/gotosocial/sqlite.db",
            "-e", "GTS_ACCOUNTS_REGISTRATION_OPEN=false",
            IMAGE));
        assertTrue(!containerId.isEmpty(), "Failed to start GoToSocial container");

        waitForLog("listening on", 120);

        // The account is what WebFinger resolves; create it in the running instance (shares the SQLite file).
        ProcessResult create = run(180, "docker", "exec", containerId,
            "/gotosocial/gotosocial", "admin", "account", "create",
            "--username", USERNAME, "--email", "test@example.com", "--password", "Testpassword1!");
        assertTrue(create.exitCode == 0 || create.output.contains("already in use"),
            "Account creation failed: " + create.output);

        // Route http traffic to the mapped port while keeping Host: localhost, as explained in the class Javadoc.
        originalHttpProtocol = Protocol.getProtocol("http");
        Protocol.registerProtocol("http",
            new Protocol("http", (ProtocolSocketFactory) new FixedAddressSocketFactory(serverAddress()), 80));
    }

    @AfterAll
    static void stopServer() throws Exception
    {
        if (originalHttpProtocol != null) {
            Protocol.registerProtocol("http", originalHttpProtocol);
        }
        if (containerId != null && !containerId.isEmpty()) {
            run(60, "docker", "rm", "-f", containerId);
        }
    }

    @Test
    void discoverActorThroughWebfinger() throws Exception
    {
        JSONResourceDescriptor jrd = this.client.get(USERNAME + "@" + HOST);

        assertEquals("acct:" + USERNAME + "@" + HOST, jrd.getSubject());
        Optional<String> self = jrd.getLinks().stream()
            .filter(link -> Objects.equals(link.getRel(), "self"))
            .map(Link::getHref)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .findFirst();
        assertTrue(self.isPresent(), "GoToSocial WebFinger must expose a rel=self actor link");
        assertEquals("http://" + HOST + "/users/" + USERNAME, self.get());
    }

    private static boolean dockerAvailable()
    {
        try {
            return run(20, "docker", "version", "--format", "{{.Server.Version}}").exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static InetSocketAddress serverAddress() throws Exception
    {
        String ip = lastLine(docker("inspect", "-f",
            "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{\"\\n\"}}{{end}}", containerId));
        if (!ip.isEmpty()) {
            InetSocketAddress containerAddress = new InetSocketAddress(ip, GTS_PORT);
            try (Socket socket = new Socket()) {
                socket.connect(containerAddress, CONNECT_TIMEOUT_MS);
                return containerAddress;
            } catch (IOException e) {
                // The container network is not routable from here (e.g. Docker Desktop): use the mapped port.
            }
        }
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), mappedPort());
    }

    private static int mappedPort() throws Exception
    {
        // "docker port <id> 8080" prints e.g. "0.0.0.0:49153"; extract the host port.
        String mapping = docker("port", containerId, String.valueOf(GTS_PORT)).trim();
        Matcher matcher = PORT_PATTERN.matcher(mapping.split("\\r?\\n")[0]);
        assertTrue(matcher.find(), "Could not parse mapped port from: " + mapping);
        return Integer.parseInt(matcher.group(1));
    }

    private static String lastLine(String output)
    {
        String[] lines = output.trim().split("\\r?\\n");
        return lines[lines.length - 1].trim();
    }

    private static void waitForLog(String needle, int timeoutSeconds) throws Exception
    {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        ProcessResult logs = null;
        while (System.currentTimeMillis() < deadline) {
            logs = run(20, "docker", "logs", containerId);
            if (logs.output.contains(needle)) {
                return;
            }
            Thread.sleep(1000);
        }
        // Include the container state and its logs so a CI failure shows where GoToSocial stopped during startup.
        ProcessResult state = run(20, "docker", "inspect", "-f",
            "status={{.State.Status}} exitCode={{.State.ExitCode}} oomKilled={{.State.OOMKilled}}", containerId);
        ProcessResult stats = run(30, "docker", "stats", "--no-stream", containerId);
        throw new IllegalStateException(String.format(
            "Timed out after %ds waiting for GoToSocial log line [%s]%nContainer state: %s%nContainer stats:%n%s%n"
                + "docker logs (exit code %d):%n%s",
            timeoutSeconds, needle, state.output.trim(), stats.output.trim(), logs.exitCode, logs.output));
    }

    private static String docker(String... args) throws Exception
    {
        String[] command = new String[args.length + 1];
        command[0] = "docker";
        System.arraycopy(args, 0, command, 1, args.length);
        ProcessResult result = run(120, command);
        if (result.exitCode != 0) {
            throw new IllegalStateException("docker " + String.join(" ", args) + " failed: " + result.output);
        }
        return result.output;
    }

    private static ProcessResult run(int timeoutSeconds, String... command) throws Exception
    {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (InputStream is = process.getInputStream()) {
            output = readFully(is);
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Command timed out: " + String.join(" ", command));
        }
        return new ProcessResult(process.exitValue(), output);
    }

    private static String readFully(InputStream is) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }

    private static final class ProcessResult
    {
        private final int exitCode;

        private final String output;

        ProcessResult(int exitCode, String output)
        {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    /**
     * A commons-httpclient socket factory that ignores the requested host/port and always connects to the GoToSocial
     * server address.
     */
    private static final class FixedAddressSocketFactory implements ProtocolSocketFactory
    {
        private final InetSocketAddress target;

        FixedAddressSocketFactory(InetSocketAddress target)
        {
            this.target = target;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException
        {
            return new Socket(this.target.getAddress(), this.target.getPort());
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException
        {
            return new Socket(this.target.getAddress(), this.target.getPort(), localAddress, localPort);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
            org.apache.commons.httpclient.params.HttpConnectionParams params) throws IOException
        {
            Socket socket = new Socket();
            int timeout = params == null ? 0 : params.getConnectionTimeout();
            socket.connect(this.target, timeout);
            return socket;
        }
    }
}
