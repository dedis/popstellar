package com.github.dedis.student20_pop.utility.network;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class SimpleEncoder implements Encoder.Text<String> {

    @Override
    public String encode(String object) {
        return object;
    }

    @Override
    public void init(EndpointConfig config) {}

    @Override
    public void destroy() {}
}
