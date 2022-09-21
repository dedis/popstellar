package com.github.dedis.popstellar.ui.detail.witness;

import android.Manifest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.detail.witness.WitnessFragmentPageObject.addWitnessButton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for the Witnesses fragment
 *
 * <p>The test suite handles the cases where the camera permission is granted at startup
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WitnessingFragmentWithPermissionTest {

  private static final String LAO_NAME = "LAO";
  private static final PublicKey ORGANIZER = Base64DataUtils.generatePublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, ORGANIZER, 10223421);
  private static final String LAO_ID = LAO.getId();

  @BindValue @Mock LAORepository repository;

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          when(repository.getLaoObservable(any()))
              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));
        }
      };

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<LaoDetailActivity, WitnessesFragment> scenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoDetailActivity.class,
          new BundleBuilder()
              .putString(laoIdExtra(), LAO_ID)
              .putString(fragmentToOpenExtra(), laoDetailValue())
              .build(),
          containerId(),
          WitnessesFragment.class,
          WitnessesFragment::newInstance);

  @Rule(order = 4)
  public final GrantPermissionRule rule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

  @Test
  public void addWitnessOpenScanningNoPermission() {
    addWitnessButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(qrCodeFragmentId()))));
  }
}
