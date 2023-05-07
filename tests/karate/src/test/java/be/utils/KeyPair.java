package be.utils;

import karate.com.linecorp.armeria.internal.shaded.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import karate.com.linecorp.armeria.internal.shaded.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.util.Base64;
import java.util.Random;

public class KeyPair {

  private static final long SEED = 2023;
  private static final Random RANDOM = new Random(SEED);
  private static final int KEY_LENGTH = 32;
  private byte[] publicKey;
  private byte[] privateKey;


  /**
   * @return a pseudo randomly generated valid Ed25519 keypair
   */
  public KeyPair() {
    byte[] privateKey = new byte[KEY_LENGTH];
    RANDOM.nextBytes(privateKey);

    Ed25519PrivateKeyParameters privateKeyParameters =
      new Ed25519PrivateKeyParameters(privateKey, 0);
    Ed25519PublicKeyParameters publicKeyParameters = privateKeyParameters.generatePublicKey();

    this.privateKey = privateKey;
    this.publicKey = publicKeyParameters.getEncoded();
  }

  public String getPublicKey(){
    return Base64.getUrlEncoder().encodeToString(publicKey);
  }

  public String getPrivateKey(){
    return Base64.getUrlEncoder().encodeToString(privateKey);
  }

  public byte[] getPrivateKeyBytes(){
    return privateKey;
  }

  public String getPrivateKeyHex(){
    return bytesToHex(privateKey);
  }


  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
