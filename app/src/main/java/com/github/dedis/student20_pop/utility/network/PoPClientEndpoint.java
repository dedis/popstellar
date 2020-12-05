package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Person;

import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * A client endpoint to a web socket using jsr 365 library
 *
 * TODO link onOpen and onRemove to the UI
 */
@ClientEndpoint(encoders = PoPClientEndpoint.SimpleEncoder.class, decoders = PoPClientEndpoint.SimpleDecoder.class)
public final class PoPClientEndpoint {

    private static final ClientManager client = ClientManager.createClient();

    /**
     * Create a new HighLevelClientProxy that will encapsulate the socket
     *
     * @param host to connect to
     * @return the proxy
     * @throws DeploymentException if an error occurs during the deployment
     */
    public static HighLevelClientProxy connectToServer(URI host, Person person) throws DeploymentException {
        Session session = client.connectToServer(PoPClientEndpoint.class, host);
        HighLevelClientProxy client = new HighLevelClientProxy(session, person);
        listeners.put(session, client.lowLevel());
        return client;
    }

    private static final Map<Session, LowLevelClientProxy> listeners = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Client successfully connected to " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        synchronized (listeners) {
            LowLevelClientProxy client = listeners.get(session);
            if(client == null)
                throw new IllegalArgumentException();

            client.onMessage(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        listeners.remove(session);
    }

    static class SimpleDecoder implements Decoder.Text<String> {

        @Override
        public String decode(String s) {
            return s;
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }

        @Override
        public void init(EndpointConfig config) {}

        @Override
        public void destroy() {}
    }

    static class SimpleEncoder implements Encoder.Text<String> {

        @Override
        public String encode(String object) {
            return object;
        }

        @Override
        public void init(EndpointConfig config) {}

        @Override
        public void destroy() {}
    }
}
