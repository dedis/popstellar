package com.github.dedis.popstellar.model.objects;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.elGamal.*;

import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import ch.epfl.dedis.lib.exception.CothorityCryptoException;

import static com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion.OPEN_BALLOT;
import static com.github.dedis.popstellar.model.objects.event.EventType.ELECTION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ElectionTest {

  // Generate public key and populate the election key field
  ElectionKeyPair encryptionKeys = ElectionKeyPair.generateKeyPair();
  ElectionPublicKey electionPublicKey = encryptionKeys.getEncryptionScheme();
  ElectionPrivateKey electionPrivateKey = encryptionKeys.getDecryptionScheme();

  private final String name = "my election name";
  private final Election election =
      new Election.ElectionBuilder("lao_id", Instant.now().getEpochSecond(), name)
          .setElectionVersion(OPEN_BALLOT)
          .setElectionKey(
              new Base64URLData(electionPublicKey.getPublicKey().toBytes()).getEncoded())
          .build();

  // Add some vote for decryption/encryption testing purposes
  private final String questionId1 = " myQuestion1";

  // Set up a open ballot election
  private final PlainVote plainVote1 = new PlainVote(questionId1, 1, false, null, election.getId());
  private final List<PlainVote> plainVotes = Collections.singletonList(plainVote1);

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Test
  public void getVersionTest() {
    assertThat(ElectionVersion.OPEN_BALLOT, is(election.getElectionVersion()));
  }

  @Test
  public void electionEncryptionProcess() {
    // First encrypt
    List<EncryptedVote> encryptedVotes = election.encrypt(plainVotes);

    // Compare results
    for (int i = 0; i < encryptedVotes.size(); i++) {
      EncryptedVote e = encryptedVotes.get(i);
      PlainVote o = plainVotes.get(i);
      try {
        byte[] decryptedData = electionPrivateKey.decrypt(e.getVote());
        // Pad the result
        int decryptedINt = ((decryptedData[1] & 0xff)) | (decryptedData[0] & 0xff << 8);
        int openVoteIndice = o.getVote();
        assertEquals(openVoteIndice, decryptedINt);
      } catch (CothorityCryptoException exception) {
        // Shouldn't do anything
      }
    }
  }

  @Test
  public void getCreationInMillisTest() {
    assertEquals(election.getCreation() * 1000, election.getCreationInMillis());
  }

  @Test
  public void getTypeTest() {
    assertEquals(ELECTION, election.getType());
  }
}
