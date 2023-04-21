package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.model.Immutable;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.*;
import java.util.Arrays;
import java.util.Base64;

/** A public key that can be used to verify a signature */
@Immutable
public class PublicKey extends Base64URLData {

  private static final Logger logger = LogManager.getLogger(PublicKey.class);

  private final PublicKeyVerify verifier;

  public PublicKey(byte[] data) {
    super(data);
    verifier = new Ed25519Verify(data);
  }

  public PublicKey(String data) {
    super(data);
    verifier = new Ed25519Verify(this.data);
  }

  public boolean verify(Signature signature, Base64URLData data) {
    try {
      verifier.verify(signature.getData(), data.getData());
      return true;
    } catch (GeneralSecurityException e) {
      logger.error("failed to verify witness signature", e);
      return false;
    }
  }

  /**
   * Function that compute the hash of a public key
   *
   * @return String which correspond to the SHA256 Hash
   */
  public String computeHash() {
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(this.getData());
      return Base64.getUrlEncoder().encodeToString(Arrays.copyOf(hash, 20));
    } catch (NoSuchAlgorithmException e) {
      logger.error("Something is wrong by hashing the String element", e);
      throw new IllegalArgumentException("Error in computing the hash in public key");
    }
  }
}
