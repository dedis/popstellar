// package com.github.dedis.popstellar.ui.detail;
//
// import androidx.test.espresso.contrib.DrawerActions;
// import androidx.test.espresso.contrib.NavigationViewActions;
// import androidx.test.espresso.intent.Intents;
// import androidx.test.ext.junit.rules.ActivityScenarioRule;
// import androidx.test.ext.junit.runners.AndroidJUnit4;
//
// import com.github.dedis.popstellar.model.objects.Lao;
// import com.github.dedis.popstellar.model.objects.Wallet;
// import com.github.dedis.popstellar.model.objects.view.LaoView;
// import com.github.dedis.popstellar.repository.LAORepository;
// import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
// import com.github.dedis.popstellar.testutils.*;
// import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
// import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
// import com.github.dedis.popstellar.utility.error.UnknownLaoException;
// import com.google.gson.Gson;
//
// import org.junit.Rule;
// import org.junit.Test;
// import org.junit.rules.ExternalResource;
// import org.junit.runner.RunWith;
// import org.mockito.Mock;
// import org.mockito.junit.MockitoJUnit;
// import org.mockito.junit.MockitoTestRule;
//
// import java.security.GeneralSecurityException;
//
// import javax.inject.Inject;
//
// import dagger.hilt.android.testing.*;
// import io.reactivex.subjects.BehaviorSubject;
//
// import static androidx.test.espresso.action.ViewActions.click;
// import static androidx.test.espresso.assertion.ViewAssertions.matches;
// import static androidx.test.espresso.intent.Intents.intended;
// import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
// import static androidx.test.espresso.matcher.ViewMatchers.withChild;
// import static androidx.test.espresso.matcher.ViewMatchers.withId;
// import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;
//
// @HiltAndroidTest
// @RunWith(AndroidJUnit4.class)
// public class LaoDetailActivityTest {
//
//  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
//  private static final String LAO_ID = LAO.getId();
//
//  @Inject GlobalNetworkManager networkManager;
//  @Inject Gson gson;
//
//  @BindValue @Mock LAORepository laoRepository;
//  @BindValue @Mock Wallet wallet;
//
//  @Rule(order = 0)
//  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);
//
//  @Rule(order = 1)
//  public final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);
//
//  @Rule(order = 2)
//  public final ExternalResource setupRule =
//      new ExternalResource() {
//        @Override
//        protected void before() throws UnknownLaoException, GeneralSecurityException {
//          hiltAndroidRule.inject();
//          when(laoRepository.getLao(any())).thenAnswer(invocation -> new LaoView(LAO));
//
//          when(laoRepository.getLaoObservable(anyString()))
//              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));
//
//          when(wallet.exportSeed())
//              .thenReturn(
//                  new String[] {
//                    "jar",
//                    "together",
//                    "minor",
//                    "alley",
//                    "glow",
//                    "hybrid",
//                    "village",
//                    "creek",
//                    "meadow",
//                    "atom",
//                    "travel",
//                    "bracket"
//                  });
//        }
//      };
//
//  @Rule(order = 3)
//  public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule =
//      new ActivityScenarioRule<>(
//          IntentUtils.createIntent(
//              LaoDetailActivity.class,
//              new BundleBuilder()
//                  .putString(laoIdExtra(), LAO_ID)
//                  .putString(fragmentToOpenExtra(), laoDetailValue())
//                  .build()));
//
//
//  @Test
//  public void witnessingTabShowsWitnessTab() {
//    witnessButton().perform(click());
//    fragmentContainer().check(matches(withChild(withId(witnessingFragmentId()))));
//  }
//
//  @Test
//  public void socialMediaNavOpensSocialMediaActivity() {
//    Intents.init();
//    socialMediaButton().perform(click());
//    intended(hasComponent(SocialMediaActivity.class.getName()));
//    Intents.release();
//  }
//
//  @Test
//  public void digitalCashNavOpensActivity() {
//    drawerLayout().perform(DrawerActions.open());
//    Intents.init();
//    navigationDrawer().perform(NavigationViewActions.navigateTo(digitalCashMenu()));
//    intended(hasComponent(DigitalCashActivity.class.getName()));
//    Intents.release();
//  }
//
//  private void openDrawer(){
//  }
// }
