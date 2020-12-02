package com.github.dedis.student20_pop.utility.network;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class SimpleDecoder implements Decoder.Text<String> {
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
