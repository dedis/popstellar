package com.github.dedis.student20_pop.mockserver;

import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * A simply implementation of a server web socket that will be used to test our client
 */
public class ServerWebSocket {

    /**
     * Start the server and wait for a key to be pressed to stop it
     * Very rough, but operational
     *
     * @param args java main method arguments
     */
    public static void main(String[] args) {
        Server server = new Server("localhost", 2000, "/", PoPServerEndpoint.class);

        try {
            server.start();

            System.out.println("Press enter to stop the server...");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    /**
     * A sever endpoint for a web socket using the jsr 365 library
     */
    @ServerEndpoint("/")
    private static final class PoPServerEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            System.out.printf("Connected to %s\n", session.getId());
        }



        @OnMessage
        public void onMessage(String message, Session session) {
            System.out.printf("Message received from %s : %s\n", session.getId(), message);
        }



        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            System.out.printf("Session %s closed because of %s\n", session.getId(), closeReason);
        }
    }
}