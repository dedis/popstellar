package com.github.dedis.student20_pop.utility.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.StateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.level.low.Catchup;
import com.github.dedis.student20_pop.model.network.level.low.Message;
import com.github.dedis.student20_pop.model.network.level.low.Broadcast;
import com.github.dedis.student20_pop.model.network.level.low.Publish;
import com.github.dedis.student20_pop.model.network.level.low.Subscribe;
import com.github.dedis.student20_pop.model.network.level.low.Unsubscribe;
import com.github.dedis.student20_pop.model.network.level.low.answer.Answer;
import com.github.dedis.student20_pop.model.network.level.low.answer.Error;
import com.github.dedis.student20_pop.model.network.level.low.answer.ErrorCode;
import com.github.dedis.student20_pop.model.network.level.low.answer.Result;
import com.github.dedis.student20_pop.model.network.level.mid.MessageGeneral;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * Test object encoding and decoding with Gson
 *
 * Also validate generated json objects with the schema defined by the protocol
 */
public class TestJson {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Message.class, new JsonLowMessageSerializer())
            .registerTypeAdapter(Answer.class, new JsonResultSerializer())
            .registerTypeAdapter(Data.class, new JsonMessageSerializer())
            .create();

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonSchema lowSchema;
    private JsonSchema highSchema;

    @Before
    public void setupSchema() throws ProcessingException {
        lowSchema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/schema/genericMessage.json");
        highSchema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/schema/query/method/message/data/data.json");
    }

    private void testChanneledMessage(Message msg) throws JsonProcessingException, ProcessingException {
        String json = gson.toJson(msg, Message.class);
        lowSchema.validInstance(mapper.readTree(json));
        Assert.assertEquals(msg, gson.fromJson(json, Message.class));
    }

    private void testResult(Answer msg) throws JsonProcessingException, ProcessingException {
        String json = gson.toJson(msg, Answer.class);
        lowSchema.validInstance(mapper.readTree(json));
        Assert.assertEquals(msg, gson.fromJson(json, Answer.class));
    }

    private void testMessage(Data msg) throws JsonProcessingException, ProcessingException {
        String json = gson.toJson(msg, Data.class);
        highSchema.validate(mapper.readTree(json));
        Assert.assertEquals(msg, gson.fromJson(json, Data.class));
    }

    @Test
    public void testSubscribe() throws JsonProcessingException, ProcessingException {
        testChanneledMessage(new Subscribe("test", 0));
    }

    @Test
    public void testUnsubscribe() throws JsonProcessingException, ProcessingException {
        testChanneledMessage(new Unsubscribe("test", 0));
    }


    @Test
    public void testPublish() throws JsonProcessingException, ProcessingException {
        testChanneledMessage(new Publish("test", 0,
                new MessageGeneral("sender", "data", "signature", "id", Arrays.asList("witness1", "witness2"))));
    }

    @Test
    public void testCatchup() throws JsonProcessingException, ProcessingException {
        testChanneledMessage(new Catchup("test", 0));
    }

    @Test
    public void testMessageLow() throws JsonProcessingException, ProcessingException {
        testChanneledMessage(new Broadcast("test",
                new MessageGeneral("sender", "data", "signature", "id", Arrays.asList("witness1", "witness2"))));
    }

    @Test
    public void testSuccess() throws JsonProcessingException, ProcessingException {
        testResult(new Result(0, gson.toJsonTree(40)));
    }

    @Test
    public void testFailure() throws JsonProcessingException, ProcessingException {
        testResult(new Error(4, new ErrorCode(4, "Test")));
    }

    @Test
    public void testCreateLao() throws JsonProcessingException, ProcessingException {
        testMessage(new CreateLao("id", "name", 12L, 202L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testStateLao() throws JsonProcessingException, ProcessingException {
        testMessage(new StateLao("id", "name", 12L, 202L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testUpdateLao() throws JsonProcessingException, ProcessingException {
        testMessage(new UpdateLao("name", 202L, Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testCreateMeeting() throws JsonProcessingException, ProcessingException {
        testMessage(new CreateMeeting("id", "name", 12L, 202L, "location", 40, 231));
    }

    @Test
    public void testStateMeeting() throws JsonProcessingException, ProcessingException {
        testMessage(new CreateMeeting("id", "name", 12L, 202L, "location", 40, 231));
    }

    @Test
    public void testWitnessMessage() throws JsonProcessingException, ProcessingException {
        testMessage(new WitnessMessage("id", "signature"));
    }
}
