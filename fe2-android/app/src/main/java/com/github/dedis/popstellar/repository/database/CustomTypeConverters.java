package com.github.dedis.popstellar.repository.database;

import androidx.room.ProvidedTypeConverter;
import androidx.room.TypeConverter;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Set;

/** Class used by the database to convert all the fields in Entities into Strings and vice versa */
@ProvidedTypeConverter
public class CustomTypeConverters {

  private final Gson gson;

  public CustomTypeConverters(Gson gson) {
    this.gson = gson;
  }

  /* ----  From String to Object  ---- */
  @TypeConverter
  public MessageGeneral messageFromString(String value) {
    return gson.fromJson(value, MessageGeneral.class);
  }

  @TypeConverter
  public MessageID messageIDFromString(String value) {
    return gson.fromJson(value, MessageID.class);
  }

  @TypeConverter
  public Lao laoFromString(String value) {
    return gson.fromJson(value, Lao.class);
  }

  @TypeConverter
  public List<String> listOfStringsFromString(String value) {
    return gson.fromJson(value, new TypeToken<List<String>>() {}.getType());
  }

  @TypeConverter
  public Set<Channel> setOfChannelsFromString(String value) {
    return gson.fromJson(value, new TypeToken<Set<Channel>>() {}.getType());
  }

  @TypeConverter
  public Election electionFromString(String value) {
    return gson.fromJson(value, Election.class);
  }

  @TypeConverter
  public RollCall rollcallFromString(String value) {
    return gson.fromJson(value, RollCall.class);
  }

  @TypeConverter
  public Meeting meetingFromString(String value) {
    return gson.fromJson(value, Meeting.class);
  }

  @TypeConverter
  public Chirp chirpFromString(String value) {
    return gson.fromJson(value, Chirp.class);
  }

  @TypeConverter
  public Reaction reactionFromString(String value) {
    return gson.fromJson(value, Reaction.class);
  }

  /* ----  From Object to String  ---- */
  @TypeConverter
  public String messageToString(MessageGeneral messageGeneral) {
    return gson.toJson(messageGeneral, MessageGeneral.class);
  }

  @TypeConverter
  public String messageIDToString(MessageID messageID) {
    return gson.toJson(messageID, MessageID.class);
  }

  @TypeConverter
  public String laoToString(Lao lao) {
    return gson.toJson(lao, Lao.class);
  }

  @TypeConverter
  public String listOfStringsToString(List<String> seed) {
    return gson.toJson(seed, new TypeToken<List<String>>() {}.getType());
  }

  @TypeConverter
  public String setOfChannelsToString(Set<Channel> channels) {
    return gson.toJson(channels, new TypeToken<Set<Channel>>() {}.getType());
  }

  @TypeConverter
  public String electionToString(Election election) {
    return gson.toJson(election, Election.class);
  }

  @TypeConverter
  public String rollcallToString(RollCall rollCall) {
    return gson.toJson(rollCall, RollCall.class);
  }

  @TypeConverter
  public String meetingToString(Meeting meeting) {
    return gson.toJson(meeting, Meeting.class);
  }

  @TypeConverter
  public String chirpToString(Chirp chirp) {
    return gson.toJson(chirp, Chirp.class);
  }

  @TypeConverter
  public String reactionToString(Reaction reaction) {
    return gson.toJson(reaction, Reaction.class);
  }
}
