package com.github.dedis.popstellar.testutils.fragment;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import org.junit.rules.ExternalResource;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A test rule that creates a Fragment for each test
 *
 * @param <F> the fragment type
 */
public class ActivityFragmentScenarioRule<A extends AppCompatActivity, F extends Fragment>
    extends ExternalResource {

  private final Supplier<FragmentScenario<A, F>> scenarioSupplier;
  @Nullable private FragmentScenario<A, F> scenario;

  public ActivityFragmentScenarioRule(Supplier<FragmentScenario<A, F>> scenarioSupplier) {
    this.scenarioSupplier = scenarioSupplier;
  }

  public static <A extends AppCompatActivity, F extends Fragment>
      ActivityFragmentScenarioRule<A, F> launchIn(
          Class<A> activityClass,
          @IdRes int contentId,
          Class<F> fragmentClass,
          Supplier<F> supplier) {
    return new ActivityFragmentScenarioRule<>(
        () -> FragmentScenario.launchIn(activityClass, contentId, fragmentClass, supplier));
  }

  public static <A extends AppCompatActivity, F extends Fragment>
      ActivityFragmentScenarioRule<A, F> launchIn(
          Class<A> activityClass,
          Bundle activityArgs,
          @IdRes int contentId,
          Class<F> fragmentClass,
          Supplier<F> supplier) {
    return new ActivityFragmentScenarioRule<>(
        () ->
            FragmentScenario.launchIn(
                activityClass, activityArgs, contentId, fragmentClass, supplier));
  }

  public static <A extends AppCompatActivity, F extends Fragment>
      ActivityFragmentScenarioRule<A, F> launchIn(
          Class<A> activityClass,
          Bundle activityArgs,
          @IdRes int contentId,
          Class<F> fragmentClass,
          Supplier<F> supplier,
          Bundle fragmentArgs) {
    return new ActivityFragmentScenarioRule<>(
        () ->
            FragmentScenario.launchIn(
                activityClass, activityArgs, contentId, fragmentClass, supplier, fragmentArgs));
  }

  @Override
  protected void before() throws Throwable {
    scenario = scenarioSupplier.get();
  }

  @Override
  protected void after() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @NonNull
  public FragmentScenario<A, F> getScenario() {
    return Objects.requireNonNull(scenario);
  }
}
