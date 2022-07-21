package com.github.dedis.popstellar.ui.pages.qrcode;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionFragment;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This is the page object of CameraPermissionFragment
 *
 * <p>It makes writing test easier
 */
public class CameraPermissionPageObject {

  public static ViewInteraction allowCameraButton() {
    return onView(withId(R.id.allow_camera_button));
  }

  public static String getRequestKey() {
    return CameraPermissionFragment.REQUEST_KEY;
  }
}
