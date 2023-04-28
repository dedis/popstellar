package com.github.dedis.popstellar.repository.database;

import androidx.room.TypeConverter;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class CustomTypeConverters {

  private static final Type messageIDType = new TypeToken<MessageID>() {}.getType();
  private static final Type messageGeneralType = new TypeToken<MessageGeneral>() {}.getType();
  private static final Type laoType = new TypeToken<Lao>() {}.getType();
  private static final Type listStringType = new TypeToken<List<String>>() {}.getType();
  private static final Type setChannelType = new TypeToken<Set<Channel>>() {}.getType();

  private static final Gson gson = new Gson();

  /* ----  From String to Object  ---- */
  @TypeConverter
  public MessageGeneral messageFromString(String value) {
    return gson.fromJson(value, messageGeneralType);
  }

  @TypeConverter
  public MessageID messageIDFromString(String value) {
    return gson.fromJson(value, messageIDType);
  }

  @TypeConverter
  public Lao laoFromString(String value) {
    return gson.fromJson(value, laoType);
  }

  @TypeConverter
  public List<String> listOfStringsFromString(String value) {
    return gson.fromJson(value, listStringType);
  }

  @TypeConverter
  public Set<Channel> setOfChannelsFromString(String value) {
    return gson.fromJson(value, setChannelType);
  }

  /* ----  From Object to String  ---- */
  @TypeConverter
  public String messageToString(MessageGeneral messageGeneral) {
    return gson.toJson(messageGeneral, messageGeneralType);
  }

  @TypeConverter
  public String messageIDToString(MessageID messageID) {
    return gson.toJson(messageID, messageIDType);
  }

  @TypeConverter
  public String laoToString(Lao lao) {
    return gson.toJson(lao, laoType);
  }

  @TypeConverter
  public String listOfStringsToString(List<String> seed) {
    return gson.toJson(seed, listStringType);
  }

  @TypeConverter
  public String setOfChannelsToString(Set<Channel> channels) {
    return gson.toJson(channels, setChannelType);
  }
}
