package com.github.dedis.student20_pop.json;

import com.github.dedis.student20_pop.model.network.Catchup;
import com.github.dedis.student20_pop.model.network.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.MessageLow;
import com.github.dedis.student20_pop.model.network.Publish;
import com.github.dedis.student20_pop.model.network.Subscribe;
import com.github.dedis.student20_pop.model.network.Unsubscribe;
import com.github.dedis.student20_pop.model.network.message.Message;
import com.github.dedis.student20_pop.utility.json.JsonActionSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

public class TestJson {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChanneledMessage.class, new JsonActionSerializer())
            .create();

    private void testJson(ChanneledMessage msg) {
        String json = gson.toJson(msg, ChanneledMessage.class);
        Assert.assertEquals(msg, gson.fromJson(json, ChanneledMessage.class));
    }

    @Test
    public void testSubscribe() {
        testJson(new Subscribe("test", 0));
    }

    @Test
    public void testUnsubscribe() {
        testJson(new Unsubscribe("test", 0));
    }


    @Test
    public void testPublish() {
        testJson(new Publish("test", 0, new Message()));
    }

    @Test
    public void testCatchup() {
        testJson(new Catchup("test", 0));
    }

    @Test
    public void testMessageLow() {
        testJson(new MessageLow("test", new Message()));
    }
}
