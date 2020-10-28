package com.github.dedis.student20_pop.utility.network;

import javax.websocket.Session;

public final class ClientSession {

    private final Session session;

    public ClientSession(Session session) {
        this.session = session;
    }

    public void subscribe(/* TODO */) {
        //TODO
    }

    public void publish(/* TODO */) {
        //TODO
    }

    public void fetch(/* TODO */) {
        //TODO fetch data. Is it a request ?
    }

    public void onMessage(String msg) {
        //TODO convert msg to object with json
    }

    // TODO remove this once not needed for testing
    @Deprecated
    public Session getSession() {
        return session;
    }
}
