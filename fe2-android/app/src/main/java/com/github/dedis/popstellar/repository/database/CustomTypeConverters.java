package com.github.dedis.popstellar.repository.database;

import androidx.room.TypeConverter;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.serializer.LaoSerializer;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class CustomTypeConverters {

  private CustomTypeConverters() {}

  private static final Type listStringType = new TypeToken<List<String>>() {}.getType();
  private static final Type setChannelType = new TypeToken<Set<Channel>>() {}.getType();

  private static final Gson gson =
      new GsonBuilder().registerTypeAdapter(Lao.class, new LaoSerializer()).create();

  /* ----  From String to Object  ---- */
  @TypeConverter
  public static MessageGeneral messageFromString(String value) {
    return gson.fromJson(value, MessageGeneral.class);
  }

  @TypeConverter
  public static MessageID messageIDFromString(String value) {
    return gson.fromJson(value, MessageID.class);
  }

  @TypeConverter
  public static Lao laoFromString(String value) {
    return gson.fromJson(value, Lao.class);
  }

  @TypeConverter
  public static List<String> listOfStringsFromString(String value) {
    return gson.fromJson(value, listStringType);
  }

  @TypeConverter
  public static Set<Channel> setOfChannelsFromString(String value) {
    return gson.fromJson(value, setChannelType);
  }

  /* ----  From Object to String  ---- */
  @TypeConverter
  public static String messageToString(MessageGeneral messageGeneral) {
    return gson.toJson(messageGeneral, MessageGeneral.class);
  }

  @TypeConverter
  public static String messageIDToString(MessageID messageID) {
    return gson.toJson(messageID, MessageID.class);
  }

  @TypeConverter
  public static String laoToString(Lao lao) {
    return gson.toJson(lao, Lao.class);
  }

  @TypeConverter
  public static String listOfStringsToString(List<String> seed) {
    return gson.toJson(seed, listStringType);
  }

  @TypeConverter
  public static String setOfChannelsToString(Set<Channel> channels) {
    return gson.toJson(channels, setChannelType);
  }
}
