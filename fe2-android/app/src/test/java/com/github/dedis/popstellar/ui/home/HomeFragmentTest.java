package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static com.github.dedis.popstellar.ui.pages.home.HomeFragmentPageObject.laoList;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentContainerId;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Collections;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class HomeFragmentTest {

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltRule.inject();

          when(repository.getAllLaos())
              .thenReturn(BehaviorSubject.createDefault(Collections.emptyList()));
        }
      };

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<HomeActivity, HomeFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          homeFragmentContainerId(),
          HomeFragment.class,
          HomeFragment::newInstance);

  @Mock @BindValue LAORepository repository;

  @Test
  public void laosListIsDisplayed() {
    laoList().check(matches(hasChildCount(0)));
  }
}
