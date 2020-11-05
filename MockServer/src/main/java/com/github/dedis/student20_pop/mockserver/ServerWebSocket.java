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

public class ServerWebSocket {

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