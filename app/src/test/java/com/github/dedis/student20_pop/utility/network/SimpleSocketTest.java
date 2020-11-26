package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Person;

import net.jodah.concurrentunit.Waiter;

import org.glassfish.tyrus.server.Server;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Test the web socket implementation
 */
public class SimpleSocketTest {

    private static Waiter waiter;
    private static Queue<String> messages;

    /**
     * Currently test that the mocked server receive every string the client sends.
     * This wil change once the publish-subscribe protocol is implemented
     */
    @Test
    public void simpleMessageExchange() throws DeploymentException, TimeoutException, InterruptedException {
        //Setup
        List<String> toSend = new ArrayList<>();
        toSend.add("Test 1");
        toSend.add("Test 2");
        toSend.add("Test 3");

        waiter = new Waiter();
        messages = new LinkedList<>(toSend);

        Server server = new Server("localhost", 2000, "", MockServerEndpoint.class);
        server.start();

        Thread t = new Thread(() -> {
            try {
                LowLevelClientProxy session = PoPClientEndpoint.connectToServer(URI.create("ws://localhost:2000/"), new Person("tester")).lowLevel();
                for(String s : toSend)
                    session.getSession().getBasicRemote().sendText(s);
                session.getSession().close();
                waiter.resume();
            } catch (DeploymentException | IOException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();

        waiter.await(10000, toSend.size() + 1);

        server.stop();
    }

    @ServerEndpoint("/")
    public static class MockServerEndpoint {

        @OnMessage
        public void onMessage(String message, Session session) {
            waiter.assertEquals(messages.poll(), message);
            waiter.resume();
        }
    }
}
