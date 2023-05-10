package com.github.dedis.popstellar.utility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.github.dedis.popstellar.R;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.*;
import timber.log.Timber;

/**
 * This class extends the default Timber.Tree to implement a custom logger which decides whether to
 * forward logs also to a server or to print them on the console.
 */
public class NetworkLogger extends Timber.Tree {

  private static final String TAG = NetworkLogger.class.getSimpleName();

  /** Boolean flag whether to close the websocket connection whenever the application pauses */
  private static final boolean SAVE_ENERGY = false;

  /** URL of the remote server to which sending the logs */
  private static final StringBuilder serverUrl = new StringBuilder();

  private static WebSocket webSocket;

  /**
   * As the connection is implemented static to be opened and closed only once, its access is
   * synchronized through this lock
   */
  private static final Object lock = new Object();

  /**
   * Boolean which is atomically set by the user from the settings and checked by the loggers. This
   * value is set based on the settings, which is persisted even after the application is closed
   */
  private static final AtomicBoolean sendToServer = new AtomicBoolean(false);

  /**
   * Initialize the value of server url and the enable flag from the application context, based on
   * the preference value persisted in memory. Register the callback to shut down the connection
   * when the app is closed.
   *
   * @param application PoP application
   */
  public static void loadFromPersistPreference(Application application) {
    // Retrieve the server url and the enable flag from the persisted preferences
    Context context = application.getApplicationContext();
    sendToServer.set(
        (PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.settings_logging_key), false)));
    serverUrl.append(
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.settings_server_url_key), ""));

    // Register the callback for a graceful shutdown
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_DESTROY, activity -> closeWebSocket());
    Consumer<Activity> saverConsumer =
        activity -> {
          if (SAVE_ENERGY) {
            closeWebSocket();
            closeWebSocket();
          }
        };
    consumerMap.put(Lifecycle.Event.ON_PAUSE, saverConsumer);
    consumerMap.put(Lifecycle.Event.ON_STOP, saverConsumer);
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  @Override
  protected void log(int priority, String tag, @NonNull String message, Throwable t) {
    // Take the stack trace of the error if present
    String error = t == null ? "" : Log.getStackTraceString(t);

    // Always print the log to console
    if (t != null) {
      Log.println(priority, tag, message + "\n" + error);
    } else {
      Log.println(priority, tag, message);
    }

    // Send the log message to the remote server
    if (sendToServer.get()) {
      String log = buildRemoteLog(priority, tag, message, error);
      sendToServer(log);
    }
  }

  /** Function to enable the remote logging. It opens the websocket */
  public static void enableRemote() {
    // Open the websocket connection
    connectWebSocket();
    // Enable the log method to forward logs to the server
    sendToServer.set(true);
  }

  /** Function to disable the remote logging. It closes the websocket */
  public static void disableRemote() {
    // Stop the logs to be crafted and sent to server, continue to print on console
    sendToServer.set(false);
    // Close the websocket connection
    closeWebSocket();
  }

  public static void setServerUrl(String url) {
    serverUrl.replace(0, serverUrl.length(), url);
  }

  private void sendToServer(String log) {
    // Take the lock and send the log as UTF-8 encoded string
    synchronized (lock) {
      // If the websocket hasn't been already opened then open it
      if (webSocket == null) {
        connectWebSocket();
      }
      // Connect may fail
      if (webSocket != null) {
        webSocket.send(log);
      }
    }
  }

  /** Function which opens the web socket with the server */
  @SuppressLint("LogNotTimber")
  private static void connectWebSocket() {
    try {
      Request request = new Request.Builder().url(serverUrl.toString()).build();
      OkHttpClient client = new OkHttpClient();
      // Create the socket with an empty listener
      webSocket =
          client.newWebSocket(
              request,
              new WebSocketListener() {
                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                  // For the moment no message from the remote server is sent
                  // (i.e. no handling is required)
                }
              });
      Log.d(TAG, "Connected to server " + serverUrl);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Error on the url provided", e);
    }
  }

  @SuppressLint("LogNotTimber")
  private static void closeWebSocket() {
    // Take the lock and then close the websocket
    synchronized (lock) {
      if (webSocket != null) {
        webSocket.close(1000, null);
        webSocket = null;
        Log.d(TAG, "Disconnected from server " + serverUrl);
      }
    }
  }

  /**
   * Function that builds the log string to send to the server
   *
   * @param priority Debug, Info, Error, Warn
   * @param tag class name
   * @param message log message
   * @param error stack trace of the error if present, empty if no error
   * @return log string
   */
  private String buildRemoteLog(int priority, String tag, String message, String error) {
    // Transform the priority code into a string
    String priorityString;
    switch (priority) {
      case Log.DEBUG:
        priorityString = "D/:";
        break;
      case Log.INFO:
        priorityString = "I/:";
        break;
      case Log.WARN:
        priorityString = "W/:";
        break;
      case Log.ERROR:
        priorityString = "E/:";
        break;
      default:
        priorityString = "UNKNOWN/:";
    }

    // Take the UTC-timezone timestamp: [yyyy-mm-ddThh:mm:ss.msZ]
    String timestamp = String.valueOf(Clock.systemUTC().instant());

    return error.isEmpty()
        ? String.format("[%s] - %s %s: %s", timestamp, priorityString, tag, message)
        : String.format(
            "[%s] - %s %s: %s%nERROR: %s", timestamp, priorityString, tag, message, error);
  }
}
