package com.github.dedis.popstellar.ui.detail.event.consensus;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionStartButton;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionStatus;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionTitle;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.nodesGrid;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.handler.ConsensusHandler;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ElectionStartFragmentTest {

  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").withZone(ZoneId.systemDefault());
  private static final String laoId = "laoId";
  private static final String laoChannel = "/root/" + laoId;
  private static final String consensusChannel = laoChannel + "/consensus";
  private static final String electionName = "My Election !";
  private static final long pastTime = 946684800;
  private static final long futureTime = 2145916800;

  private static final Lao lao = new Lao(laoId);
  private static final Election election = new Election(laoId, pastTime, electionName);
  private static final String node2Key = "12RDUW4s9bZrdqJB7zoVag";
  private static final String node3Key = "dRCXFdXYfY5OVAzh3oQ3pA";
  private static final ConsensusKey key = new ConsensusKey("election", election.getId(), "state");
  private static final String instanceId =
      Consensus.generateConsensusId(key.getType(), key.getId(), key.getProperty());

  private static final ConsensusElect elect =
      new ConsensusElect(pastTime, key.getId(), key.getType(), key.getProperty(), "started");
  private static final ConsensusElectAccept accept3 =
      new ConsensusElectAccept(instanceId, "m3", true);

  private static final ConsensusLearn learn3 =
      new ConsensusLearn(instanceId, "m3", Collections.emptyList());

  private static final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

  private static String publicKey;
  private static LAORepository laoRepository;
  private static boolean isAlreadySetup = false;

  LAORemoteDataSource remoteDataSource = Mockito.mock(LAORemoteDataSource.class);
  LAOLocalDataSource localDataSource = Mockito.mock(LAOLocalDataSource.class);

  @Rule
  public final FragmentScenarioRule<ElectionStartFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(
          ElectionStartFragment.class,
          () -> {
            ElectionStartFragment fragment = ElectionStartFragment.newInstance();
            if (!isAlreadySetup) {
              setup();
              isAlreadySetup = true;
            }
            return fragment;
          });

  private void setup() {
    Mockito.when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
    Observable<GenericMessage> upstream = Observable.fromArray((GenericMessage) new Result(42));

    // Mock the remote data source to receive a response
    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);

    try {
      LAORepository.destroyInstance();
      laoRepository =
          LAORepository.getInstance(
              remoteDataSource,
              localDataSource,
              Injection.provideAndroidKeysetManager(ApplicationProvider.getApplicationContext()),
              Injection.provideGson(),
              new ProdSchedulerProvider());
      publicKey = getPublicKey();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    election.setStart(futureTime);
    lao.setChannel(laoChannel);
    lao.setOrganizer(publicKey);
    lao.setWitnesses(Sets.newSet(node2Key, node3Key));

    laoRepository.getLaoById().put(laoChannel, new LAOState(lao));
    laoRepository.updateNodes(laoChannel);
  }

  @After
  public void clean() {
    LAORepository.destroyInstance();
  }

  private String getPublicKey() throws IOException, GeneralSecurityException {
    KeysetHandle publicKeysetHandle =
        Injection.provideAndroidKeysetManager(ApplicationProvider.getApplicationContext())
            .getKeysetHandle()
            .getPublicKeysetHandle();
    return Keys.getEncodedKey(publicKeysetHandle);
  }

  @Test
  public void displayWithUpdatesIsCorrectAndButtonsProduceCorrectMessages() throws InterruptedException {
    fragmentRule
        .getScenario()
        .onFragment(
            electionStartFragment -> {
              FragmentActivity fragmentActivity = electionStartFragment.requireActivity();
              LaoDetailViewModel laoDetailViewModel =
                  LaoDetailActivity.obtainViewModel(fragmentActivity);
              laoDetailViewModel.setCurrentElection(election);
              laoDetailViewModel.setCurrentLao(lao);
            });

    // Recreate the fragment because the viewModel needed to be modified before start
    fragmentRule.getScenario().recreate();

    String expectedTitle = "Election " + '"' + electionName + '"';
    String expectedStatusBefore = "Waiting scheduled time";
    String expectedStatusAfter = "Ready to start";
    String futureDate = dateTimeFormatter.format(Instant.ofEpochSecond(futureTime));
    String pastDate = dateTimeFormatter.format(Instant.ofEpochSecond(pastTime));
    String expectedStartBefore = "Election scheduled to start at\n" + futureDate;
    String expectedStartAfter = "Start Election";
    String expectedStatusStarted = "Started";
    String expectedStartStarted = "Election started successfully at\n" + pastDate;

    // Election start time has not passed yet, should display that it's waiting
    electionTitle().check(matches(withText(expectedTitle))).check(matches(isDisplayed()));
    electionStatus().check(matches(withText(expectedStatusBefore))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartBefore)))
        .check(matches(isDisplayed()))
        .check(matches(isNotEnabled()));

    // Wait for the timer update
    election.setStart(pastTime);
    TimeUnit.SECONDS.sleep(2);

    // Election start time has passed, should display that it's ready and start button enabled
    electionStatus().check(matches(withText(expectedStatusAfter))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartAfter)))
        .check(matches(isDisplayed()))
        .check(matches(isEnabled()));

    // Order of nodes are not guaranteed in general, but in this this it's ownNode(0), node2, node3
    DataInteraction grid = nodesGrid();
    nodeAssertions(grid, 0, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, 1, "Waiting\n" + node2Key, false);
    nodeAssertions(grid, 2, "Waiting\n" + node3Key, false);

    // Nodes 3 try to start
    ConsensusHandler.handleConsensusMessage(laoRepository, consensusChannel, elect, "m3", node3Key);
    laoRepository.updateNodes(laoChannel);

    nodeAssertions(grid, 2, "Approve Start by\n" + node3Key, true);

    // We try to start
    long minCreation = Instant.now().getEpochSecond();
    electionStartButton().perform(ViewActions.click());
    Mockito.verify(remoteDataSource).sendMessage(captor.capture());
    Publish publish = (Publish) captor.getValue();
    MessageGeneral msgGeneral = publish.getMessage();
    long maxCreation = Instant.now().getEpochSecond();
    assertEquals(consensusChannel, publish.getChannel());
    assertEquals(publicKey, msgGeneral.getSender());

    ConsensusElect elect = (ConsensusElect) msgGeneral.getData();
    assertEquals(key, elect.getKey());
    assertEquals(instanceId, elect.getInstanceId());
    assertEquals("started", elect.getValue());
    assertTrue(minCreation <= elect.getCreation() && elect.getCreation() <= maxCreation);

    ConsensusHandler.handleConsensusMessage(
        laoRepository, consensusChannel, elect, "m1", publicKey);
    laoRepository.updateNodes(laoChannel);

    nodeAssertions(grid, 0, "Approve Start by\n" + publicKey, true);

    // We try to accept node3
    grid.atPosition(2).perform(ViewActions.click());
    Mockito.verify(remoteDataSource, Mockito.times(2)).sendMessage(captor.capture());
    publish = (Publish) captor.getValue();
    msgGeneral = publish.getMessage();
    assertEquals(consensusChannel, publish.getChannel());
    assertEquals(publicKey, msgGeneral.getSender());
    ConsensusElectAccept electAccept = (ConsensusElectAccept) msgGeneral.getData();
    assertEquals(new ConsensusElectAccept(instanceId, "m3", true), electAccept);

    // We accepted node 3 (it should disable button for node3)
    ConsensusHandler.handleConsensusMessage(
        laoRepository, consensusChannel, accept3, "a3", publicKey);
    laoRepository.updateNodes(laoChannel);

    nodeAssertions(grid, 2, "Approve Start by\n" + node3Key, false);

    // Receive a learn message => node3 was accepted and has started the election
    ConsensusHandler.handleConsensusMessage(laoRepository, consensusChannel, learn3, "l3", null);
    laoRepository.updateNodes(laoChannel);

    electionStatus().check(matches(withText(expectedStatusStarted))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartStarted)))
        .check(matches(isDisplayed()))
        .check(matches(isNotEnabled()));

    nodeAssertions(grid, 2, "Started by\n" + node3Key, false);
  }

  private void nodeAssertions(
      DataInteraction grid, int position, String expectedText, boolean enabled) {
    grid.atPosition(position)
        .check(
            matches(
                allOf(
                    isDisplayed(),
                    withText(expectedText),
                    enabled ? isEnabled() : isNotEnabled())));
  }
}
