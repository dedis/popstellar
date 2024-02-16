package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.subtle.Ed25519Sign
import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.security.GeneralSecurityException

class PrivateKeyTest {
  @Test
  @Throws(GeneralSecurityException::class)
  fun signGivesSameValueForBothKeyType() {
    val key1: PrivateKey = PlainPrivateKey(VALID_PRIVATE_KEY)
    val keyset = Mockito.mock(
      KeysetHandle::class.java
    )
    Mockito.`when`(
      keyset.getPrimitive(
        PublicKeySign::class.java
      )
    ).thenReturn(Ed25519Sign(VALID_PRIVATE_KEY))
    val key2: PrivateKey = ProtectedPrivateKey(keyset)
    val sign1 = key1.sign(DATA)
    val sign2 = key2.sign(DATA)
    Assert.assertEquals(sign1, sign2)
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun signGivesExpectedValue() {
    val key: PrivateKey = PlainPrivateKey(VALID_PRIVATE_KEY)
    val sign = key.sign(DATA)
    Assert.assertEquals(EXPECTED_SIGNATURE, sign)
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun badKeyFailsAtConstruction() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      PlainPrivateKey(
        byteArrayOf(
          0,
          1,
          2
        )
      )
    }
    val keyset = Mockito.mock(
      KeysetHandle::class.java
    )
    Mockito.`when`(
      keyset.getPrimitive(
        PublicKeySign::class.java
      )
    ).thenThrow(GeneralSecurityException())
    Assert.assertThrows(IllegalArgumentException::class.java) { ProtectedPrivateKey(keyset) }
  }

  @Test
  fun privateKeyHidesValueInStringRepresentation() {
    val key = PlainPrivateKey(VALID_PRIVATE_KEY)
    Assert.assertFalse(key.toString().contains(key.encoded))
  }

  companion object {
    private val DATA = Base64URLData("REFUQQ==")
    private val VALID_PRIVATE_KEY =
      Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")
    private val EXPECTED_SIGNATURE = Signature(
      "hhJwFWUwcm1B9PapIQ6Ct6NDRBpITP_AGsIHaU6biJ8d94uDEydGrRZ5NInIjwBqoqUa2rROgx0xA705pXkgDQ=="
    )
  }
}