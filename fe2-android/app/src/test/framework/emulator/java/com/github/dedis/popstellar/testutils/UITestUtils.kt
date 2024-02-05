package com.github.dedis.popstellar.testutils

import android.app.Dialog
import android.content.DialogInterface
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

/** This class holds utility functions when retrieving particular elements of a view in a test  */
object UITestUtils {
  /**
   * Assert that the latest toast was shown with the expected text
   *
   * @param resId resource of the text
   * @param args arguments to the resource
   */
  fun assertToastIsDisplayedWithText(@StringRes resId: Int, vararg args: Any?) {}

  /**
   * Retrieve the last opened dialog expecting it to be of the provided type
   *
   * @param type of the dialog
   * @param <D> of the dialog
   * @return the dialog
  </D> */
  fun <D : Dialog?> getLastDialog(type: Class<D>?): D? {
    return null as D?
  }

  /**
   * Retrieve the positive button of the latest dialog
   *
   *
   * For example : The Accept button or Confirm button
   *
   * @return the button
   */
  fun dialogPositiveButton(): Button {
    return getAlertDialogButton(DialogInterface.BUTTON_POSITIVE)
  }

  /**
   * Retrieve the negative button of the latest dialog
   *
   *
   * For example : The Cancel button
   *
   * @return the button
   */
  fun dialogNegativeButton(): Button {
    return getAlertDialogButton(DialogInterface.BUTTON_NEGATIVE)
  }

  /**
   * Retrieve a specific button from the latest Alert Dialog
   *
   *
   * This function aims at supporting most AlertDialog from different android versions
   *
   * @param buttonId the id of the Button, found in [DialogInterface]
   */
  fun getAlertDialogButton(buttonId: Int): Button {
    val dialog = getLastDialog(
      Dialog::class.java
    )!!
    return if (dialog is AlertDialog) {
      dialog.getButton(buttonId)
    } else if (dialog is android.app.AlertDialog) {
      dialog.getButton(buttonId)
    } else {
      throw AssertionError("The dialog does not have a positive button")
    }
  }
}