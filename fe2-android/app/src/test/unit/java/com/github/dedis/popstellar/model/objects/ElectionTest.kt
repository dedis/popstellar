package com.github.dedis.popstellar.model.objects

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair.Companion.generateKeyPair
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ElectionTest {
  // Generate public key and populate the election key field
  private var encryptionKeys = generateKeyPair()
  private var electionPublicKey = encryptionKeys.encryptionScheme
  private var electionPrivateKey = encryptionKeys.decryptionScheme
  private val name = "my election name"
  private val election =
    ElectionBuilder("lao_id", Instant.now().epochSecond, name)
      .setElectionVersion(ElectionVersion.OPEN_BALLOT)
      .setElectionKey(Base64URLData(electionPublicKey.publicKey.toBytes()).encoded)
      .build()

  // Add some vote for decryption/encryption testing purposes
  private val questionId1 = " myQuestion1"

  // Set up a open ballot election
  private val plainVote1 = PlainVote(questionId1, 1, false, null, election.id)
  private val plainVotes = listOf(plainVote1)

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @Test
  fun versionTest() {
    MatcherAssert.assertThat(
      ElectionVersion.OPEN_BALLOT,
      CoreMatchers.`is`(election.electionVersion)
    )
  }

  @Test
  fun electionEncryptionProcess() {
    // First encrypt
    val encryptedVotes = election.encrypt(plainVotes)

    // Compare results
    for (i in encryptedVotes.indices) {
      val e = encryptedVotes[i]
      val o = plainVotes[i]
      try {
        val decryptedData = electionPrivateKey.decrypt(e.vote!!)
        // Pad the result
        val decryptedINt =
          decryptedData[1].toInt() and 0xff or (decryptedData[0].toInt() and (0xff shl 8))
        val openVoteIndice = o.vote!!
        Assert.assertEquals(openVoteIndice.toLong(), decryptedINt.toLong())
      } catch (exception: CothorityCryptoException) {
        exception.printStackTrace()
      }
    }
  }

  @Test
  fun creationInMillisTest() {
    Assert.assertEquals(election.creation * 1000, election.creationInMillis)
  }

  @Test
  fun typeTest() {
    Assert.assertEquals(EventType.ELECTION, election.type)
  }
}
