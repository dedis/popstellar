package com.github.dedis.popstellar.testutils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.Is
import org.junit.Assert
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast

/** This class holds utility functions when retrieving particular elements of a view in a test */
object UITestUtils {
  /**
   * Assert that the latest toast was shown with the expected text
   *
   * @param resId resource of the text
   * @param args arguments to the resource
   */
  @JvmStatic
  fun assertToastIsDisplayedWithText(@StringRes resId: Int, vararg args: Any?) {
    MatcherAssert.assertThat(
      "No toast was displayed",
      ShadowToast.getLatestToast(),
      Matchers.notNullValue()
    )

    val expected = ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)
    Assert.assertEquals(expected, ShadowToast.getTextOfLatestToast())
  }

  @JvmStatic
  fun assertToastIsDisplayedContainsText(@StringRes resId: Int, vararg args: Any?) {
    MatcherAssert.assertThat(
      "No toast was displayed",
      ShadowToast.getLatestToast(),
      Matchers.notNullValue()
    )

    val expected = ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)
    Assert.assertTrue(ShadowToast.getTextOfLatestToast().contains(expected))
  }

  @JvmStatic
  fun assertLatestToastContent(isContained: Boolean, @StringRes resId: Int, vararg args: Any?) {
    val expected = ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)
    Assert.assertEquals(isContained, ShadowToast.getTextOfLatestToast()?.contains(expected)
      ?: false)
  }

  /**
   * Retrieve the last opened dialog expecting it to be of the provided type
   *
   * @param type of the dialog
   * @param <D> of the dialog
   * @return the dialog
   */
  @JvmStatic
  fun <D : Dialog?> getLastDialog(type: Class<D>): D {
    val dialog = ShadowDialog.getLatestDialog()

    MatcherAssert.assertThat("No dialog has been displayed", dialog, Matchers.notNullValue())
    MatcherAssert.assertThat(
      "The dialog is not a " + type.simpleName,
      dialog,
      Is.`is`(Matchers.instanceOf(type))
    )

    return dialog as D
  }

  /**
   * Retrieve the positive button of the latest dialog
   *
   * For example : The Accept button or Confirm button
   *
   * @return the button
   */
  @JvmStatic
  fun dialogPositiveButton(): Button {
    return getAlertDialogButton(DialogInterface.BUTTON_POSITIVE)
  }

  /**
   * Retrieve the negative button of the latest dialog
   *
   * For example : The Cancel button
   *
   * @return the button
   */
  @JvmStatic
  fun dialogNegativeButton(): Button {
    return getAlertDialogButton(DialogInterface.BUTTON_NEGATIVE)
  }

  /**
   * Retrieve the neutral button of the latest dialog
   *
   * For example : The OK button
   *
   * @return the button
   */
  @JvmStatic
  fun dialogNeutralButton(): Button {
    return getAlertDialogButton(DialogInterface.BUTTON_NEUTRAL)
  }

  /**
   * Retrieve a specific button from the latest Alert Dialog
   *
   * This function aims at supporting most AlertDialog from different android versions
   *
   * @param buttonId the id of the Button, found in [DialogInterface]
   */
  fun getAlertDialogButton(buttonId: Int): Button {
    val dialog = getLastDialog(Dialog::class.java)

    return when (dialog) {
      is AlertDialog -> {
        dialog.getButton(buttonId)
      }
      is android.app.AlertDialog -> {
        dialog.getButton(buttonId)
      }
      else -> {
        throw AssertionError("The dialog does not have a positive button")
      }
    }
  }

  fun childAtPosition(parentMatcher: Matcher<View?>, position: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

      override fun describeTo(description: Description) {
        description.appendText("Child at position $position in parent ")
        parentMatcher.describeTo(description)
      }

      public override fun matchesSafely(view: View): Boolean {
        val parent = view.parent
        return (parent is ViewGroup &&
          parentMatcher.matches(parent) &&
          view == parent.getChildAt(position))
      }
    }
  }

  /**
   * Is used to type text in EditTexts programmatically. roboelectric sometimes des not write all
   * characters like ":"
   *
   * @param text to type
   * @return A ViewAction to be performed
   */
  @JvmStatic
  fun forceTypeText(text: String?): ViewAction {
    return object : ViewAction {
      override fun getDescription(): String {
        return "force type text"
      }

      override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.isEnabled())
      }

      override fun perform(uiController: UiController, view: View) {
        val editText = view as EditText
        editText.append(text)
        uiController.loopMainThreadUntilIdle()
      }
    }
  }
}
