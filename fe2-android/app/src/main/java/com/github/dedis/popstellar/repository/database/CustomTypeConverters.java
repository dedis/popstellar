package com.github.dedis.popstellar.repository.database;

import androidx.room.TypeConverter;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class CustomTypeConverters {

  private static final Type messageIDType = new TypeToken<MessageID>() {}.getType();
  private static final Type messageGeneralType = new TypeToken<MessageGeneral>() {}.getType();
  private static final Type laoType = new TypeToken<Lao>() {}.getType();

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
}
