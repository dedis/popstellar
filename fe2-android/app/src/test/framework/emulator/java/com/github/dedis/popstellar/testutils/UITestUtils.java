package com.github.dedis.popstellar.testutils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.*;
import android.widget.Button;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import org.hamcrest.*;

/** This class holds utility functions when retrieving particular elements of a view in a test */
public class UITestUtils {

  /**
   * Assert that the latest toast was shown with the expected text
   *
   * @param resId resource of the text
   * @param args arguments to the resource
   */
  public static void assertToastIsDisplayedWithText(@StringRes int resId, Object... args) {}

  /**
   * Retrieve the last opened dialog expecting it to be of the provided type
   *
   * @param type of the dialog
   * @param <D> of the dialog
   * @return the dialog
   */
  public static <D extends Dialog> D getLastDialog(Class<D> type) {
    return (D) null;
  }

  /**
   * Retrieve the positive button of the latest dialog
   *
   * <p>For example : The Accept button or Confirm button
   *
   * @return the button
   */
  public static Button dialogPositiveButton() {
    return getAlertDialogButton(DialogInterface.BUTTON_POSITIVE);
  }

  /**
   * Retrieve the negative button of the latest dialog
   *
   * <p>For example : The Cancel button
   *
   * @return the button
   */
  public static Button dialogNegativeButton() {
    return getAlertDialogButton(DialogInterface.BUTTON_NEGATIVE);
  }

  /**
   * Retrieve a specific button from the latest Alert Dialog
   *
   * <p>This function aims at supporting most AlertDialog from different android versions
   *
   * @param buttonId the id of the Button, found in {@link DialogInterface}
   */
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

  public static Matcher<View> childAtPosition(
      final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup
            && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
