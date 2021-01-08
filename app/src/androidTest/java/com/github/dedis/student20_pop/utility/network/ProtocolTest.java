package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.model.network.level.high.rollcall.CreateRollCall;

import net.jodah.concurrentunit.Waiter;

import org.glassfish.tyrus.server.Server;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
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

    public static final String LAO_NAME = "name";
    public static final String MEETING_NAME = "name";
    public static final String PERSON_NAME = "Bob";

    public static final String HOST_NAME = "localhost";
    public static final int PORT = 2020;

    public static final int REQUEST_TIMEOUT = 6000;
    public static final int TEST_TIMEOUT = 10000;
    public static final String LAO_ID = "id";
    public static final String MESSAGE_ID = "mId";
    public static final String MESSAGE_DATA = "data";
    public static final String LOCATION = "loc";

    private Server startAcceptEverythingServer() throws DeploymentException {
        Server server = new Server(HOST_NAME, PORT, "", PerfectServer.class);
        server.start();
        return server;
    }

    // Test timeouts
    private Server startNoAnswerServer() throws DeploymentException {
        Server server = new Server(HOST_NAME, PORT, "", TimeoutServer.class);
        server.start();
        return server;
    }

    @Test
    public void testTimeout() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startNoAnswerServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.createLao(LAO_NAME, 0, 0, bob.getId())
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t != null);
                    waiter.resume();
                });

        synchronized (this) {
            wait(REQUEST_TIMEOUT);
        }

        proxy.lowLevel().purge();

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testCreateLao() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.createLao(LAO_NAME, 0, 0, bob.getId())
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testUpdateLao() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.updateLao(LAO_ID, LAO_NAME, 0, Collections.singletonList(bob.getId()))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testCreateMeeting() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.createMeeting(LAO_ID, MEETING_NAME, 0, 0, "loc", 0, 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testWitnessMessage() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.witnessMessage(LAO_ID, "mId", "data")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testCreateRollCall() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.createRollCall(LAO_ID, "roll", 0, 0, CreateRollCall.StartType.NOW, "loc", "desc")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createRollCall(LAO_ID, "roll", 0, 0, CreateRollCall.StartType.SCHEDULED, "loc", "desc")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createRollCall(LAO_ID, "roll", 0, 0, CreateRollCall.StartType.NOW, "loc")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createRollCall(LAO_ID, "roll", 0, 0, CreateRollCall.StartType.SCHEDULED, "loc")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 4);
        server.stop();
    }

    @Test
    public void testOpenRollCall() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.openRollCall(LAO_ID, "rollId", 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testReopenRollCall() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.reopenRollCall(LAO_ID, "rollId", 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }


    @Test
    public void testCloseRollCall() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.closeRollCall(LAO_ID, "rollId", 0, 0, Arrays.asList("attendee1", "attendee2"))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 1);
        server.stop();
    }

    @Test
    public void testProtocol() throws DeploymentException, TimeoutException, InterruptedException {
        Server server = startAcceptEverythingServer();
        Waiter waiter = new Waiter();

        Person bob = new Person(PERSON_NAME);
        HighLevelClientProxy proxy = PoPClientEndpoint.connect(URI.create("ws://" + HOST_NAME + ":" + PORT + "/"), bob);

        proxy.createLao(LAO_NAME, 0, 0, bob.getId())
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.updateLao(LAO_ID, LAO_NAME, 0, Collections.singletonList(bob.getId()))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createMeeting(LAO_ID, MEETING_NAME, 0, 0, LOCATION, 0, 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.witnessMessage(LAO_ID, MESSAGE_ID, MESSAGE_DATA)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.createRollCall(LAO_ID, "roll", 0, 0, CreateRollCall.StartType.NOW, "loc", "desc")
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.openRollCall(LAO_ID, "rollId", 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.reopenRollCall(LAO_ID, "rollId", 0)
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        proxy.closeRollCall(LAO_ID, "rollId", 0, 0, Arrays.asList("attendee1", "attendee2"))
                .whenComplete((i, t) -> {
                    waiter.assertTrue(t == null);
                    waiter.assertEquals(i, 0);
                    waiter.resume();
                });

        waiter.await(TEST_TIMEOUT, 8);
        server.stop();
    }

    @ServerEndpoint("/")
    public static class PerfectServer {

        public static final String JSONRPC_FORMAT = "{\"jsonrpc\": \"2.0\",\"result\": 0,\"id\": %d}";
        public static final String ID_REGEX = "\"id\":(-?\\d+)";

        @OnMessage
        public void onMessage(String message, Session session) {
            Pattern pattern = Pattern.compile(ID_REGEX);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                try {
                    session.getBasicRemote().sendText(
                            String.format(Locale.US,
                                    JSONRPC_FORMAT,
                                    Integer.parseInt(matcher.group(1))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @ServerEndpoint("/")
    public static class TimeoutServer {
    }
}
