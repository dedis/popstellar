package com.github.dedis.popstellar.ui.detail.event.consensus;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.pages.qrcode.ElectionStartPageObject.electionStartButton;
import static com.github.dedis.popstellar.pages.qrcode.ElectionStartPageObject.electionStatus;
import static com.github.dedis.popstellar.pages.qrcode.ElectionStartPageObject.electionTitle;
import static com.github.dedis.popstellar.pages.qrcode.ElectionStartPageObject.nodesGrid;
import static org.hamcrest.core.AllOf.allOf;

import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ElectionStartFragmentTest {

  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").withZone(ZoneId.systemDefault());
  private static final String laoId = "laoId";
  private static final String laoChannel = "/root/" + laoId;
  private static final String electionName = "My Election !";
  private static final long pastTime = 946684800;
  private static final long futureTime = 2145916800;

  private static final Election election = new Election(laoId, pastTime, electionName);
  private static final Lao lao = new Lao(laoId);
  private static final ConsensusNode node2 = new ConsensusNode("node2Key");
  private static final ConsensusNode node3 = new ConsensusNode("node3Key");
  private static final ConsensusKey key = new ConsensusKey("election", election.getId(), "state");
  private static final Consensus consensus2 = new Consensus(pastTime, key, null);
  private static final Consensus consensus3 = new Consensus(pastTime, key, null);

  private static String publicKey;
  private static ConsensusNode ownNode;
  private static LAORepository laoRepository;
  private static LaoDetailViewModel laoDetailViewModel;

  @Rule
  public final FragmentScenarioRule<ElectionStartFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(
          ElectionStartFragment.class, ElectionStartFragment::newInstance);

  @Test
  public void test() throws InterruptedException {
    fragmentRule
        .getScenario()
        .onFragment(
            electionStartFragment -> {
              FragmentActivity fragmentActivity = electionStartFragment.requireActivity();
              laoDetailViewModel = LaoDetailActivity.obtainViewModel(fragmentActivity);
              laoRepository = LAORepository.getInstance(null, null, null, null, null);

              election.setStart(futureTime);
              publicKey = laoDetailViewModel.getPublicKey();
              ownNode = new ConsensusNode(publicKey);

              lao.setChannel(laoChannel);
              lao.getWitnesses()
                  .addAll(Arrays.asList(publicKey, node2.getPublicKey(), node3.getPublicKey()));
              lao.getNodes().addAll(Arrays.asList(ownNode, node2, node3));

              laoRepository.getLaoById().put(laoChannel, new LAOState(lao));
              laoRepository.updateNodes(laoChannel);

              laoDetailViewModel.setCurrentElection(election);
              laoDetailViewModel.setCurrentLao(lao);
            });

    // TODO find how to access the viewModel/repository before the fragment is created
    fragmentRule.getScenario().recreate();

    String expectedTitle = "Election " + '"' + electionName + '"';
    String expectedStatusBefore = "Waiting scheduled time";
    String expectedStatusAfter = "Ready to start";
    String futureDate = dateTimeFormatter.format(Instant.ofEpochSecond(futureTime));
    String expectedStartBefore = "Election scheduled to start at\n" + futureDate;
    String expectedStartAfter = "Start Election";

    // Election start time has not passed yet, should display that it's waiting
    electionTitle().check(matches(withText(expectedTitle))).check(matches(isDisplayed()));
    electionStatus().check(matches(withText(expectedStatusBefore))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartBefore)))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()))
        .check(matches(isNotEnabled()));

    // Wait for the timer update
    election.setStart(pastTime);
    TimeUnit.SECONDS.sleep(2);

    // Election start time has passed, should display that it's ready and start button enabled
    electionTitle().check(matches(withText(expectedTitle))).check(matches(isDisplayed()));
    electionStatus().check(matches(withText(expectedStatusAfter))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartAfter)))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()))
        .check(matches(isEnabled()));

    // Order of nodes are not guaranteed in general, but in this this it's ownNode(0), node2, node3
    DataInteraction grid = nodesGrid();
    nodeAssertions(grid, 0, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, 1, "Waiting\nnode2Key", false);
    nodeAssertions(grid, 2, "Waiting\nnode3Key", false);

    // Nodes 2 and 3 try to start, we accept node 3 (it should disable button for node3)
    consensus2.setChannel(laoChannel + "/consensus");
    consensus2.setMessageId("m2");
    consensus3.setChannel(laoChannel + "/consensus");
    consensus3.setMessageId("m3");
    node2.addConsensus(consensus2);
    node3.addConsensus(consensus3);
    ownNode.addMessageIdOfAnAcceptedConsensus("m3");
    laoRepository.updateNodes(laoChannel);

    nodeAssertions(grid, 0, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, 1, "Approve Start by\nnode2Key", true);
    nodeAssertions(grid, 2, "Approve Start by\nnode3Key", false);

    // TODO find how to intercept message
    grid.atPosition(1).perform(ViewActions.click());
  }

  private void nodeAssertions(
      DataInteraction grid, int position, String expectedText, boolean enabled) {
    grid.atPosition(position)
        .check(
            matches(
                allOf(
                    isDisplayed(),
                    isClickable(),
                    withText(expectedText),
                    enabled ? isEnabled() : isNotEnabled())));
  }
}
