package com.github.dedis.popstellar.utility;

import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class NetworkLogger extends Timber.Tree {
  private static final String SERVER_URL = "url";

  public static final AtomicBoolean sendToServer = new AtomicBoolean(false);

  @Override
  protected void log(int priority, String tag, @NonNull String message, Throwable t) {
    // Print the log to console
    Timber.tag(tag).log(priority, message, t);

    // Send the log message to the remote server
    if (sendToServer.get()) {
      sendToServer(
          String.format(
              "[%s] [%s] %s: %s\nE/: %s",
              Clock.systemUTC().instant(),
              getPriorityString(priority),
              tag,
              message,
              t.getMessage()));
    }
  }

  public static void enableRemote() {
    sendToServer.set(true);
  }

  public static void disableRemote() {
    sendToServer.set(false);
  }

  private void sendToServer(String log) {}

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
