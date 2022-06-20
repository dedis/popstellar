package com.github.dedis.popstellar.ui.digitalcash;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.fragmentToOpenExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoDetailValue;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.digitalCashFragmentId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.historyButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.homeButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.issueButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.receiveButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.sendButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.HistoryPageObject.fragmentDigitalCashHistoryId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.IssuePageObject.fragmentDigitalCashIssueId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceiptPageObject.fragmentDigitalCashReceiptId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceivePageObject.fragmentDigitalCashReceiveId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.fragmentDigitalCashSendId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.sendButtonToReceipt;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.IntentUtils;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;


@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class DigitalCashActivityTest {

    private static final String LAO_NAME = "LAO";
    private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
    private static final PublicKey PK = KEY_PAIR.getPublicKey();
    private static final Lao LAO = new Lao(LAO_NAME, PK, 10223421);
    private static final String LAO_ID = LAO.getId();
    private static final String RC_TITLE = "Roll-Call Title";

    @Inject
    Gson gson;

    @BindValue @Mock
    GlobalNetworkManager networkManager;
    @BindValue @Mock LAORepository repository;
    @BindValue @Mock
    MessageSender messageSender;
    @BindValue @Mock
    KeyManager keyManager;

    @BindValue @Mock
    DigitalCashViewModel model;

    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

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
                    when(repository.getLaoObservable(anyString()))
                            .thenReturn(BehaviorSubject.createDefault(LAO));
                    when(repository.getAllLaos())
                            .thenReturn(BehaviorSubject.createDefault(Collections.singletonList(LAO)));
                    when(model.getCurrentLao())
                            .thenReturn(LAO);
                    when(LAO.getOrganizer()).thenReturn(PK);
/*
                    when(keyManager.getMainPublicKey()).thenReturn(PK);
                    when(keyManager.getMainKeyPair()).thenReturn(KEY_PAIR);
                    when(networkManager.getMessageSender()).thenReturn(messageSender);
                    when(messageSender.subscribe(any())).then(args -> Completable.complete());
                    when(messageSender.publish(any(), any())).then(args -> Completable.complete());
                    when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());

 */
                }
            };

    @Rule(order = 3)
    public ActivityScenarioRule<DigitalCashActivity> activityScenarioRule =
            new ActivityScenarioRule<>(
                    IntentUtils.createIntent(
                            DigitalCashActivity.class,
                            new BundleBuilder()
                                    .putString(laoIdExtra(), LAO_ID)
                                    .putString(fragmentToOpenExtra(), laoDetailValue())
                                    .build()));

    @Test
    public void homeButtonStaysHome() {
        homeButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(digitalCashFragmentId()))));
    }

    @Test
    public void sendButtonGoesToSendThenToReceipt() {
        sendButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashSendId()))));
        sendButtonToReceipt().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiptId()))));
    }

    @Test
    public void historyButtonGoesToHistory() {
        historyButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashHistoryId()))));
    }

    @Test
    public void issueButtonGoesToIssue() {
        issueButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashIssueId()))));
    }

    @Test
    public void receiveButtonGoesToReceive() {
        receiveButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiveId()))));
    }
}