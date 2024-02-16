package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import java.time.Instant
import org.junit.Assert
import org.junit.Test
import org.mockito.internal.util.collections.Sets

class ConsensusRepositoryTest {

  private val consensusRepository = ConsensusRepository()
  private val laoId =
    generateLaoId(Base64DataUtils.generatePublicKey(), Instant.now().epochSecond, "LaoName")
  private val WITNESSES_WITH_NULL =
    Sets.newSet(Base64DataUtils.generatePublicKey(), null, Base64DataUtils.generatePublicKey())

  @Test
  fun setNullWitnessesTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      consensusRepository.initKeyToNode(laoId, null)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      consensusRepository.initKeyToNode(laoId, WITNESSES_WITH_NULL)
    }
  }
}
