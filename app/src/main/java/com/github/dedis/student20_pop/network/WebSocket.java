package com.github.dedis.student20_pop.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Prototype
 *
 * A simple websocket sending and receiving strings
 *
 * Possibility to register listeners
 */
public class WebSocket implements Closeable {

    private final Socket socket;
    private final BufferedWriter writer;
    private final Set<INetworkListener> listeners = new HashSet<>();

    /**
     * Create a new WebSocket connected to the given URL
     *
     * @param url to connect to
     * @throws IOException if something goes wrong during the connection
     */
    public WebSocket(URL url) throws IOException {
        this(url.getHost(), url.getPort());
    }

    /**
     * Create a new WebSocket connected to the given host and port
     *
     * @param host to connect to
     * @param port to connect to
     *
     * @throws IOException if something goes wrong during the connection
     */
    public WebSocket(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        startListener();
    }

    private void startListener() throws IOException {
        InputStream in = socket.getInputStream();
        Thread thread = new Thread(() -> {
            try(final InputStreamReader inReader = new InputStreamReader(in);
                    final BufferedReader reader = new BufferedReader(inReader)) {
                while(!socket.isClosed()) {
                    final String json = reader.readLine();

                    synchronized (listeners) {
                        for (INetworkListener listener : listeners)
                            listener.receive(json);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Add a listener to this web socket. It will be notified whenever a string is received
     * @param listener to add
     */
    public void addListener(INetworkListener listener) {
        synchronized (listeners) {
            this.listeners.add(listener);
        }
    }

    /**
     * Sends a string over the network
     * @param json to send
     * @throws IOException if someting goes wrong during the sending
     */
    public void sendData(String json) throws IOException {
        synchronized (writer) {
            writer.write(json);
            writer.newLine();
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
