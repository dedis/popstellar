package com.github.dedis.popstellar.model.objects.security.ed25519;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.util.Log;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.elGamal.*;
import org.junit.Test;

public class ElectionKeyPairTest {

  private final ElectionKeyPair encryptionKeys = ElectionKeyPair.generateKeyPair();
  private final ElectionPublicKey electionPublicKey = encryptionKeys.encryptionScheme;
  private final ElectionPrivateKey electionPrivateKey = encryptionKeys.decryptionScheme;

  @Test
  public void testKeyGeneration() {
    assertNotEquals(null, encryptionKeys);
    assertNotEquals(null, encryptionKeys.encryptionScheme);
    assertNotEquals(null, encryptionKeys.decryptionScheme);
  }

  @Test
  public void simpleEncryptionDecryptionScheme() {
    // Test basic encryption/decryption scheme
    long data = 1;
    // First transform the value into two bytes
    byte[] valueToByte = {(byte) data, (byte) (data >> 8)};
    // Encrypt
    String encryptedData = electionPublicKey.encrypt(valueToByte);
    try {
      // Decrypt
      byte[] decryptedData = electionPrivateKey.decrypt(encryptedData);
      Log.d(
          "Private base64 encoded key : ",
          new Base64URLData(electionPrivateKey.toString()).getData().toString());
      Log.d(
          "Public base64 encoded key : ",
          new Base64URLData(electionPublicKey.toString()).getData().toString());
      // Pad the decrypted data and observe the result
      int decryptedInt = ((decryptedData[1] & 0xff) << 8) | (decryptedData[0] & 0xff);
      assertEquals(data, decryptedInt);
    } catch (CothorityCryptoException e) {
    } // Exception should not catch anything
  }
}
