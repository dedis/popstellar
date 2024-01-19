package com.github.dedis.popstellar.repository.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Class used by the database to convert all the fields in Entities into Strings and vice versa */
@ProvidedTypeConverter
class CustomTypeConverters(private val gson: Gson) {
  /* ----  From String to Object  ---- */
  @TypeConverter
  fun messageFromString(value: String): MessageGeneral? {
    return gson.fromJson(value, MessageGeneral::class.java)
  }

  @TypeConverter
  fun messageIDFromString(value: String): MessageID {
    return gson.fromJson(value, MessageID::class.java)
  }

  @TypeConverter
  fun laoFromString(value: String): Lao {
    return gson.fromJson(value, Lao::class.java)
  }

  @TypeConverter
  fun listOfStringsFromString(value: String): List<String> {
    return gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
  }

  @TypeConverter
  fun setOfChannelsFromString(value: String): Set<Channel> {
    return gson.fromJson(value, object : TypeToken<Set<Channel>>() {}.type)
  }

  @TypeConverter
  fun electionFromString(value: String): Election? {
    return gson.fromJson(value, Election::class.java)
  }

  @TypeConverter
  fun rollcallFromString(value: String): RollCall? {
    return gson.fromJson(value, RollCall::class.java)
  }

  @TypeConverter
  fun meetingFromString(value: String): Meeting? {
    return gson.fromJson(value, Meeting::class.java)
  }

  @TypeConverter
  fun chirpFromString(value: String): Chirp {
    return gson.fromJson(value, Chirp::class.java)
  }

  @TypeConverter
  fun reactionFromString(value: String): Reaction {
    return gson.fromJson(value, Reaction::class.java)
  }

  @TypeConverter
  fun publicKeyFromString(value: String): PublicKey {
    return gson.fromJson(value, PublicKey::class.java)
  }

  @TypeConverter
  fun transactionObjectFromString(value: String): TransactionObject {
    return gson.fromJson(value, TransactionObject::class.java)
  }

  @TypeConverter
  fun witnessMessageFromString(value: String): WitnessMessage {
    return gson.fromJson(value, WitnessMessage::class.java)
  }

  /* ----  From Object to String  ---- */
  @TypeConverter
  fun messageToString(messageGeneral: MessageGeneral?): String {
    return gson.toJson(messageGeneral, MessageGeneral::class.java)
  }

  @TypeConverter
  fun messageIDToString(messageID: MessageID?): String {
    return gson.toJson(messageID, MessageID::class.java)
  }

  @TypeConverter
  fun laoToString(lao: Lao?): String {
    return gson.toJson(lao, Lao::class.java)
  }

  @TypeConverter
  fun listOfStringsToString(seed: List<String>?): String {
    return gson.toJson(seed, object : TypeToken<List<String>?>() {}.type)
  }

  @TypeConverter
  fun setOfChannelsToString(channels: Set<Channel>?): String {
    return gson.toJson(channels, object : TypeToken<Set<Channel>?>() {}.type)
  }

  @TypeConverter
  fun electionToString(election: Election?): String {
    return gson.toJson(election, Election::class.java)
  }

  @TypeConverter
  fun rollcallToString(rollCall: RollCall?): String {
    return gson.toJson(rollCall, RollCall::class.java)
  }

  @TypeConverter
  fun meetingToString(meeting: Meeting?): String {
    return gson.toJson(meeting, Meeting::class.java)
  }

  @TypeConverter
  fun chirpToString(chirp: Chirp?): String {
    return gson.toJson(chirp, Chirp::class.java)
  }

  @TypeConverter
  fun reactionToString(reaction: Reaction?): String {
    return gson.toJson(reaction, Reaction::class.java)
  }

  @TypeConverter
  fun publicKeyToString(publicKey: PublicKey?): String {
    return gson.toJson(publicKey, PublicKey::class.java)
  }

  @TypeConverter
  fun transactionObjectToString(transactionObject: TransactionObject?): String {
    return gson.toJson(transactionObject, TransactionObject::class.java)
  }

  @TypeConverter
  fun witnessMessageToString(witnessMessage: WitnessMessage?): String {
    return gson.toJson(witnessMessage, WitnessMessage::class.java)
  }
}
