package com.github.dedis.popstellar.utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.github.dedis.popstellar.R;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.*;
import timber.log.Timber;

/**
 * This class extends the default Timber.Tree to implement a custom logger which decides whether to
 * forward logs also to a server or to print them on the console.
 */
public class NetworkLogger extends Timber.Tree {

  private static final String TAG = NetworkLogger.class.getSimpleName();

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

  @Override
  protected void log(int priority, String tag, @NonNull String message, Throwable t) {
    // Take the stack trace of the error if present
    String error = t == null ? "" : Log.getStackTraceString(t);

    // Print the log to console
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

  /**
   * Initialize the value of server url and the enable flag from the application context, based on
   * the preference value persisted in memory
   *
   * @param context application context
   */
  public static void loadFromPersistPreference(Context context) {
    // Retrieve the server url and the enable flag from the persisted preferences
    sendToServer.set(
        (PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.settings_logging_key), false)));
    serverUrl.append(
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.settings_server_url_key), ""));
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

    String timestamp = String.valueOf(Clock.systemUTC().instant());

    return error.isEmpty()
        ? String.format("[%s] - %s %s: %s", timestamp, priorityString, tag, message)
        : String.format(
            "[%s] - %s %s: %s%nERROR: %s", timestamp, priorityString, tag, message, error);
  }

  /** Function to enable the remote logging. It opens the websocket */
  public static void enableRemote() {
    connectWebSocket();
    sendToServer.set(true);
  }

  /** Function to disable the remote logging. It closes the websocket */
  public static void disableRemote() {
    sendToServer.set(false);
    closeWebSocket();
  }

  public static void setServerUrl(String url) {
    serverUrl.replace(0, serverUrl.length(), url);
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
                  // For now no message from the remote server is sent, so no handling required
                }
              });
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Error on the url provided", e);
    }
  }

  private static void closeWebSocket() {
    // Take the lock and then close the websocket
    synchronized (lock) {
      if (webSocket != null) {
        webSocket.close(1000, null);
        webSocket = null;
      }
    }
  }

  private void sendToServer(String log) {
    // Take the lock and send the log as UTF-8 encoded string
    synchronized (lock) {
      if (webSocket == null) {
        connectWebSocket();
      }
      // Connect may fail
      if (webSocket != null) {
        webSocket.send(log);
      }
    }
  }
}
