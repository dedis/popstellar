package com.github.dedis.popstellar.repository.database;

import androidx.room.ProvidedTypeConverter;
import androidx.room.TypeConverter;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
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
}
