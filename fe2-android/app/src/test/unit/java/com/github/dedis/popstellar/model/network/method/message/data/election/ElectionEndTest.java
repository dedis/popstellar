package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;
import java.time.Instant;
import org.junit.Test;

public class ElectionEndTest {

  private final PublicKey organizer = Base64DataUtils.generatePublicKey();
  private final long creation = Instant.now().getEpochSecond();
  private final String laoId = Lao.generateLaoId(organizer, creation, "name");
  private final String electionId =
      Election.generateElectionSetupId(laoId, creation, "election name");

  private final String registeredVotes = Base64DataUtils.generateRandomBase64String();
  private final ElectionEnd electionEnd = new ElectionEnd(electionId, laoId, registeredVotes);

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsElectionIdNotBase64Test() {
    new ElectionEnd("not base 64", laoId, registeredVotes);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsLaoIdNotBase64Test() {
    new ElectionEnd(electionId, "not base 64", registeredVotes);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsRegisteredVotesNotBase64Test() {
    new ElectionEnd(electionId, laoId, "not base 64");
  }

  @Test
  public void electionEndGetterReturnsCorrectElectionId() {
    assertThat(electionEnd.getElectionId(), is(electionId));
  }

  @Test
  public void electionEndGetterReturnsCorrectLaoId() {
    assertThat(electionEnd.getLaoId(), is(laoId));
  }

  @Test
  public void electionEndGetterReturnsCorrectRegisteredVotes() {
    assertThat(electionEnd.getRegisteredVotes(), is(registeredVotes));
  }

  @Test
  public void electionEndGetterReturnsCorrectObject() {
    assertThat(electionEnd.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void electionEndGetterReturnsCorrectAction() {
    assertThat(electionEnd.getAction(), is(Action.END.getAction()));
  }

  @Test
  public void fieldsCantBeNull() {
    assertThrows(
        IllegalArgumentException.class, () -> new ElectionEnd(null, laoId, registeredVotes));
    assertThrows(
        IllegalArgumentException.class, () -> new ElectionEnd(electionId, null, registeredVotes));
    assertThrows(IllegalArgumentException.class, () -> new ElectionEnd(electionId, laoId, null));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(electionEnd);

    String pathDir = "protocol/examples/messageData/election_end/";

    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "bad_election_end_created_at_before_end.json");

    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
  }
}
