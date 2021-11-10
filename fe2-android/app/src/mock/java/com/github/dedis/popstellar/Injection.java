package com.github.dedis.popstellar;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.serializer.JsonAnswerSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonDataSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonGenericMessageDeserializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageGeneralSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonMessageSerializer;
import com.github.dedis.popstellar.model.network.serializer.JsonResultSerializer;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.local.LAODatabase;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.repository.remote.LAOService;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.WebSocket;

import java.io.IOException;
import java.security.GeneralSecurityException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import okhttp3.OkHttpClient;

public class Injection {

  private static final String KEYSET_NAME = "POP_KEYSET";

  private static final String SHARED_PREF_FILE_NAME = "POP_KEYSET_SP";

  private static final String MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY";

  private static AndroidKeysetManager KEYSET_MANAGER;

  private static volatile ViewModelFactory viewModelFactory;

  private Injection() {}

  @SuppressWarnings("unused")
  public static AndroidKeysetManager provideAndroidKeysetManager(Context applicationContext)
      throws GeneralSecurityException, IOException {
    if (KEYSET_MANAGER == null) {
      SharedPreferences.Editor editor =
          applicationContext
              .getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
              .edit();
      editor.apply();

      Ed25519PrivateKeyManager.registerPair(true);
      PublicKeySignWrapper.register();

      // TODO: move to background thread
      AndroidKeysetManager keysetManager =
          new AndroidKeysetManager.Builder()
              .withSharedPref(applicationContext, KEYSET_NAME, SHARED_PREF_FILE_NAME)
              .withKeyTemplate(Ed25519PrivateKeyManager.rawEd25519Template())
              .withMasterKeyUri(MASTER_KEY_URI)
              .build();

      KeysetHandle publicKeysetHandle = keysetManager.getKeysetHandle().getPublicKeysetHandle();

      try {
        String publicKey = Keys.getEncodedKey(publicKeysetHandle);
      } catch (IOException e) {
        Log.e("INJECTION", "failed to retrieve public key", e);
      }

      KEYSET_MANAGER = keysetManager;
    }
    return KEYSET_MANAGER;
  }

  public static Gson provideGson() {
    return new GsonBuilder()
        .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message.class, new JsonMessageSerializer())
        .registerTypeAdapter(Data.class, new JsonDataSerializer())
        .registerTypeAdapter(Result.class, new JsonResultSerializer())
        .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
        .registerTypeAdapter(MessageGeneral.class, new JsonMessageGeneralSerializer())
        .create();
  }

  @SuppressWarnings("unused")
  public static BarcodeDetector provideQRCodeDetector(Context context) {
    return null;
  }

  @SuppressWarnings("unused")
  public static CameraSource provideCameraSource(
      Context context, Detector<Barcode> qrDetector, int width, int height) {
    return null;
  }

  public static OkHttpClient provideOkHttpClient() {
    return null;
  }

  @SuppressWarnings("unused")
  public static Scarlet provideScarlet(
      Application application, OkHttpClient okHttpClient, Gson gson) {
    return null;
  }

  @SuppressWarnings("unused")
  public static LAOService provideLAOService(Scarlet scarlet) {
    return null;
  }

  @SuppressWarnings("unused")
  public static LAORepository provideLAORepository(
      Application application, LAOService service, AndroidKeysetManager keysetManager, Gson gson) {
    LAODatabase db = LAODatabase.getDatabase(application);
    return LAORepository.getInstance(
        LAORemoteDataSource.getInstance(getMockService()),
        LAOLocalDataSource.getInstance(db),
        keysetManager,
        gson,
        new ProdSchedulerProvider());
  }

  private static LAOService getMockService() {
    return new LAOService() {
      @Override
      public void sendMessage(Message msg) {
        // "mock" method
      }

      @Override
      public Observable<GenericMessage> observeMessage() {
        return new Observable<GenericMessage>() {
          @Override
          protected void subscribeActual(Observer<? super GenericMessage> observer) {
            // "mock" method
          }
        };
      }

      @Override
      public Observable<WebSocket.Event> observeWebsocket() {
        return new Observable<WebSocket.Event>() {
          @Override
          protected void subscribeActual(Observer<? super WebSocket.Event> observer) {
            // "mock" method
          }
        };
      }
    };
  }

  public static synchronized ViewModelFactory provideViewModelFactory(Application application) {
    if (viewModelFactory == null) {
      Log.d(
          ViewModelFactory.class.getSimpleName(),
          "Creating new instance of " + ViewModelFactory.class.getSimpleName());
      viewModelFactory = new ViewModelFactory(application);
    }
    return viewModelFactory;
  }
}
