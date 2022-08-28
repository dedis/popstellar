package com.github.dedis.popstellar.testutils.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class allows easy testing of fragments. It is greatly inspired by {@link
 * androidx.fragment.app.testing.FragmentScenario} and
 * https://homanhuang.medium.com/to-make-fragment-test-under-hilt-installed-65ff2d5e5eb6#1736
 *
 * <p>It allows Hilt injection and custom activity holding the fragment
 *
 * @param <A> Activity class
 * @param <F> Fragment class
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class FragmentScenario<A extends AppCompatActivity, F extends Fragment> {

  /**
   * Launch a new FragmentScenario
   *
   * @param fragmentClass fragment to launch the scenario on
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <F extends Fragment> FragmentScenario<EmptyHiltActivity, F> launch(
      @NonNull Class<F> fragmentClass) {
    return launchIn(
        EmptyHiltActivity.class,
        Bundle.EMPTY,
        android.R.id.content,
        fragmentClass,
        (FragmentFactory) null,
        Bundle.EMPTY);
  }

  /**
   * Launch a new FragmentScenario
   *
   * @param fragmentClass fragment to launch the scenario on
   * @param fragmentSupplier supplier that creates the fragment object
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <F extends Fragment> FragmentScenario<EmptyHiltActivity, F> launch(
      @NonNull Class<F> fragmentClass, @NonNull Supplier<F> fragmentSupplier) {
    return launchIn(
        EmptyHiltActivity.class,
        Bundle.EMPTY,
        android.R.id.content,
        fragmentClass,
        factory(fragmentSupplier),
        Bundle.EMPTY);
  }

  /**
   * Launch a new FragmentScenario
   *
   * @param activityClass activity to launch the scenario on
   * @param contentId id of the placeholder where the fragment will be put
   * @param fragmentClass fragment to launch the scenario on
   * @param <A> Activity type
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <A extends AppCompatActivity, F extends Fragment> FragmentScenario<A, F> launchIn(
      Class<A> activityClass, @IdRes int contentId, Class<F> fragmentClass) {
    return launchIn(
        activityClass,
        Bundle.EMPTY,
        contentId,
        fragmentClass,
        (FragmentFactory) null,
        Bundle.EMPTY);
  }

  /**
   * Launch a new FragmentScenario
   *
   * @param activityClass activity to launch the scenario on
   * @param contentId id of the placeholder where the fragment will be put
   * @param fragmentClass fragment to launch the scenario on
   * @param fragmentSupplier supplier that creates the fragment object
   * @param <A> Activity type
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <A extends AppCompatActivity, F extends Fragment> FragmentScenario<A, F> launchIn(
      Class<A> activityClass,
      @IdRes int contentId,
      Class<F> fragmentClass,
      Supplier<F> fragmentSupplier) {
    return launchIn(
        activityClass,
        Bundle.EMPTY,
        contentId,
        fragmentClass,
        factory(fragmentSupplier),
        Bundle.EMPTY);
  }

  /**
   * Launch a new FragmentScenario
   *
   * @param activityClass activity to launch the scenario on
   * @param activityArgs arguments of the activity
   * @param contentId id of the placeholder where the fragment will be put
   * @param fragmentClass fragment to launch the scenario on
   * @param fragmentSupplier supplier that creates the fragment object
   * @param <A> Activity type
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <A extends AppCompatActivity, F extends Fragment> FragmentScenario<A, F> launchIn(
      Class<A> activityClass,
      Bundle activityArgs,
      @IdRes int contentId,
      Class<F> fragmentClass,
      Supplier<F> fragmentSupplier) {
    return launchIn(
        activityClass,
        activityArgs,
        contentId,
        fragmentClass,
        factory(fragmentSupplier),
        Bundle.EMPTY);
  }

  /**
   * Launch a new FragmentScenario
   *
   * @param activityClass activity to launch the scenario on
   * @param activityArgs arguments of the activity
   * @param contentId id of the placeholder where the fragment will be put
   * @param fragmentClass fragment to launch the scenario on
   * @param fragmentSupplier supplier that creates the fragment object
   * @param fragmentArgs arguments of the fragment
   * @param <A> Activity type
   * @param <F> Fragment type
   * @return the launched scenario
   */
  public static <A extends AppCompatActivity, F extends Fragment> FragmentScenario<A, F> launchIn(
      Class<A> activityClass,
      Bundle activityArgs,
      @IdRes int contentId,
      Class<F> fragmentClass,
      Supplier<F> fragmentSupplier,
      Bundle fragmentArgs) {
    return launchIn(
        activityClass,
        activityArgs,
        contentId,
        fragmentClass,
        factory(fragmentSupplier),
        fragmentArgs);
  }

  private static <F extends Fragment> FragmentFactory factory(Supplier<F> fragmentSupplier) {
    return new SimpleFragmentFactory(fragmentSupplier);
  }

  public static final String TAG = "FRAGMENT";

  /**
   * Launch a new FragmentScenario with following arguments :
   *
   * @param activityClass activity to launch the fragment on
   * @param activityArgs arguments of the activity
   * @param contentId id of the placeholder where the fragment will be put
   * @param fragmentClass fragment to launch
   * @param factory that produces the fragment object. If null, the android default will be used.
   * @param fragmentArgs arguments of the fragment
   * @param <A> Activity type
   * @param <F> Fragment type
   * @return the launched FragmentScenario
   */
  public static <A extends AppCompatActivity, F extends Fragment> FragmentScenario<A, F> launchIn(
      @NonNull Class<A> activityClass,
      @Nullable Bundle activityArgs,
      @IdRes int contentId,
      @NonNull Class<F> fragmentClass,
      @Nullable FragmentFactory factory,
      @Nullable Bundle fragmentArgs) {

    Intent mainActivityIntent =
        Intent.makeMainActivity(
            new ComponentName(ApplicationProvider.getApplicationContext(), activityClass));
    mainActivityIntent.putExtras(activityArgs);

    ActivityScenario<A> scenario = ActivityScenario.launch(mainActivityIntent);
    ActivityScenario.ActivityAction<A> action =
        activity -> {
          if (factory != null) {
            activity.getSupportFragmentManager().setFragmentFactory(factory);
          }

          Fragment fragment =
              activity
                  .getSupportFragmentManager()
                  .getFragmentFactory()
                  .instantiate(
                      Objects.requireNonNull(fragmentClass.getClassLoader()),
                      fragmentClass.getName());
          fragment.setArguments(fragmentArgs);

          activity
              .getSupportFragmentManager()
              .beginTransaction()
              .replace(contentId, fragment, TAG)
              .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
              .commitNow();
        };

    FragmentScenario<A, F> fragmentScenario =
        new FragmentScenario<>(scenario, fragmentClass, action);

    scenario.onActivity(action);

    return fragmentScenario;
  }

  private final ActivityScenario<A> activityScenario;
  private final Class<F> fragmentClass;
  private final ActivityScenario.ActivityAction<A> startFragment;

  private FragmentScenario(
      ActivityScenario<A> scenario,
      Class<F> clazz,
      ActivityScenario.ActivityAction<A> startFragmentAction) {
    activityScenario = scenario;
    fragmentClass = clazz;
    startFragment = startFragmentAction;
  }

  /**
   * Recreate the scenario
   *
   * <p>This function will move the activity to the destroyed state and recreate it. It will the
   * recreate the fragment from the scenario and place it in the activity.
   *
   * @return the scenario
   */
  public FragmentScenario<A, F> recreate() {
    activityScenario.recreate();
    activityScenario.onActivity(startFragment);
    return this;
  }

  /**
   * Execute on action on the activity the scenario is running on
   *
   * @param action to execute on the activity
   * @return the scenario
   */
  public FragmentScenario<A, F> onActivity(ActivityScenario.ActivityAction<A> action) {
    activityScenario.onActivity(action);
    return this;
  }

  /**
   * Execute an action on the fragment the scenario is running on
   *
   * @param action to execute on the fragment
   * @return the scenario
   */
  public FragmentScenario<A, F> onFragment(Consumer<F> action) {
    activityScenario.onActivity(
        activity ->
            action.accept(
                Objects.requireNonNull(
                    fragmentClass.cast(
                        activity.getSupportFragmentManager().findFragmentByTag(TAG)))));
    return this;
  }

  /**
   * Advance the fragment to a new state
   *
   * @param newState to move onto
   * @return the scenario
   */
  public FragmentScenario<A, F> moveToState(Lifecycle.State newState) {
    if (newState == Lifecycle.State.DESTROYED) {
      activityScenario.onActivity(
          activity -> {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
            if (fragment != null) {
              activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
            }
          });
    } else {
      activityScenario.onActivity(
          activity -> {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
            if (fragment == null) throw new IllegalStateException("fragment is already destroyed");

            activity
                .getSupportFragmentManager()
                .beginTransaction()
                .setMaxLifecycle(fragment, newState)
                .commitNow();
          });
    }
    return this;
  }

  /** Close the scenario. This should be called at the end of any test. */
  public void close() {
    activityScenario.close();
  }

  private static class SimpleFragmentFactory extends FragmentFactory {

    private final Supplier<? extends Fragment> supplier;

    public SimpleFragmentFactory(Supplier<? extends Fragment> fragmentSupplier) {
      this.supplier = fragmentSupplier;
    }

    @NonNull
    @Override
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
      return supplier.get();
    }
  }
}
