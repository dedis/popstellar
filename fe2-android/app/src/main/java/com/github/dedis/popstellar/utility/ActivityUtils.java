package com.github.dedis.popstellar.utility;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.*;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao;
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity;
import com.github.dedis.popstellar.repository.database.wallet.WalletDao;
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.github.dedis.popstellar.utility.Constants.ORIENTATION_DOWN;
import static com.github.dedis.popstellar.utility.Constants.ORIENTATION_UP;

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
   * This performs the persistent storage of the wallet.
   *
   * @param wallet the singleton wallet used to store PoP tokens
   * @param walletDao interface to query the database
   */
  public static Disposable saveWalletRoutine(Wallet wallet, WalletDao walletDao)
      throws GeneralSecurityException {
    String[] seed = wallet.exportSeed();

    WalletEntity walletEntity =
        // Constant id as we need to store only 1 entry (next insert must replace)
        new WalletEntity(0, Collections.unmodifiableList(Arrays.asList(seed)));

    // Save in the database the state
    return walletDao
        .insert(walletEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            () -> Timber.tag(TAG).d("Persisted wallet seed: %s", Arrays.toString(seed)),
            err -> Timber.tag(TAG).e(err, "Error persisting the wallet"));
  }

  /**
   * This function performs a saving routing of the connection information of a given lao. Each lao
   * saves its own set of subscriptions, such that it's possible to restore connections also with
   * old laos.
   *
   * @param laoId identifier of the lao to persist
   * @param networkManager network manager containing the message sender with the url and
   *     subscriptions of the lao
   * @param subscriptionsDao interface to query the subscriptions table
   */
  public static Disposable saveSubscriptionsRoutine(
      String laoId, GlobalNetworkManager networkManager, SubscriptionsDao subscriptionsDao) {
    String currentServerAddress = networkManager.getCurrentUrl();

    if (currentServerAddress == null) {
      return null;
    }

    Set<Channel> subscriptions = networkManager.getMessageSender().getSubscriptions();

    SubscriptionsEntity subscriptionsEntity =
        new SubscriptionsEntity(laoId, currentServerAddress, subscriptions);

    // Save in the db the connections
    return subscriptionsDao
        .insert(subscriptionsEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            () -> Timber.tag(TAG).d("Persisted connections for lao %s : %s", laoId, subscriptions),
            err -> Timber.tag(TAG).e(err, "Error persisting the connections for lao %s", laoId));
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
   * This function returns a callback to be registered to the application lifecycle.
   *
   * @param consumers map that has as key the method to override, as value the consumer to apply
   * @return the lifecycle callback
   */
  public static Application.ActivityLifecycleCallbacks buildLifecycleCallback(
      Map<Lifecycle.Event, Consumer<Activity>> consumers) {
    return new Application.ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(
          @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_CREATE);
        if (consumer != null) {
          consumer.accept(activity);
        }
      }

      @Override
      public void onActivityStarted(@NonNull Activity activity) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_START);
        if (consumer != null) {
          consumer.accept(activity);
        }
      }

      @Override
      public void onActivityResumed(@NonNull Activity activity) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_RESUME);
        if (consumer != null) {
          consumer.accept(activity);
        }
      }

      @Override
      public void onActivityPaused(@NonNull Activity activity) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_PAUSE);
        if (consumer != null) {
          consumer.accept(activity);
        }
      }

      @Override
      public void onActivityStopped(@NonNull Activity activity) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_STOP);
        if (consumer != null) {
          consumer.accept(activity);
        }
      }

      @Override
      public void onActivitySaveInstanceState(
          @NonNull Activity activity, @NonNull Bundle outState) {
        // Do nothing here
      }

      @Override
      public void onActivityDestroyed(@NonNull Activity activity) {
        Consumer<Activity> consumer = consumers.get(Lifecycle.Event.ON_DESTROY);
        if (consumer != null) {
          consumer.accept(activity);
        }
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

  /** Callback function for the card listener to expand and shrink a text box */
  public static void handleExpandArrow(ImageView arrow, TextView text) {
    float newRotation;
    int visibility;
    // If the arrow is pointing up, then rotate down and make visible the text
    if (arrow.getRotation() == ORIENTATION_UP) {
      newRotation = ORIENTATION_DOWN;
      visibility = View.VISIBLE;
    } else { // Otherwise rotate up and hide the text
      newRotation = ORIENTATION_UP;
      visibility = View.GONE;
    }

    // Use an animation to rotate smoothly
    arrow.animate().rotation(newRotation).setDuration(300).start();
    text.setVisibility(visibility);
  }
}
