package com.github.dedis.popstellar.ui.detail;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoViewModelTest {

  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();

  @Inject GlobalNetworkManager networkManager;
  @Inject Gson gson;
  @Inject KeyManager keyManager;


  @BindValue @Mock LAORepository laoRepository;

  // Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);
  // Setup rule, used to setup things before the activity is started
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltAndroidRule.inject();

          when(laoRepository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));
        }
      };

  @Before
  public setUp(){

  }

}
