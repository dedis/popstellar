package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.serializer.JsonAnswerSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonBase64DataSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonCastVoteDeserializer;
import com.github.dedis.popstellar.model.network.serializer.JsonChannelSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonDataSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonGenericMessageDeserializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageGeneralSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonResultSerializer;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class JsonModule {

  private JsonModule() {}

  @Provides
  @Singleton
  public static Gson provideGson(DataRegistry dataRegistry) {
    return new GsonBuilder()
        .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message.class, new JsonMessageSerializer())
        .registerTypeAdapter(Data.class, new JsonDataSerializer(dataRegistry))
        .registerTypeAdapter(CastVote.class, new JsonCastVoteDeserializer())
        .registerTypeAdapter(Result.class, new JsonResultSerializer())
        .registerTypeAdapter(ResultMessages.class, new JsonResultSerializer())
        .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
        .registerTypeAdapter(MessageGeneral.class, new JsonMessageGeneralSerializer())
        .registerTypeAdapter(Channel.class, new JsonChannelSerializer())
        // Base64URLData serializers
        .registerTypeAdapter(
            Base64URLData.class, new JsonBase64DataSerializer<>(Base64URLData::new))
        .registerTypeAdapter(PublicKey.class, new JsonBase64DataSerializer<>(PublicKey::new))
        .registerTypeAdapter(Signature.class, new JsonBase64DataSerializer<>(Signature::new))
        .registerTypeAdapter(MessageID.class, new JsonBase64DataSerializer<>(MessageID::new))
        .disableHtmlEscaping()
        .create();
  }
}
