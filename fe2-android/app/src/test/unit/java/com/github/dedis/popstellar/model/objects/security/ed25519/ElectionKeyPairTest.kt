package com.github.dedis.popstellar.model.objects.security.ed25519

import android.util.Log
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair.Companion.generateKeyPair
import org.junit.Assert
import org.junit.Test

class ElectionKeyPairTest {
  private val encryptionKeys = generateKeyPair()
  private val electionPublicKey = encryptionKeys.encryptionScheme
  private val electionPrivateKey = encryptionKeys.decryptionScheme

  @Test
  fun testKeyGeneration() {
    Assert.assertNotEquals(null, encryptionKeys)
    Assert.assertNotEquals(null, encryptionKeys.encryptionScheme)
    Assert.assertNotEquals(null, encryptionKeys.decryptionScheme)
  }

  @Test
  fun simpleEncryptionDecryptionScheme() {
    // Test basic encryption/decryption scheme
    val data: Long = 1
    // First transform the value into two bytes
    val valueToByte = byteArrayOf(data.toByte(), (data shr 8).toByte())
    // Encrypt
    val encryptedData = electionPublicKey.encrypt(valueToByte)
    try {
      // Decrypt
      val decryptedData = electionPrivateKey.decrypt(encryptedData!!)
      Log.d(
        "Private base64 encoded key : ",
        Base64URLData(electionPrivateKey.toString()).data.toString()
      )
      Log.d(
        "Public base64 encoded key : ",
        Base64URLData(electionPublicKey.toString()).data.toString()
      )
      // Pad the decrypted data and observe the result
      val decryptedInt =
        decryptedData[1].toInt() and 0xff shl 8 or (decryptedData[0].toInt() and 0xff)
      Assert.assertEquals(data, decryptedInt.toLong())
    } catch (_: CothorityCryptoException) {} // Exception should not catch anything
  }
}
