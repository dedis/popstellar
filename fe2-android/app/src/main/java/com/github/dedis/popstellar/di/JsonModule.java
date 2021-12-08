package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.serializer.JsonAnswerSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonDataSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonGenericMessageDeserializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageGeneralSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonResultSerializer;
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
  public static Gson provideGson() {
    return new GsonBuilder()
        .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message.class, new JsonMessageSerializer())
        .registerTypeAdapter(Data.class, new JsonDataSerializer())
        .registerTypeAdapter(Result.class, new JsonResultSerializer())
        .registerTypeAdapter(ResultMessages.class, new JsonResultSerializer())
        .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
        .registerTypeAdapter(MessageGeneral.class, new JsonMessageGeneralSerializer())
        .disableHtmlEscaping()
        .create();
  }
}
