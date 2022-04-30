package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Map;

public class Transaction {
  private final MessageID messageId;
  private final Channel channel;
  private final int version;
  private final long timestamp;
  private final PublicKey sender;
  private final Integer overall_money;
  private final Map<PublicKey, Integer> receiver_amount;
}
