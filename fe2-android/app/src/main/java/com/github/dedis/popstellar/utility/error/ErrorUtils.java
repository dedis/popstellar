package com.github.dedis.popstellar.utility.error;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.github.dedis.popstellar.R;

import org.bouncycastle.util.Arrays;

import java.util.concurrent.TimeoutException;

public class ErrorUtils {

  private ErrorUtils() throws IllegalAccessException {
    throw new IllegalAccessException();
  }

  /**
   * Log and error and show a localized {@link Toast} to the user
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

    Log.e(tag, exceptionMsg, error);

    //This makes it so that the toast is run on the UI thread
    //Otherwise it would crash
    Log.d(tag, "We got there");
    new Handler(context.getMainLooper()).post(
            () -> Toast.makeText(context, exceptionMsg, Toast.LENGTH_LONG).show());
  }

  private static String getLocalizedMessage(
      Context context, Throwable error, @StringRes int action, String... actionArgs) {
    String errorSpecificMessage = getErrorSpecificMessage(context, error);
    actionArgs = Arrays.append(actionArgs, errorSpecificMessage);
    return context.getString(action, (Object) actionArgs);
  }

  private static String getErrorSpecificMessage(Context context, Throwable error) {
    if (error instanceof GenericException) {
      GenericException exception = (GenericException) error;
      return context.getString(
          exception.getUserMessage(), (Object) exception.getUserMessageArguments());
    } else if (error instanceof TimeoutException) {
      return context.getString(R.string.timeout_exception);
    } else {
      // It is not a known error, let's log it but not show information to
      // the average user as it will not be useful to him.
      Log.d(
          ErrorUtils.class.getSimpleName(),
          "Error " + error.getClass() + " is not a know error type.");
      return "";
    }
  }
}
