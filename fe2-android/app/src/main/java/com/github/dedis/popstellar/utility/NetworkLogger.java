package com.github.dedis.popstellar.utility;

import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.*;
import timber.log.Timber;

public class NetworkLogger extends Timber.Tree {
  private static final String SERVER_URL = "url";

  private static WebSocket webSocket;
  private static final Object lock = new Object();

  public static final AtomicBoolean sendToServer = new AtomicBoolean(false);

  @Override
  protected void log(int priority, String tag, @NonNull String message, Throwable t) {
    String error = t == null ? "" : Log.getStackTraceString(t);
    // Print the log to console
    if (t != null) {
      Log.println(priority, tag, message + "\n" + error);
    } else {
      Log.println(priority, tag, message);
    }

    // Send the log message to the remote server
    if (sendToServer.get()) {
      String log =
          t == null
              ? String.format(
                  "[%s]-[%s]-%s: %s",
                  Clock.systemUTC().instant(), getPriorityString(priority), tag, message)
              : String.format(
                  "[%s]-[%s]-%s: %s\nERROR: %s",
                  Clock.systemUTC().instant(), getPriorityString(priority), tag, message, error);
      sendToServer(log);
    }
  }

  public static void enableRemote() {
    connectWebSocket();
    sendToServer.set(true);
  }

  public static void disableRemote() {
    sendToServer.set(false);
    closeWebSocket();
  }

  private static void connectWebSocket() {
    Request request = new Request.Builder().url(SERVER_URL).build();
    OkHttpClient client = new OkHttpClient();
    webSocket =
        client.newWebSocket(
            request,
            new WebSocketListener() {
              @Override
              public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                // For now no message from the remote server is sent, so no handling required
              }
            });
  }

  private static void closeWebSocket() {
    synchronized (lock) {
      if (webSocket != null) {
        webSocket.close(1000, null);
        webSocket = null;
      }
    }
  }

  private void sendToServer(String log) {
    synchronized (lock) {
      if (webSocket == null) {
        connectWebSocket();
      }
      webSocket.send(log);
    }
  }

  private String getPriorityString(int priority) {
    switch (priority) {
      case Log.DEBUG:
        return "D/";
      case Log.INFO:
        return "I/";
      case Log.WARN:
        return "W/";
      case Log.ERROR:
        return "E/";
      default:
        return "UNKNOWN/";
    }
  }
}
