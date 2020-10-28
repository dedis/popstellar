package com.github.dedis.student20_pop.utility.network.encoder;

import com.github.dedis.student20_pop.model.Vote;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class VoteEncoder implements Encoder.Text<Vote> {

    @Override
    public void init(EndpointConfig config) {}

    @Override
    public void destroy() {}

    @Override
    public String encode(Vote vote) throws EncodeException {
        return null;
    }
}
