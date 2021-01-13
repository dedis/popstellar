package com.github.dedis.student20_pop.utility.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Error;
import com.github.dedis.student20_pop.model.network.answer.ErrorCode;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Catchup;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.Unsubscribe;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.StateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.student20_pop.model.network.method.message.data.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.OpenRollCall;
import com.google.gson.Gson;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.uri.URIFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Test object encoding and decoding with Gson
 * <p>
 * Also validate generated json objects with the schema defined by the protocol
 */
public class TestJson {

    private final Gson gson = JsonUtils.createGson();

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonSchema lowSchema;
    private JsonSchemaFactory factory;

    @Before
    public void setupSchema() {
        factory = new JsonSchemaFactory.Builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV201909().getUri())
                .addMetaSchema(JsonMetaSchema.getV201909())
                .uriFactory(new URIFactory() {
                    @Override
                    public URI create(String uri) {
                        return URI.create(uri.replace("https://github.com/dedis/student20_pop/tree/proto-specs/", "resource:/schema/"));
                    }

                    @Override
                    public URI create(URI baseURI, String segment) {
                        return URI.create("resource:/schema/" + segment);
                    }
                }, "https")
                .build();
        lowSchema = factory.getSchema(URI.create("resource:/schema/genericMessage.json"), new SchemaValidatorsConfig());
    }

    private void testMessage(Message msg) throws JsonProcessingException {
        String json = gson.toJson(msg, Message.class);
        Set<ValidationMessage> errors = lowSchema.validate(mapper.readTree(json));
        if (errors.size() != 0) System.out.println(errors);
        Assert.assertEquals(0, errors.size());
        Assert.assertEquals(msg, gson.fromJson(json, Message.class));
    }

    private void testResult(Answer msg) throws JsonProcessingException {
        String json = gson.toJson(msg, Answer.class);
        Set<ValidationMessage> errors = lowSchema.validate(mapper.readTree(json));
    if (errors.size() != 0) System.out.println(errors);
        Assert.assertEquals(0, errors.size());
        Assert.assertEquals(msg, gson.fromJson(json, Answer.class));
    }

    private void testData(Data msg) throws JsonProcessingException {
        String json = gson.toJson(msg, Data.class);
        JsonSchema schema = factory.getSchema(URI.create("resource:/schema/query/method/message/data/data" + msg.getClass().getSimpleName() + ".json"), new SchemaValidatorsConfig());
        Set<ValidationMessage> errors = schema.validate(mapper.readTree(json));
        if (errors.size() != 0) System.out.println(errors);
        Assert.assertEquals(0, errors.size());
        Assert.assertEquals(msg, gson.fromJson(json, Data.class));
    }

    @Test
    public void testSubscribe() throws JsonProcessingException {
        testMessage(new Subscribe("/root/test", 0));
    }

    @Test
    public void testUnsubscribe() throws JsonProcessingException {
        testMessage(new Unsubscribe("/root/test", 0));
    }


    @Test
    public void testPublish() throws JsonProcessingException {
        testMessage(new Publish("/root", 0,
                new MessageGeneral("sender", "data", "signature", "id", Collections.emptyList())));
    }

    @Test
    public void testCatchup() throws JsonProcessingException {
        testMessage(new Catchup("/root/test", 0));
    }

    @Test
    public void testBroadcast() throws JsonProcessingException {
        testMessage(new Broadcast("/root/test",
                new MessageGeneral("sender", "data", "signature", "id", Collections.emptyList())));
    }

    @Test
    public void testMessageGeneral() throws JsonProcessingException {
        MessageGeneral msg = new MessageGeneral("sender", "data", "signature", "id", Collections.emptyList());
        String json = gson.toJson(msg, MessageGeneral.class);
        JsonSchema schema = factory.getSchema(URI.create("resource:/schema/query/method/message/MessageGeneral.json"), new SchemaValidatorsConfig());
        Set<ValidationMessage> errors = schema.validate(mapper.readTree(json));
        if (errors.size() != 0) System.out.println(errors);
        Assert.assertEquals(0, errors.size());
        Assert.assertEquals(msg, gson.fromJson(json, MessageGeneral.class));
    }

    @Test
    public void testSuccess() throws JsonProcessingException {
        testResult(new Result(4, gson.toJsonTree(0)));
    }

    @Test
    public void testError() throws JsonProcessingException {
        testResult(new Error(4, new ErrorCode(-4, "Test")));
    }

    @Test
    public void testCreateLao() throws JsonProcessingException {
        testData(new CreateLao("id", "name", 12L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testStateLao() throws JsonProcessingException {
        testData(new StateLao("id", "name", 12L, 202L, "organizer", Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testUpdateLao() throws JsonProcessingException {
        testData(new UpdateLao("id", "name", 202L, Arrays.asList("witness1", "witness2")));
    }

    @Test
    public void testCreateMeeting() throws JsonProcessingException {
        testData(new CreateMeeting("id", "name", 12L, "location", 40, 231));
    }

    @Test
    public void testStateMeeting() throws JsonProcessingException {
        testData(new StateMeeting("id", "name", 12L, 202L, "location", 40, 231, "modId", Collections.emptyList()));
    }

    @Test
    public void testWitnessMessage() throws JsonProcessingException {
        testData(new WitnessMessage("id", "signature"));
    }

    @Test
    public void testCreateRollCall() throws JsonProcessingException {
        testData(new CreateRollCall("id", "name", 432, 231, CreateRollCall.StartType.NOW, "loc", "desc"));
        testData(new CreateRollCall("id", "name", 432, 231, CreateRollCall.StartType.NOW, "loc", null));
        testData(new CreateRollCall("id", "name", 432, 231, CreateRollCall.StartType.SCHEDULED, "loc", "desc"));
        testData(new CreateRollCall("id", "name", 432, 231, CreateRollCall.StartType.SCHEDULED, "loc", null));
    }

    @Test
    public void testOpenRollCall() throws JsonProcessingException {
        testData(new OpenRollCall("id", 32));
    }

    @Test
    public void testCloseRollCall() throws JsonProcessingException {
        testData(new CloseRollCall("id", 32, 342, Arrays.asList("1", "2", "3")));
    }
}
