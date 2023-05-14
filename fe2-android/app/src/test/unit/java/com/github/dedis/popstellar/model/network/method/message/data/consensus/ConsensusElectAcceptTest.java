package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConsensusElectAcceptTest {

  private static final MessageID messageId = generateMessageID();
  private static final String instanceId = "bbb";
  private static final ConsensusElectAccept consensusElectAcceptAccept =
      new ConsensusElectAccept(instanceId, messageId, true);
  private static final ConsensusElectAccept consensusElectAcceptReject =
      new ConsensusElectAccept(instanceId, messageId, false);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void getInstanceId() {
    assertEquals(instanceId, consensusElectAcceptAccept.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, consensusElectAcceptAccept.getMessageId());
  }

  @Test
  public void isAcceptTest() {
    assertTrue(consensusElectAcceptAccept.isAccept());
    assertFalse(consensusElectAcceptReject.isAccept());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), consensusElectAcceptAccept.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ELECT_ACCEPT.getAction(), consensusElectAcceptAccept.getAction());
  }

  @Test
  public void equalsTest() {
    assertEquals(consensusElectAcceptAccept, new ConsensusElectAccept(instanceId, messageId, true));
    assertEquals(
        consensusElectAcceptReject, new ConsensusElectAccept(instanceId, messageId, false));

    assertNotEquals(
        consensusElectAcceptAccept, new ConsensusElectAccept("random", messageId, true));
    assertNotEquals(
        consensusElectAcceptAccept,
        new ConsensusElectAccept(
            instanceId, Base64DataUtils.generateMessageIDOtherThan(messageId), true));
    assertNotEquals(
        consensusElectAcceptAccept, new ConsensusElectAccept(instanceId, messageId, false));
    assertNotEquals(
        consensusElectAcceptReject, new ConsensusElectAccept(instanceId, messageId, true));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(consensusElectAcceptAccept);
  }
}
