package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static com.github.dedis.popstellar.ui.pages.home.HomeFragmentPageObject.laoList;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentId;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class HomeFragmentTest {

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ActivityFragmentScenarioRule<HomeActivity, HomeFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class, homeFragmentId(), HomeFragment.class, HomeFragment::new);

  public void emptyLaosHasEmptyList() {
    laoList().check(matches(hasChildCount(0)));
  }
}
