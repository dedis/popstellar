package com.github.dedis.popstellar.testutils.pages.detail.event.consensus;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.ConsensusNode;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class ElectionStartPageObject {

  private ElectionStartPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction electionTitle() {
    return onView(withId(R.id.election_title));
  }

  public static ViewInteraction electionStatus() {
    return onView(withId(R.id.election_status));
  }

  public static ViewInteraction electionStartButton() {
    return onView(withId(R.id.election_start));
  }

  public static DataInteraction nodesGrid() {
    return onData(allOf(is(instanceOf(ConsensusNode.class))))
        .inAdapterView(withId(R.id.nodes_grid));
  }
}
