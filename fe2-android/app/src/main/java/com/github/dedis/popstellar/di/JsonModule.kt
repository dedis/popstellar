package com.github.dedis.popstellar.di

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.answer.Answer
import com.github.dedis.popstellar.model.network.answer.Result
import com.github.dedis.popstellar.model.network.answer.ResultMessages
import com.github.dedis.popstellar.model.network.method.Message
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.github.dedis.popstellar.model.network.serializer.base64.JsonBase64DataSerializer
import com.github.dedis.popstellar.model.network.serializer.data.JsonDataSerializer
import com.github.dedis.popstellar.model.network.serializer.data.JsonVoteSerializer
import com.github.dedis.popstellar.model.network.serializer.database.JsonElectionSerializer
import com.github.dedis.popstellar.model.network.serializer.database.JsonLaoSerializer
import com.github.dedis.popstellar.model.network.serializer.database.JsonTransactionObjectSerializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonAnswerSerializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonChannelSerializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonGenericMessageDeserializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonMessageGeneralSerializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonMessageSerializer
import com.github.dedis.popstellar.model.network.serializer.network.JsonResultSerializer
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JsonModule {

  @JvmStatic
  @Provides
  @Singleton
  fun provideGson(dataRegistry: DataRegistry): Gson {
    return GsonBuilder()
        .registerTypeAdapter(GenericMessage::class.java, JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message::class.java, JsonMessageSerializer())
        .registerTypeAdapter(Data::class.java, JsonDataSerializer(dataRegistry))
        .registerTypeAdapter(Vote::class.java, JsonVoteSerializer())
        .registerTypeAdapter(Result::class.java, JsonResultSerializer())
        .registerTypeAdapter(ResultMessages::class.java, JsonResultSerializer())
        .registerTypeAdapter(Answer::class.java, JsonAnswerSerializer())
        .registerTypeAdapter(MessageGeneral::class.java, JsonMessageGeneralSerializer())
        .registerTypeAdapter(Channel::class.java, JsonChannelSerializer())
        // Objects serializer for database
        .registerTypeAdapter(Lao::class.java, JsonLaoSerializer())
        .registerTypeAdapter(Election::class.java, JsonElectionSerializer())
        .registerTypeAdapter(TransactionObject::class.java, JsonTransactionObjectSerializer())
        // Base64URLData serializers
        .registerTypeAdapter(
            Base64URLData::class.java,
            JsonBase64DataSerializer { data: String -> Base64URLData(data) })
        .registerTypeAdapter(
            PublicKey::class.java, JsonBase64DataSerializer { data: String -> PublicKey(data) })
        .registerTypeAdapter(
            Signature::class.java, JsonBase64DataSerializer { data: String -> Signature(data) })
        .registerTypeAdapter(
            MessageID::class.java, JsonBase64DataSerializer { data: String -> MessageID(data) })
        .disableHtmlEscaping()
        .create()
  }
}
