package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import java.security.GeneralSecurityException
import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class KeyPairTest {
  @Test
  @Throws(GeneralSecurityException::class)
  fun signDataUsesSignFromThePrivateKey() {
    val privateKey = Mockito.mock(PrivateKey::class.java)
    Mockito.`when`(privateKey.sign(MockitoKotlinHelpers.any())).thenReturn(SIGNATURE)
    val publicKey = Mockito.mock(PublicKey::class.java)
    Mockito.`when`(publicKey.verify(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any()))
      .thenReturn(true)
    val pair = KeyPair(privateKey, publicKey)

    Assert.assertTrue(pair.verify(SIGNATURE, DATA))
    Assert.assertEquals(SIGNATURE, pair.sign(DATA))
    Mockito.verify(privateKey).sign(MockitoKotlinHelpers.any())
    Mockito.verify(publicKey).verify(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
  }

  @Test
  fun publicKeyReturnsRightValue() {
    val privateKey = Mockito.mock(PrivateKey::class.java)
    val publicKey =
      PublicKey(
        Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7")
      )
    val keyPair = KeyPair(privateKey, publicKey)

    Assert.assertEquals(publicKey, keyPair.publicKey)
  }

  @Test
  fun pubKeyHash() {
    // Tested with value in keypair.json (see #1042)
    val pk = PublicKey("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")
    Assert.assertEquals("-_qR4IHwsiq50raa8jURNArds54=", pk.computeHash())
  }

  @Test
  fun pubKeyHash2() {
    // Tested with value in keypair.json (see #1042)
    val pk = PublicKey("oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=")
    Assert.assertEquals("SGnNfF533PBEUMYPMqBSQY83z5U=", pk.computeHash())
  }

  companion object {
    private val SIGNATURE = Signature("U0lHTkFUVVJF")
    private val DATA = Base64URLData("REFUQQ==")
  }
}
