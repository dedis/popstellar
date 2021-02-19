package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;
import android.hardware.Camera;

import androidx.annotation.NonNull;

import com.github.dedis.student20_pop.model.data.LAODatabase;
import com.github.dedis.student20_pop.model.data.LAOLocalDataSource;
import com.github.dedis.student20_pop.model.data.LAORemoteDataSource;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.data.LAOService;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.utility.json.JsonAnswerSerializer;
import com.github.dedis.student20_pop.utility.json.JsonCreateRollCallSerializer;
import com.github.dedis.student20_pop.utility.json.JsonDataSerializer;
import com.github.dedis.student20_pop.utility.json.JsonGenericMessageDeserializer;
import com.github.dedis.student20_pop.utility.json.JsonMessageSerializer;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter;
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class Injection {
    private static String SERVER_URL = "ws://10.0.2.2:8080";
    private static final String TAG = "INJECTION";

    public static Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
                .registerTypeAdapter(Message.class, new JsonMessageSerializer())
                .registerTypeAdapter(Data.class, new JsonDataSerializer())
                .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
                .registerTypeAdapter(CreateRollCall.class, new JsonCreateRollCallSerializer())
                .create();
    }

    public static BarcodeDetector provideQRCodeDetector(Context context) {
        return new BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
    }

    public static CameraSource provideCameraSource(Context context, Detector<Barcode> qrDetector, int width, int height) {
        return new CameraSource.Builder(context, qrDetector)
        .setFacing(CameraSource.CAMERA_FACING_BACK)
        .setRequestedPreviewSize(width, height)
        .setRequestedFps(15.0f)
        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
        .build();
    }

    public static LAORepository provideLAORepository(@NonNull Application application, Gson gson) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        Scarlet scarlet = new Scarlet.Builder()
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okHttpClient, SERVER_URL))
                .addMessageAdapterFactory(new GsonMessageAdapter.Factory(gson))
                .addStreamAdapterFactory(new RxJava2StreamAdapterFactory())
                .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
                //.backoffStrategy(new ExponentialBackoffStrategy())
                .build();

        LAOService service = scarlet.create(LAOService.class);
        LAODatabase db = LAODatabase.getDatabase(application);

        return LAORepository.getInstance(LAORemoteDataSource.getInstance(service), LAOLocalDataSource.getInstance(db));
    }
}
