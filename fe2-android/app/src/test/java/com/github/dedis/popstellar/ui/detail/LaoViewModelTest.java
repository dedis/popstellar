package com.github.dedis.popstellar.ui.detail;


import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.material.appbar.AppBarLayout.Behavior;
import com.google.gson.Gson;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoViewModelTest {

  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final Election ELECTION = new Election(LAO_ID,10223426, "Debug");

  @Inject GlobalNetworkManager networkManager;
  @Inject Gson gson;
  @Inject KeyManager keyManager;
  @Inject Wallet wallet;

  @Mock Observer<SingleEvent<Boolean>> observer;
  @Mock LAORepository laoRepository;
  @Mock Channel channel;

  LaoDetailViewModel laoViewModel;

  // Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);
  // Setup rule, used to setup things before the activity is started
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltAndroidRule.inject();
          laoViewModel = new LaoDetailViewModel(ApplicationProvider.getApplicationContext(),
              laoRepository, networkManager, keyManager, gson, wallet);;
        }
      };

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltAndroidRule)
          .around(setupRule);

  @Test
  public void openElectionTest(){
    laoViewModel.getOpenElectionEvent().observeForever(observer);
    laoViewModel.openElection(ELECTION);
    ELECTION.setChannel(channel);
    laoViewModel.getOpenElectionEvent().removeObserver(observer);
  }


}
