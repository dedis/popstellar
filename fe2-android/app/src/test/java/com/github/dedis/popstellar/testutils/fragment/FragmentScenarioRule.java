package com.github.dedis.popstellar.testutils.fragment;

import androidx.fragment.app.Fragment;

import java.util.function.Supplier;

/**
 * An {@link ActivityFragmentScenarioRule} where the activity doesn't matter
 *
 * @param <F> Fragment type
 */
public class FragmentScenarioRule<F extends Fragment>
    extends ActivityFragmentScenarioRule<EmptyHiltActivity, F> {

  public FragmentScenarioRule(
      Supplier<FragmentScenario<EmptyHiltActivity, F>> fragmentScenarioSupplier) {
    super(fragmentScenarioSupplier);
  }

  public static <F extends Fragment> FragmentScenarioRule<F> launch(Class<F> fragmentClass) {
    return new FragmentScenarioRule<>(() -> FragmentScenario.launch(fragmentClass));
  }

  public static <F extends Fragment> FragmentScenarioRule<F> launch(
      Class<F> fragmentClass, Supplier<F> supplier) {
    return new FragmentScenarioRule<>(() -> FragmentScenario.launch(fragmentClass, supplier));
  }
}
