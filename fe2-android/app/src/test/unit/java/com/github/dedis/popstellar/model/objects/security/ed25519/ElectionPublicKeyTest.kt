package com.github.dedis.popstellar.model.objects.security.ed25519

import ch.epfl.dedis.lib.crypto.Ed25519Pair
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair.Companion.generateKeyPair
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey
import java.nio.charset.StandardCharsets
import java.util.Objects
import org.junit.Assert
import org.junit.Test

class ElectionPublicKeyTest {
  private val nonValidMockElectionKeyString = "uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ="
  private val nonValidMockEncodedElectionKey =
    Base64URLData(nonValidMockElectionKeyString.toByteArray(StandardCharsets.UTF_8))
  private val keyPairScheme = Ed25519Pair()
  private val encodedPublicUrl = Base64URLData(keyPairScheme.point.toBytes())
  private val validEncryptionScheme = ElectionPublicKey(encodedPublicUrl)

  @Test
  fun constructorTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionPublicKey(nonValidMockEncodedElectionKey)
    }
    Assert.assertNotEquals(null, validEncryptionScheme)
  }

  @Test
  fun toStringTest() {
    val format = keyPairScheme.point.toString()
    Assert.assertEquals(format, validEncryptionScheme.toString())
  }

  @Test
  fun equalsTest() {
    val scheme = generateKeyPair()
    val that = scheme.encryptionScheme
    Assert.assertNotEquals(validEncryptionScheme, that)
    Assert.assertEquals(validEncryptionScheme, validEncryptionScheme)
    Assert.assertNotEquals(null, validEncryptionScheme)
    val hash = Objects.hash(keyPairScheme.point.toString())
    Assert.assertEquals(hash.toLong(), validEncryptionScheme.hashCode().toLong())
  }

  @Test
  fun toBase64Test() {
    val keyPoint = Base64URLData(keyPairScheme.point.toBytes())
    Assert.assertEquals(keyPoint, validEncryptionScheme.toBase64())
  }

  @Test
  fun encodeToBase64Test() {
    val keyPoint = Base64URLData(keyPairScheme.point.toBytes())
    val encoded = keyPoint.encoded
    Assert.assertEquals(encoded, validEncryptionScheme.encodeToBase64())
  }

  @Test
  fun publicKeyTest() {
    val keyPoint = keyPairScheme.point
    Assert.assertEquals(keyPoint, validEncryptionScheme.publicKey)
  }

  // Encryption / decryption process is already tested in ElectionKeyPairTest
  // We check that encryption with wrong format argument throws the appropriate exception
  @Test
  fun encryptTest() {
    // Message should not exceed 29 bytes
    val tooLongMessage = ByteArray(30)
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validEncryptionScheme.encrypt(tooLongMessage)
    }
  }
}
