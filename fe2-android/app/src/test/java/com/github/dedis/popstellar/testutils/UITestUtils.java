package com.github.dedis.popstellar.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Button;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

public class UITestUtils {

  public static void assertToastIsShown(@StringRes int resId, Object... args) {
    assertThat("No toast have been displayed", ShadowToast.getLatestToast(), notNullValue());

    String expected = ApplicationProvider.getApplicationContext().getString(resId, args);
    assertEquals(expected, ShadowToast.getTextOfLatestToast());
  }

  public static <D extends Dialog> D getLastDialog(Class<D> type) {
    Dialog dialog = ShadowDialog.getLatestDialog();
    assertThat("No dialog have been displayed", dialog, notNullValue());
    assertThat("The dialog is not a " + type.getSimpleName(), dialog, is(instanceOf(type)));
    //noinspection unchecked
    return (D) dialog;
  }

  public static Button dialogPositiveButton() {
    return getAlertDialogButton(DialogInterface.BUTTON_POSITIVE);
  }

  public static Button dialogNegativeButton() {
    return getAlertDialogButton(DialogInterface.BUTTON_NEGATIVE);
  }

  public static Button getAlertDialogButton(int buttonId) {
    Dialog dialog = getLastDialog(Dialog.class);
    if (dialog instanceof AlertDialog) {
      return ((AlertDialog) dialog).getButton(buttonId);
    } else if (dialog instanceof android.app.AlertDialog) {
      return ((android.app.AlertDialog) dialog).getButton(buttonId);
    } else {
      throw new AssertionError("The dialog does not have a positive button");
    }
  }
}
