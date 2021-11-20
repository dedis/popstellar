package com.github.dedis.popstellar.testutils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;

import org.junit.rules.ExternalResource;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A test rule that creates a Fragment for each test
 *
 * @param <F> the fragment type
 */
public class FragmentScenarioRule<F extends Fragment> extends ExternalResource {

  private final Supplier<FragmentScenario<F>> scenarioSupplier;
  @Nullable private FragmentScenario<F> scenario;

  public FragmentScenarioRule(Supplier<FragmentScenario<F>> scenarioSupplier) {
    this.scenarioSupplier = scenarioSupplier;
  }

  public static <F extends Fragment> FragmentScenarioRule<F> launchInContainer(
      Class<F> fragmentClass) {
    return new FragmentScenarioRule<>(() -> FragmentScenario.launchInContainer(fragmentClass));
  }

  public static <F extends Fragment> FragmentScenarioRule<F> launchInContainer(
      Class<F> fragmentClass, Supplier<? extends Fragment> supplier) {
    return new FragmentScenarioRule<>(
        () ->
            FragmentScenario.launchInContainer(
                fragmentClass, null, new SimpleFragmentFactory(supplier)));
  }

  public static <F extends Fragment> FragmentScenarioRule<F> launch(Class<F> fragmentClass) {
    return new FragmentScenarioRule<>(() -> FragmentScenario.launch(fragmentClass));
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
  public FragmentScenario<F> getScenario() {
    return Objects.requireNonNull(scenario);
  }

  private static final class SimpleFragmentFactory extends FragmentFactory {

    private final Supplier<? extends Fragment> supplier;

    private SimpleFragmentFactory(Supplier<? extends Fragment> supplier) {
      this.supplier = supplier;
    }

    @NonNull
    @Override
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
      return supplier.get();
    }
  }
}
