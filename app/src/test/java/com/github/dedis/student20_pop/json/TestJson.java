package com.github.dedis.student20_pop.json;

import com.github.dedis.student20_pop.model.network.Catchup;
import com.github.dedis.student20_pop.model.network.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.MessageLow;
import com.github.dedis.student20_pop.model.network.Publish;
import com.github.dedis.student20_pop.model.network.Subscribe;
import com.github.dedis.student20_pop.model.network.Unsubscribe;
import com.github.dedis.student20_pop.model.network.message.Message;
import com.github.dedis.student20_pop.model.network.result.Failure;
import com.github.dedis.student20_pop.model.network.result.Result;
import com.github.dedis.student20_pop.model.network.result.ResultError;
import com.github.dedis.student20_pop.model.network.result.Success;
import com.github.dedis.student20_pop.utility.json.JsonRequestSerializer;
import com.github.dedis.student20_pop.utility.json.JsonResultSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

public class TestJson {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChanneledMessage.class, new JsonRequestSerializer())
            .registerTypeAdapter(Result.class, new JsonResultSerializer())
            .create();

    private void testChanneledMessage(ChanneledMessage msg) {
        String json = gson.toJson(msg, ChanneledMessage.class);
        Assert.assertEquals(msg, gson.fromJson(json, ChanneledMessage.class));
    }

    private void testResult(Result msg) {
        String json = gson.toJson(msg, Result.class);
        Assert.assertEquals(msg, gson.fromJson(json, Result.class));
    }

    @Test
    public void testSubscribe() {
        testChanneledMessage(new Subscribe("test", 0));
    }

    @Test
    public void testUnsubscribe() {
        testChanneledMessage(new Unsubscribe("test", 0));
    }


    @Test
    public void testPublish() {
        testChanneledMessage(new Publish("test", 0, new Message()));
    }

    @Test
    public void testCatchup() {
        testChanneledMessage(new Catchup("test", 0));
    }

    @Test
    public void testMessageLow() {
        testChanneledMessage(new MessageLow("test", new Message()));
    }

    @Test
    public void testSuccess() {
        testResult(new Success(0, gson.toJsonTree(40)));
    }

    @Test
    public void testFailure() {
        testResult(new Failure(4, new ResultError(4, "Test")));
    }
}
