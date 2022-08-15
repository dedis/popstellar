package com.github.dedis.popstellar.utility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
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
    } catch (FileNotFoundException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.nothing_stored);
      return null;
    } catch (IOException | ClassNotFoundException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.error_loading_data);
      return null;
    }

    Log.d(TAG, "loading of " + persistentData);
    return persistentData;
  }

  public static boolean clearStorage(Context context) {
    Log.d(TAG, "clearing data");

    File file = new File(context.getFilesDir(), PERSISTENT_DATA_FILE_NAME);
    return file.delete();
  }

  public static boolean activitySavingRoutine(
      GlobalNetworkManager networkManager, Wallet wallet, Context context) {
    String serverAddress = networkManager.getCurrentUrl();
    Set<Channel> subscriptions = networkManager.getMessageSender().getSubscriptions();
    if (subscriptions == null) {
      subscriptions = new HashSet<>();
    }

    String[] seed;
    try {
      seed = wallet.exportSeed();
    } catch (GeneralSecurityException e) {
      Log.e(TAG, "Error exporting wallet seed : " + e);
      return false;
    }
    if (seed == null) {
      // it returns empty array if not initialized
      throw new IllegalStateException("Seed should not be null");
    }
    Log.d(
        TAG,
        "seed "
            + Arrays.toString(seed)
            + " address "
            + serverAddress
            + " subscriptions "
            + subscriptions);
    return storePersistentData(context, new PersistentData(seed, serverAddress, subscriptions));
  }
}
