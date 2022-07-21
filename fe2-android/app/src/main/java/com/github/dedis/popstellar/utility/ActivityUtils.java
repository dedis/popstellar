package com.github.dedis.popstellar.utility;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;

public class ActivityUtils {

  public static void replaceFragmentInActivity(
      @NonNull FragmentManager fragmentManager, @NonNull Fragment fragment, int frameId) {
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(frameId, fragment);
    transaction.commit();
  }
}
