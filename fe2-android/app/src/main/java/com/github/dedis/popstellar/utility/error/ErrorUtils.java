package com.github.dedis.popstellar.utility.error;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.github.dedis.popstellar.R;

import org.bouncycastle.util.Arrays;

import java.util.concurrent.TimeoutException;

import timber.log.Timber;

public class ErrorUtils {

  private static final String TAG = ErrorUtils.class.getSimpleName();

  private ErrorUtils() throws IllegalAccessException {
    throw new IllegalAccessException();
  }

  /**
   * Log non-error and show a localized {@link Toast} to the user
   *
   * @param context to retrieve the localized messages and show the toast on
   * @param tag used to log the error
   * @param action short description of what was happening when the error occurred
   */
  public static void logAndShow(Context context, String tag, @StringRes int action) {
    String message = context.getString(action);

    Timber.tag(tag).e(message);
    displayToast(context, message);
  }

  /**
   * Log error and show a localized {@link Toast} to the user
   *
   * <p>action is expected to leave a string placeholder for the error specific message at the last
   * place
   *
   * <p>Ex: "An error occurred while processing %1$s : %2$s
   *
   * @param context to retrieve the localized messages and show the toast on
   * @param tag used to log the error
   * @param error to log
   * @param action short description of what was happening when the error occurred
   * @param actionArgs arguments to the message
   */
  public static void logAndShow(
      Context context, String tag, Throwable error, @StringRes int action, String... actionArgs) {
    String exceptionMsg = getLocalizedMessage(context, error, action, actionArgs);

    Timber.tag(tag).e(error, exceptionMsg);
    displayToast(context, exceptionMsg);
  }

  private static void displayToast(Context context, String text) {
    // This makes it so that the toast is run on the UI thread
    // Otherwise it would crash
    new Handler(context.getMainLooper())
        .post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
  }

  private static String getLocalizedMessage(
      Context context, Throwable error, @StringRes int action, String... actionArgs) {
    String errorSpecificMessage = getErrorSpecificMessage(context, error);
    actionArgs = Arrays.append(actionArgs, errorSpecificMessage);
    return context.getString(action, java.util.Arrays.toString(actionArgs));
  }

  private static String getErrorSpecificMessage(Context context, Throwable error) {
    if (error instanceof GenericException) {
      GenericException exception = (GenericException) error;
      return context.getString(exception.getUserMessage(), exception.getUserMessageArguments());
    } else if (error instanceof TimeoutException) {
      return context.getString(R.string.timeout_exception);
    } else {
      // It is not a known error, let's log it but not show information to
      // the average user as it will not be useful to him.
      Timber.tag(TAG).d("Error %s is not a know error type", error.getClass());
      return "";
    }
  }
}
