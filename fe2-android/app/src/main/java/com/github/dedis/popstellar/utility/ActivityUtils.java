package com.github.dedis.popstellar.utility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.io.*;
import java.util.function.Supplier;

public class ActivityUtils {
  private static final String TAG = ActivityUtils.class.getSimpleName();

  private static final String PERSISTENT_DATA_FILE_NAME = "persistent_data";

  public static void setFragmentInContainer(
      FragmentManager manager, int containerId, int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = manager.findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(manager, fragment, containerId);
  }

  public static void replaceFragmentInActivity(
      @NonNull FragmentManager fragmentManager, @NonNull Fragment fragment, int frameId) {
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(frameId, fragment);
    transaction.commit();
  }

  public static boolean storePersistentData(Context context, PersistentData data) {
    Log.d(TAG, "Initiating storage of " + data);

    try {
      FileOutputStream fos =
          context.openFileOutput(PERSISTENT_DATA_FILE_NAME, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(data);
      oos.close();
      fos.close();
    } catch (IOException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.error_storing_data);
      return false;
    }
    Log.d(TAG, "storage successful");
    return true;
  }

  public static PersistentData loadPersistentData(Context context) {
    Log.d(TAG, "Initiating loading of data");

    PersistentData persistentData;
    try {
      FileInputStream fis = context.openFileInput(PERSISTENT_DATA_FILE_NAME);
      ObjectInputStream ois = new ObjectInputStream(fis);
      persistentData = (PersistentData) ois.readObject();
      ois.close();
      fis.close();
    } catch (IOException | ClassNotFoundException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.error_storing_data);
      return null;
    }

    Log.d(TAG, "loading of " + persistentData);
    return persistentData;
  }
}
