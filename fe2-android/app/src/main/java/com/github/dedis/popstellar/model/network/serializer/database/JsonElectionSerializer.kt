package com.github.dedis.popstellar.model.network.serializer.database

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class JsonElectionSerializer : JsonSerializer<Election>, JsonDeserializer<Election> {
  @Throws(JsonParseException::class)
  override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): Election {
    val jsonObject = json.asJsonObject

    // Deserialize the nested Channel object
    val channel = context.deserialize<Channel>(jsonObject["channel"], Channel::class.java)

    // Deserialize the primitive values
    val id = jsonObject["id"].asString
    val name = jsonObject["name"].asString
    val creation = jsonObject["creation"].asLong
    val start = jsonObject["start"].asLong
    val end = jsonObject["end"].asLong
    val electionKeyElement = jsonObject["electionKey"]
    val electionKey = electionKeyElement?.asString

    // Deserialize the enums
    val electionVersion = ElectionVersion.valueOf(jsonObject["electionVersion"].asString)
    val stateString = jsonObject["state"].asString
    val state = if (stateString.isEmpty()) null else EventState.valueOf(stateString)

    // Deserialize the list electionQuestions
    val electionQuestionsJsonArray = jsonObject["electionQuestions"].asJsonArray
    val electionQuestions: MutableList<ElectionQuestion> = ArrayList()
    for (electionQuestionJsonElement in electionQuestionsJsonArray) {
      electionQuestions.add(
          context.deserialize(electionQuestionJsonElement, ElectionQuestion::class.java))
    }

    // Deserialize the map votesBySender
    val votesBySender: MutableMap<PublicKey, List<Vote>> = HashMap()
    val votesBySenderObject = jsonObject["votesBySender"].asJsonObject
    for ((key, value) in votesBySenderObject.entrySet()) {
      val senderPublicKey = PublicKey(key)
      val votesBySenderJsonArray = value.asJsonArray
      // Deserialize the value of the map (list of votes)
      val votesBySenderPk: MutableList<Vote> = ArrayList()
      for (element in votesBySenderJsonArray) {
        votesBySenderPk.add(context.deserialize(element, Vote::class.java))
      }
      votesBySender[senderPublicKey] = votesBySenderPk
    }

    // Deserialize the map messageMap
    val messageMap: MutableMap<PublicKey, MessageID> = HashMap()
    val messageMapObject = jsonObject["messageMap"].asJsonObject
    for ((key, value) in messageMapObject.entrySet()) {
      val senderPublicKey = PublicKey(key)
      val messageId = MessageID(value.asString)
      messageMap[senderPublicKey] = messageId
    }

    // Deserialize the map results
    val results: MutableMap<String, Set<QuestionResult>> = HashMap()
    val resultsObject = jsonObject["results"].asJsonObject
    for ((questionId, value) in resultsObject.entrySet()) {
      // Deserialize the value of the map (set of question results)
      val resultsJsonArray = value.asJsonArray
      val resultsByQuestion: MutableSet<QuestionResult> = HashSet()
      for (element in resultsJsonArray) {
        resultsByQuestion.add(context.deserialize(element, QuestionResult::class.java))
      }
      results[questionId] = resultsByQuestion
    }

    return Election(
        id,
        name,
        creation,
        channel,
        start,
        end,
        electionQuestions,
        electionKey,
        electionVersion,
        votesBySender,
        messageMap,
        state,
        results)
  }

  override fun serialize(
      election: Election,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement {
    val jsonObject = JsonObject()

    // Serialize the channel object
    jsonObject.add("channel", context.serialize(election.channel, Channel::class.java))

    // Serialize the primitive values
    jsonObject.addProperty("id", election.id)
    jsonObject.addProperty("name", election.name)
    jsonObject.addProperty("creation", election.creation)
    jsonObject.addProperty("start", election.startTimestamp)
    jsonObject.addProperty("end", election.endTimestamp)
    jsonObject.addProperty("electionKey", election.electionKey)

    // Serialize the enum
    val state = election.state
    jsonObject.addProperty("state", state?.name ?: "")
    jsonObject.addProperty("electionVersion", election.electionVersion.name)

    // Serialize the list of election questions into a JsonArray
    val electionQuestionsJsonArray = JsonArray()
    for (electionQuestion in election.electionQuestions) {
      electionQuestionsJsonArray.add(
          context.serialize(electionQuestion, ElectionQuestion::class.java))
    }
    jsonObject.add("electionQuestions", electionQuestionsJsonArray)

    // Serialize the map of votesBySender into a JsonObject
    val votesBySenderJsonObject = JsonObject()
    for ((key, value) in election.votesBySender) {
      val pk = key.encoded
      // Serialize the list of votes into a JsonArray
      val votesBySenderJsonArray = JsonArray()
      for (vote in value) {
        votesBySenderJsonArray.add(context.serialize(vote, Vote::class.java))
      }
      votesBySenderJsonObject.add(pk, votesBySenderJsonArray)
    }
    jsonObject.add("votesBySender", votesBySenderJsonObject)

    // Serialize the messageMap into a JsonObject
    val messageMapJsonObject = JsonObject()
    for ((key, value) in election.messageMap) {
      val pk = key.encoded
      val messageID = value.encoded
      val jsonElement: JsonElement = JsonPrimitive(messageID)
      messageMapJsonObject.add(pk, jsonElement)
    }
    jsonObject.add("messageMap", messageMapJsonObject)

    // Serialize the results map into a JsonObject
    val resultsJsonObject = JsonObject()
    for ((key, value) in election.results) {
      // Serialize the set of question results into a JsonArray
      val resultsJsonArray = JsonArray()
      for (vote in value) {
        resultsJsonArray.add(context.serialize(vote, QuestionResult::class.java))
      }
      resultsJsonObject.add(key, resultsJsonArray)
    }
    jsonObject.add("results", resultsJsonObject)

    return jsonObject
  }
}
