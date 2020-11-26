package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.StateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.level.low.Catchup;
import com.github.dedis.student20_pop.model.network.level.low.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.level.low.LowLevelMessage;
import com.github.dedis.student20_pop.model.network.level.low.Publish;
import com.github.dedis.student20_pop.model.network.level.low.Subscribe;
import com.github.dedis.student20_pop.model.network.level.low.Unsubscribe;
import com.github.dedis.student20_pop.model.network.level.low.result.Failure;
import com.github.dedis.student20_pop.model.network.level.low.result.Result;
import com.github.dedis.student20_pop.model.network.level.low.result.ResultError;
import com.github.dedis.student20_pop.model.network.level.low.result.Success;
import com.github.dedis.student20_pop.model.network.level.mid.MessageContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Test object encoding and decoding with Gson
 */
public class TestJson {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChanneledMessage.class, new JsonLowMessageSerializer())
            .registerTypeAdapter(Result.class, new JsonResultSerializer())
            .registerTypeAdapter(Message.class, new JsonMessageSerializer())
            .create();

    private void testChanneledMessage(ChanneledMessage msg) {
        String json = gson.toJson(msg, ChanneledMessage.class);
        Assert.assertEquals(msg, gson.fromJson(json, ChanneledMessage.class));
    }

    private void testResult(Result msg) {
        String json = gson.toJson(msg, Result.class);
        Assert.assertEquals(msg, gson.fromJson(json, Result.class));
    }

    private void testMessage(Message msg) {
        String json = gson.toJson(msg, Message.class);
        Assert.assertEquals(msg, gson.fromJson(json, Message.class));
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
        testChanneledMessage(new Publish("test", 0,
                new MessageContainer("sender", "data", "signature", "id", Arrays.asList("witness1", "witness2"))));
    }

    @Test
    public void testCatchup() {
        testChanneledMessage(new Catchup("test", 0));
    }

    @Test
    public void testMessageLow() {
        testChanneledMessage(new LowLevelMessage("test",
                new MessageContainer("sender", "data", "signature", "id", Arrays.asList("witness1", "witness2"))));
    }

    @Test
    public void testSuccess() {
        testResult(new Success(0, gson.toJsonTree(40)));
    }

    @Test
    public void testFailure() {
        testResult(new Failure(4, new ResultError(4, "Test")));
    }

    @Test
    public void testCreateLao() {
        testMessage(new CreateLao("id", "name", 12L, 202L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testStateLao() {
        testMessage(new StateLao("id", "name", 12L, 202L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testUpdateLao() {
        testMessage(new UpdateLao("name", 202L, Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testCreateMeeting() {
        testMessage(new CreateMeeting("id", "name", 12L, 202L, "location", 40, 231));
    }

    @Test
    public void testStateMeeting() {
        testMessage(new CreateMeeting("id", "name", 12L, 202L, "location", 40, 231));
    }

    @Test
    public void testWitnessMessage() {
        testMessage(new WitnessMessage("id", "signature"));
    }
}
