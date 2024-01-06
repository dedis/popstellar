package com.github.dedis.popstellar.utility

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import com.github.dedis.popstellar.R
import java.time.Clock
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

/**
 * This class extends the default Timber.Tree to implement a custom logger which decides whether to
 * forward logs also to a server or to print them on the console.
 */
class NetworkLogger : Timber.Tree() {
  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    // Take the stack trace of the error if present
    val error = if (t == null) "" else Log.getStackTraceString(t)

    // Always print the log to console
    if (t != null) {
      Log.println(
          priority,
          tag,
          """
     $message
     $error
     """
              .trimIndent())
    } else {
      Log.println(priority, tag, message)
    }

    // Send the log message to the remote server
    if (sendToServer.get()) {
      val log = buildRemoteLog(priority, tag, message, error)
      sendToServer(log)
    }
  }

  private fun sendToServer(log: String) {
    // Take the lock and send the log as UTF-8 encoded string
    synchronized(lock) {

      // If the websocket hasn't been already opened then open it
      if (webSocket == null) {
        connectWebSocket()
      }
      // Connect may fail
      if (webSocket != null) {
        webSocket!!.send(log)
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
  private fun buildRemoteLog(priority: Int, tag: String?, message: String, error: String): String {
    // Transform the priority code into a string
    val priorityString: String =
        when (priority) {
          Log.DEBUG -> "D/:"
          Log.INFO -> "I/:"
          Log.WARN -> "W/:"
          Log.ERROR -> "E/:"
          else -> "UNKNOWN/:"
        }

    // Take the UTC-timezone timestamp: [yyyy-mm-ddThh:mm:ss.msZ]
    val timestamp = Clock.systemUTC().instant().toString()
    return if (error.isEmpty()) "[$timestamp] - $priorityString $tag: $message"
    else "[$timestamp] - $priorityString $tag: $message%nERROR: $error"
  }

  companion object {
    private val TAG = NetworkLogger::class.java.simpleName

    /** Boolean flag whether to close the websocket connection whenever the application pauses */
    private const val SAVE_ENERGY = false

    /** URL of the remote server to which sending the logs */
    private val serverUrl = StringBuilder()
    private var webSocket: WebSocket? = null

    /**
     * As the connection is implemented static to be opened and closed only once, its access is
     * synchronized through this lock
     */
    private val lock = Any()

    /**
     * Boolean which is atomically set by the user from the settings and checked by the loggers.
     * This value is set based on the settings, which is persisted even after the application is
     * closed
     */
    private val sendToServer = AtomicBoolean(false)

    /**
     * Initialize the value of server url and the enable flag from the application context, based on
     * the preference value persisted in memory. Register the callback to shut down the connection
     * when the app is closed.
     *
     * @param application PoP application
     */
    fun loadFromPersistPreference(application: Application) {
      // Retrieve the server url and the enable flag from the persisted preferences
      val context = application.applicationContext
      sendToServer.set(
          PreferenceManager.getDefaultSharedPreferences(context)
              .getBoolean(context.getString(R.string.settings_logging_key), false))
      serverUrl.append(
          PreferenceManager.getDefaultSharedPreferences(context)
              .getString(context.getString(R.string.settings_server_url_key), ""))

      // Register the callback for a graceful shutdown
      val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
          EnumMap(Lifecycle.Event::class.java)
      consumerMap[Lifecycle.Event.ON_DESTROY] = Consumer { closeWebSocket() }
      val saverConsumer = Consumer { _: Activity ->
        // Close the websocket on pause and stop only if we we want to save battery
        if (SAVE_ENERGY) {
          closeWebSocket()
        }
      }
      consumerMap[Lifecycle.Event.ON_PAUSE] = saverConsumer
      consumerMap[Lifecycle.Event.ON_STOP] = saverConsumer
      application.registerActivityLifecycleCallbacks(
          ActivityUtils.buildLifecycleCallback(consumerMap))
    }

    /** Function to enable the remote logging. It opens the websocket */
    @JvmStatic
    fun enableRemote() {
      // Open the websocket connection
      connectWebSocket()
      // Enable the log method to forward logs to the server
      sendToServer.set(true)
    }

    /** Function to disable the remote logging. It closes the websocket */
    @JvmStatic
    fun disableRemote() {
      // Stop the logs to be crafted and sent to server, continue to print on console
      sendToServer.set(false)
      // Close the websocket connection
      closeWebSocket()
    }

    @JvmStatic
    fun setServerUrl(url: String) {
      serverUrl.replace(0, serverUrl.length, url)
    }

    /** Function which opens the web socket with the server */
    @SuppressLint("LogNotTimber")
    private fun connectWebSocket() {
      try {
        val request: Request = Builder().url(serverUrl.toString()).build()
        val client = OkHttpClient()
        // Create the socket with an empty listener
        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                  override fun onMessage(webSocket: WebSocket, text: String) {
                    // For the moment no message from the remote server is sent
                    // (i.e. no handling is required)
                  }
                })
        Log.d(TAG, "Connected to server $serverUrl")
      } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Error on the url provided", e)
      }
    }

    @SuppressLint("LogNotTimber")
    private fun closeWebSocket() {
      // Take the lock and then close the websocket
      synchronized(lock) {
        if (webSocket != null) {
          webSocket!!.close(1000, null)
          webSocket = null
          Log.d(TAG, "Disconnected from server $serverUrl")
        }
      }
    }
  }
}
