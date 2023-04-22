package com.github.dedis.popstellar.utility;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.activity.OnBackPressedCallback;
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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import timber.log.Timber;

public class ActivityUtils {
  private static final String TAG = ActivityUtils.class.getSimpleName();

  private static final String PERSISTENT_DATA_FILE_NAME = "persistent_data";

  public static void setFragmentInContainer(
      FragmentManager manager, int containerId, int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = manager.findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) {
      fragment = fragmentSupplier.get();
    }

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
   *
   * @param context of the UI
   * @param data the data to store
   * @return true if the storage process was a success; false otherwise
   */
  public static boolean storePersistentData(Context context, PersistentData data) {
    Timber.tag(TAG).d("Initiating storage of %s", data);

    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            context.openFileOutput(PERSISTENT_DATA_FILE_NAME, Context.MODE_PRIVATE))) {
      oos.writeObject(data);
    } catch (IOException e) {
      ErrorUtils.logAndShow(context, TAG, e, R.string.error_storing_data);
      return false;
    }
    Timber.tag(TAG).d("storage successful");
    return true;
  }

  /**
   * Load the persistent data from the phone's persistent storage
   *
   * @param context of the UI
   * @return the data if found, null otherwise
   */
  public static PersistentData loadPersistentData(Context context) {
    Timber.tag(TAG).d("Initiating loading of data");

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

    Timber.tag(TAG).d("loading of %s", persistentData);
    return persistentData;
  }

  /**
   * Clear the phone's persistent storage for the PoP app
   *
   * @param context of the UI
   * @return true if the clearing was a success; false otherwise
   */
  public static boolean clearStorage(Context context) {
    Timber.tag(TAG).d("clearing data");

    File file = new File(context.getFilesDir(), PERSISTENT_DATA_FILE_NAME);
    return file.delete();
  }

  /**
   * This performs the steps of getting and storing persistently the needed data
   *
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
    Timber.tag(TAG)
        .d(
            "seed length: %d, address: %s, subscriptions: %s",
            seed.length, serverAddress, subscriptions);
    return storePersistentData(context, new PersistentData(seed, serverAddress, subscriptions));
  }

  /**
   * The following function creates an object of type OnBackPressedCallback given a specific
   * callback function. This avoids code repetitions.
   *
   * @param tag String tag for the log
   * @param message String message for the log
   * @param callback Runnable function to use * as callback
   * @return the callback object
   */
  public static OnBackPressedCallback buildBackButtonCallback(
      String tag, String message, Runnable callback) {
    return new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        Timber.tag(tag).d("Back pressed, going to %s", message);
        callback.run();
      }
    };
  }

  /**
   * Gets the color of the QR code based on the night mode configuration of the current context.
   *
   * @return the color of the QR code (either Color.WHITE or Color.BLACK)
   */
  public static int getQRCodeColor(Context context) {
    Configuration configuration = context.getResources().getConfiguration();
    int nightModeFlags = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
      return Color.WHITE;
    }
    return Color.BLACK;
  }
}
