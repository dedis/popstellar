package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;
import com.github.dedis.student20_pop.model.data.LAODatabase;
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
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tinder.scarlet.Scarlet;
import java.io.IOException;
import java.security.GeneralSecurityException;
import okhttp3.OkHttpClient;

public class Injection {

  private static String SERVER_URL = "ws://10.0.2.2:8080";

  private static final String TAG = "INJECTION";

  private static final String KEYSET_NAME = "POP_KEYSET";

  private static final String SHARED_PREF_FILE_NAME = "POP_KEYSET_SP";

  private static final String MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY";

  private static OkHttpClient OK_HTTP_CLIENT_INSTANCE;

  private static Scarlet SCARLET_INSTANCE;

  private static LAOService LAO_SERVICE_INSTANCE;

  private static AndroidKeysetManager KEYSET_MANAGER;

  public static AndroidKeysetManager provideAndroidKeysetManager(Context applicationContext)
      throws IOException, GeneralSecurityException {
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

  public static BarcodeDetector provideQRCodeDetector(Context context) {
    return null;
  }

  public static CameraSource provideCameraSource(
      Context context, Detector<Barcode> qrDetector, int width, int height) {
    return null;
  }

  public static OkHttpClient provideOkHttpClient() {
    return null;
  }

  public static Scarlet provideScarlet(
      Application application, OkHttpClient okHttpClient, Gson gson) {
    return SCARLET_INSTANCE;
  }

  public static LAOService provideLAOService(Scarlet scarlet) {
    return LAO_SERVICE_INSTANCE;
  }

  public static LAORepository provideLAORepository(
      Application application, LAOService service, AndroidKeysetManager keysetManager, Gson gson) {
    LAODatabase db = LAODatabase.getDatabase(application);
    return null;
  }
}
