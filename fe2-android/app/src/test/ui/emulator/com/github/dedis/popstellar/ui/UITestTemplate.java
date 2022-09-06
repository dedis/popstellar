package com.github.dedis.popstellar.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.HashMap;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;

import static org.mockito.Mockito.when;

/**
 * This test class is a template that you can use as a basis to write new UI tests.
 *
 * <p>First of all, these annotations are needed :
 *
 * <ul>
 *   <li>{@link HiltAndroidTest} is needed because we are instantiating Activities and Fragments
 *       that needs Hilt's service injection
 *   <li>{@link RunWith} is needed by the Robolectric tests, but is also work in the emulator tests
 *       so keep it.
 * </ul>
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class UITestTemplate {

  /**
   * This defines a {@link org.junit.rules.TestRule} which encapsulate setup and teardown behaviors
   * of the the suite
   *
   * <p>This particular rule automatically mocks the fields annotated with {@link Mock} at the start
   * of each test using the Mockito library.
   */
  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  /**
   * This rule is necessary to use Hilt service injection.
   *
   * <p>It has also two other feature:
   *
   * <ul>
   *   <li>
   *       <p>It can inject services in the test fields using the {@link Inject} annotation.
   *       <p>An example of the is the Gson service injected in the gson field below.
   *   <li>
   *       <p>It can replace existing injection rule with ones defined by the test using {@link
   *       BindValue}
   * </ul>
   *
   * For more information about the use of Hilt in test, it is advised to read the related <a
   * href="https://developer.android.com/training/dependency-injection/hilt-testing">documentation</a>
   */
  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  /**
   * When creating a UI test, the tested ui element is started. And to do that, it needs some
   * functionality mocked.
   *
   * <p>Therefore, the mocking needs to be done before the ui rule is executed. The solution is this
   * {@link ExternalResource} rule. We can use its before function to create the mocks.
   */
  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          // This line is needed by the hilt rule to inject the fields annotated with @inject
          hiltRule.inject();
          // Place the mock definition here
          when(laoRepo.getLaoById()).thenReturn(new HashMap<>());
        }
      };

  /**
   * The rule responsible of starting the activity. If you want to start a Fragment, take a look at
   * {@link com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule} and {@link
   * com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule}
   */
  @Rule(order = 3)
  public final ActivityScenarioRule<HomeActivity> activityRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  // This field is injected by hilt and will contain the application Gson
  @Inject Gson gson;
  // This field is a mock of an LAORepository that is instantiated by the mockito rule. It i then
  // bound as the LAORepository service for the app.
  @Mock @BindValue LAORepository laoRepo;

  @Test
  public void theTest() {
    // Place your test code here
  }
}
