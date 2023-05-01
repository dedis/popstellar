package com.github.dedis.popstellar.utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.room.EmptyResultSetException;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.repository.database.core.CoreDao;
import com.github.dedis.popstellar.repository.database.core.CoreEntity;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Supplier;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ActivityUtils {
  private static final String TAG = ActivityUtils.class.getSimpleName();

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

  public static void setFragmentInContainer(
      FragmentManager manager, int containerId, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = fragmentSupplier.get();

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
   * This performs the steps of getting and storing persistently the needed data
   *
   * @param networkManager, the singleton used across the app
   * @param wallet, the singleton used across the app
   */
  @SuppressLint("CheckResult")
  public static void activitySavingRoutine(
      GlobalNetworkManager networkManager, Wallet wallet, CoreDao coreDao)
      throws GeneralSecurityException {
    String serverAddress = networkManager.getCurrentUrl();
    if (serverAddress == null) {
      return;
    }

    Set<Channel> subscriptions;
    if (networkManager.getNullableMessageSender() == null
        || networkManager.getMessageSender().getSubscriptions() == null) {
      subscriptions = new HashSet<>();
    } else {
      subscriptions = networkManager.getMessageSender().getSubscriptions();
    }

    String[] seed = wallet.exportSeed();

    CoreEntity coreEntity =
        new CoreEntity(
            serverAddress, Collections.unmodifiableList(Arrays.asList(seed)), subscriptions);

    // Search if previous entry was there
    coreDao
        .getSettings()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorResumeNext(
            err -> {
              if (err instanceof EmptyResultSetException) {
                return Single.just(CoreEntity.getEmptyEntity());
              } else {
                return Single.error(err);
              }
            })
        .subscribe(
            entity -> {
              // Use same id such that we can replace the entry
              if (!entity.equals(CoreEntity.getEmptyEntity())) {
                coreEntity.setId(entity.getId());
              }
              coreDao
                  .insert(coreEntity)
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .doOnComplete(
                      () ->
                          Timber.tag(TAG)
                              .d(
                                  "Persisted seed length: %d, address: %s, subscriptions: %s",
                                  seed.length, serverAddress, subscriptions))
                  .subscribe();
            });
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
