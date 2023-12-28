package com.github.dedis.popstellar.utility.error

import android.content.Context
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import com.github.dedis.popstellar.R
import org.bouncycastle.util.Arrays
import timber.log.Timber
import java.util.concurrent.TimeoutException

class ErrorUtils private constructor() {
    init {
        throw IllegalAccessException()
    }

    companion object {
        private val TAG = ErrorUtils::class.java.simpleName

        /**
         * Log non-error and show a localized [Toast] to the user
         *
         * @param context to retrieve the localized messages and show the toast on
         * @param tag used to log the error
         * @param action short description of what was happening when the error occurred
         */
        @JvmStatic
        fun logAndShow(context: Context, tag: String, @StringRes action: Int) {
            val message = context.getString(action)
            Timber.tag(tag).e(message)
            displayToast(context, message)
        }

        /**
         * Log error and show a localized [Toast] to the user
         *
         *
         * action is expected to leave a string placeholder for the error specific message at the last
         * place
         *
         *
         * Ex: "An error occurred while processing %1$s : %2$s
         *
         * @param context to retrieve the localized messages and show the toast on
         * @param tag used to log the error
         * @param error to log
         * @param action short description of what was happening when the error occurred
         * @param actionArgs arguments to the message
         */
        @JvmStatic
        fun logAndShow(
            context: Context,
            tag: String,
            error: Throwable,
            @StringRes action: Int,
            vararg actionArgs: String?
        ) {
            val exceptionMsg = getLocalizedMessage(context, error, action, *actionArgs)
            Timber.tag(tag).e(error)
            displayToast(context, exceptionMsg)
        }

        private fun displayToast(context: Context, text: String) {
            // This makes it so that the toast is run on the UI thread
            // Otherwise it would crash
            Handler(context.mainLooper)
                .post { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
        }

        private fun getLocalizedMessage(
            context: Context, error: Throwable, @StringRes action: Int, vararg actionArgs: String?
        ): String {
            val errorSpecificMessage = getErrorSpecificMessage(context, error)
            val args = Arrays.append(actionArgs, errorSpecificMessage)
            return context.getString(action, java.util.Arrays.toString(args))
        }

        private fun getErrorSpecificMessage(context: Context, error: Throwable): String {
            return when (error) {
                is GenericException -> {
                    context.getString(error.userMessage, *error.userMessageArguments)
                }

                is TimeoutException -> {
                    context.getString(R.string.timeout_exception)
                }

                else -> {
                    // It is not a known error, let's log it but not show information to
                    // the average user as it will not be useful to him.
                    Timber.tag(TAG).d("Error %s is not a know error type", error.javaClass)
                    ""
                }
            }
        }
    }
}