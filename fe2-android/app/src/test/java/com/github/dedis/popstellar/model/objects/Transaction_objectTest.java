package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Transaction_objectTest {

  private static TransactionObject transaction_object = new TransactionObject();

  @Test
  public void setAndGetChannelTest() {
    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    transaction_object.setChannel(channel);
    assertEquals(channel, transaction_object.getChannel());
  }

  // test get Inputs

  // test get Outputs

  // test get Locktime

  // test get version

  // test List<PublicKey> get_senders_transaction()

  // test List<String> get_receivers_hash_transaction()

  // test List<PublicKey> get_receivers_transaction(Map<String, PublicKey> map_hash_key)

  // test boolean is_receiver(PublicKey publicKey)

  // test String compute_sig_outputs_inputs(KeyPair keyPair)

  // test int get_miniLao_per_receiver(PublicKey receiver)

  // test int get_index_transaction(PublicKey publicKey)

}
