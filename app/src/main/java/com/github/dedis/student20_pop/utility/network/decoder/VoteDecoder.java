package com.github.dedis.student20_pop.utility.network.decoder;

import com.github.dedis.student20_pop.model.Vote;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class VoteDecoder implements Decoder.Text<Vote> {

    @Override
    public Vote decode(String s) throws DecodeException {
        return null;
    }

    @Override
    public boolean willDecode(String s) {
        return false;
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
