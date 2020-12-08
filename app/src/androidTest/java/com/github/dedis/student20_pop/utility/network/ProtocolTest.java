package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Person;

import net.jodah.concurrentunit.Waiter;

import org.glassfish.tyrus.server.Server;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Test that the protocol handles correctly and sends messages.
 * Also test the CompletableFuture system and timeouts
 */
public class ProtocolTest {

    private Server startAcceptEverythingServer() throws DeploymentException {
        Server server = new Server("localhost", 2020, "", PerfectServer.class);
        server.start();
        return server;
    }

    // Test timeouts
    private Server startNoAnswerServer() throws DeploymentException {
        Server server = new Server("localhost", 2020, "", TimeoutServer.class);
        server.start();
        return server;
    }

    @Test
    public void testTimeout() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startNoAnswerServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.createLoa("name", 0, 0, bob.getId())
            .whenComplete((i, t) -> {
                waiter.assertTrue(t != null);
                waiter.resume();
            });

        synchronized (this) {
            wait(6000);
        }

        proxy.lowLevel().purge();

        waiter.await(10000, 1);
        server.stop();
    }

    @Test
    public void testCreateLao() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.createLoa("name", 0, 0, bob.getId())
            .whenComplete((i, t) -> {
                waiter.assertTrue(t == null);
                waiter.assertEquals(i, 0);
                waiter.resume();
            });

        waiter.await(10000, 1);
        server.stop();
    }

    @Test
    public void testUpdateLao() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.updateLao("id", "name", 0, Collections.singletonList(bob.getId()))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(10000, 1);
        server.stop();
    }

    @Test
    public void testCreateMeeting() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.createMeeting("id", "name", 0, 0, "loc", 0, 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(10000, 1);
        server.stop();
    }

    @Test
    public void testWitnessMeeting() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.witnessMessage("id", "mId", "data")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(10000, 1);
        server.stop();
    }

    @Test
    public void testProtocol() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person("Bob");
        HighLevelClientProxy proxy = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2020/"), bob);

        proxy.createLoa("name", 0, 0, bob.getId())
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.updateLao("id", "name", 0, Collections.singletonList(bob.getId()))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createMeeting("id", "name", 0, 0, "loc", 0, 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.witnessMessage("id", "mId", "data")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(10000, 4);
        server.stop();
    }

    @ServerEndpoint("/")
    public static class PerfectServer {

        @OnMessage
        public void onMessage(String message, Session session) {
            System.out.println("===========================================0 R: " + message);
            Pattern pattern = Pattern.compile("\"id\":(-?\\d+)");
            Matcher matcher = pattern.matcher(message);
            if(matcher.find()) {
                try {
                    session.getBasicRemote().sendText(
                            String.format(Locale.US,
                                    "{\"jsonrpc\": \"2.0\",\"result\": 0,\"id\": %d}",
                                    Integer.parseInt(matcher.group(1))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @ServerEndpoint("/")
    public static class TimeoutServer {}
}
