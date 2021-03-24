package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.data.LAOService;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.utility.json.JsonAnswerSerializer;
import com.github.dedis.student20_pop.utility.json.JsonCreateRollCallSerializer;
import com.github.dedis.student20_pop.utility.json.JsonDataSerializer;
import com.github.dedis.student20_pop.utility.json.JsonGenericMessageDeserializer;
import com.github.dedis.student20_pop.utility.json.JsonMessageGeneralSerializer;
import com.github.dedis.student20_pop.utility.json.JsonMessageSerializer;
import com.github.dedis.student20_pop.utility.json.JsonResultSerializer;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tinder.scarlet.Scarlet;
import okhttp3.OkHttpClient;

public class Injection {

  private Injection() {
  }

  public static AndroidKeysetManager provideAndroidKeysetManager() {
    return null;
  }

  public static Gson provideGson() {
    return new GsonBuilder()
        .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message.class, new JsonMessageSerializer())
        .registerTypeAdapter(Data.class, new JsonDataSerializer())
        .registerTypeAdapter(Result.class, new JsonResultSerializer())
        .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
        .registerTypeAdapter(CreateRollCall.class, new JsonCreateRollCallSerializer())
        .registerTypeAdapter(MessageGeneral.class, new JsonMessageGeneralSerializer())
        .create();
  }

  public static BarcodeDetector provideQRCodeDetector() {
    return null;
  }

  public static CameraSource provideCameraSource() {
    return null;
  }

  public static OkHttpClient provideOkHttpClient() {
    return null;
  }

  public static Scarlet provideScarlet() {
    return null;
  }

  public static LAOService provideLAOService() {
    return null;
  }

  public static LAORepository provideLAORepository() {
    return null;
  }
}
