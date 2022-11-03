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

    /**
     * Store data in the phone's persistent storage
     * @param context of the UI
     * @param data the data to store
     * @return true if the storage process was a success; false otherwise
     */
  public static boolean storePersistentData(Context context, PersistentData data) {
    Log.d(TAG, "Initiating storage of " + data);

    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            context.openFileOutput(PERSISTENT_DATA_FILE_NAME, Context.MODE_PRIVATE))) {
      oos.writeObject(data);
    } catch (IOException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.error_storing_data);
      return false;
    }
    Log.d(TAG, "storage successful");
    return true;
  }

    /**
     * Load the persistent data from the phone's persistent storage
     * @param context of the UI
     * @return the data if found, null otherwise
     */
  public static PersistentData loadPersistentData(Context context) {
    Log.d(TAG, "Initiating loading of data");

    PersistentData persistentData;
    try (ObjectInputStream ois =
        new ObjectInputStream(context.openFileInput(PERSISTENT_DATA_FILE_NAME))) {
      persistentData = (PersistentData) ois.readObject();
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

    /**
     * Clear the phone's persistent storage for the PoP app
     * @param context of the UI
     * @return true if the clearing was a success; false otherwise
     */
  public static boolean clearStorage(Context context) {
    Log.d(TAG, "clearing data");

    File file = new File(context.getFilesDir(), PERSISTENT_DATA_FILE_NAME);
    return file.delete();
  }

    /**
     * This performs the steps of getting and storing persistently the needed data
     * @param networkManager, the singleton used across the app
     * @param wallet, the singleton used across the app
     * @param context of the UI
     * @return true if the saving process was a success; false otherwise
     * @throws GeneralSecurityException
     */
  public static boolean activitySavingRoutine(
      GlobalNetworkManager networkManager, Wallet wallet, Context context)
      throws GeneralSecurityException {
    String serverAddress = networkManager.getCurrentUrl();
    if (serverAddress == null) {
      return false;
    }
    Set<Channel> subscriptions = networkManager.getMessageSender().getSubscriptions();
    if (subscriptions == null) {
      subscriptions = new HashSet<>();
    }

    String[] seed = wallet.exportSeed();
    Log.d(
        TAG,
        "seed length"
            + seed.length
            + " address "
            + serverAddress
            + " subscriptions "
            + subscriptions);
    return storePersistentData(context, new PersistentData(seed, serverAddress, subscriptions));
  }
}
