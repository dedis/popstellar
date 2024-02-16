package com.github.dedis.popstellar.model.objects.security

import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Test
import java.security.GeneralSecurityException

class PoPTokenTest {
  @Test
  @Throws(GeneralSecurityException::class)
  fun signAndVerifyWorks() {
    val pair = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    val signing = pair.sign(DATA)
    Assert.assertTrue(pair.verify(signing, DATA))
  }

  @Test
  fun badSignatureFails() {
    val pair = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    Assert.assertFalse(pair.verify(BAD_SIGNATURE, DATA))
  }

  @Test
  fun equalsAndHashcodeWorksWhenSame() {
    val token1 = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    val token2 = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    Assert.assertEquals(token1, token2)
    Assert.assertEquals(token1.hashCode().toLong(), token2.hashCode().toLong())
  }

  @Test
  fun equalsAndHashcodeWorksWhenDifferent() {
    val token1 = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    val token2 = PoPToken(VALID_PRIVATE_KEY2, VALID_PUBLIC_KEY2)
    Assert.assertNotEquals(token1, token2)
    Assert.assertNotEquals(token1.hashCode().toLong(), token2.hashCode().toLong())
  }

  @Test
  fun equalsSpecialCases() {
    val token = PoPToken(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY)
    Assert.assertNotEquals(token, null)
  }

  companion object {
    private val DATA = Base64URLData("REFUQQ==")
    private val BAD_SIGNATURE = Signature("U0lHTkFUVVJF")
    private val VALID_PRIVATE_KEY =
      Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")
    private val VALID_PUBLIC_KEY =
      Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7")
    private val VALID_PRIVATE_KEY2 =
      Utils.hexToBytes("cf74d353042400806ee94c3e77eef983d9a1434d21c0a7568f203f5b091dde1d")
    private val VALID_PUBLIC_KEY2 =
      Utils.hexToBytes("6015ae4d770294f94e651a9fd6ba9c6a11e5c80803c63ee472ad525f4c3523a6")
  }
}