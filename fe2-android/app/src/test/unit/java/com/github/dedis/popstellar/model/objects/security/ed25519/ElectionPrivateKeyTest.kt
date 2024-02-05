package com.github.dedis.popstellar.model.objects.security.ed25519

import ch.epfl.dedis.lib.crypto.Ed25519Pair
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair.Companion.generateKeyPair
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPrivateKey
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey
import org.junit.Assert
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Objects

class ElectionPrivateKeyTest {
  private val nonValidMockElectionKeyString = "uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ="
  private val nonValidMockEncodedElectionKey = Base64URLData(
    nonValidMockElectionKeyString.toByteArray(
      StandardCharsets.UTF_8
    )
  )
  private val keyPairScheme = Ed25519Pair()
  private val encodedPrivateUrl = Base64URLData(keyPairScheme.scalar.toBytes())
  private val validDecryptionScheme = ElectionPrivateKey(encodedPrivateUrl)
  @Test
  fun constructorTest() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { ElectionPublicKey(nonValidMockEncodedElectionKey) }
    Assert.assertNotEquals(null, validDecryptionScheme)
  }

  @Test
  fun toStringTest() {
    val format = keyPairScheme.scalar.toString()
    Assert.assertEquals(format, validDecryptionScheme.getPrivateKey().toString())
  }

  @Test
  fun equalsTest() {
    val scheme = generateKeyPair()
    val that = scheme.decryptionScheme
    Assert.assertEquals(validDecryptionScheme, validDecryptionScheme)
    Assert.assertNotEquals(that, validDecryptionScheme)
    Assert.assertNotEquals(null, validDecryptionScheme)
    val hash = Objects.hash(keyPairScheme.scalar.toString())
    Assert.assertEquals(hash.toLong(), validDecryptionScheme.hashCode().toLong())
  }

  // Encryption / decryption process is already tested in ElectionKeyPairTest
  // We check that encryption with wrong format argument throws the appropriate exception
  @Test
  fun decryptTest() {
    val wrongBase64Encoding = "123"
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { validDecryptionScheme.decrypt(wrongBase64Encoding) }
    val wrongLengthMessage = "LX-_Hw=="
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { validDecryptionScheme.decrypt(wrongLengthMessage) }
  }
}