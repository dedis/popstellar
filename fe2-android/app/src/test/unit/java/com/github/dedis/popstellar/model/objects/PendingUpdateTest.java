package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class PendingUpdateTest {

  private static final MessageID MESSAGE_ID1 = Base64DataUtils.generateMessageID();
  private static final MessageID MESSAGE_ID2 = Base64DataUtils.generateMessageID();
  private static final long MODIFICATION1 = 1000L;
  private static final long MODIFICATION2 = 2000L;
  private static final PendingUpdate PENDING_UPDATE1 = new PendingUpdate(MODIFICATION1,
      MESSAGE_ID1);
  private static final PendingUpdate PENDING_UPDATE2 = new PendingUpdate(MODIFICATION1,
      MESSAGE_ID1);
  private static final PendingUpdate PENDING_UPDATE3 = new PendingUpdate(MODIFICATION2,
      MESSAGE_ID1);
  private static final PendingUpdate PENDING_UPDATE4 = new PendingUpdate(MODIFICATION1,
      MESSAGE_ID2);

  @Test
  public void testGetModificationTime() {
    PendingUpdate pendingUpdate = new PendingUpdate(MODIFICATION1, MESSAGE_ID1);
    assertEquals(MODIFICATION1, pendingUpdate.getModificationTime());
  }

  @Test
  public void testGetMessageId() {
    PendingUpdate pendingUpdate = new PendingUpdate(MODIFICATION1, MESSAGE_ID1);
    assertEquals(MESSAGE_ID1, pendingUpdate.getMessageId());
  }

  @Test
  public void testEqualsAndHashCode() {
    assertEquals(PENDING_UPDATE1, PENDING_UPDATE2);
    assertEquals(PENDING_UPDATE1.hashCode(), PENDING_UPDATE2.hashCode());

    assertNotEquals(PENDING_UPDATE1, PENDING_UPDATE3);
    assertNotEquals(PENDING_UPDATE1.hashCode(), PENDING_UPDATE3.hashCode());

    assertNotEquals(PENDING_UPDATE1, PENDING_UPDATE4);
    assertNotEquals(PENDING_UPDATE1.hashCode(), PENDING_UPDATE4.hashCode());
  }

  @Test
  public void testCompareTo() {
    assertTrue(PENDING_UPDATE1.compareTo(PENDING_UPDATE3) < 0);
    assertTrue(PENDING_UPDATE3.compareTo(PENDING_UPDATE1) > 0);
    assertTrue(PENDING_UPDATE1.compareTo(PENDING_UPDATE2) == 0);
  }

}
