package com.github.dedis.popstellar.repository;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import java.time.Instant;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

public class ConsensusRepositoryTest {

  private static final ConsensusRepository consensusRepository = new ConsensusRepository();
  private static final String laoId =
      Lao.generateLaoId(generatePublicKey(), Instant.now().getEpochSecond(), "LaoName");
  private static final Set<PublicKey> WITNESSES_WITH_NULL =
      Sets.newSet(generatePublicKey(), null, generatePublicKey());

  @Before
  public void setUp() {}

  @Test
  public void setNullWitnessesTest() {
    assertThrows(
        IllegalArgumentException.class, () -> consensusRepository.initKeyToNode(laoId, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> consensusRepository.initKeyToNode(laoId, WITNESSES_WITH_NULL));
  }
}
