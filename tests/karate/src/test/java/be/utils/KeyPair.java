package be.utils;

import common.utils.Base64Utils;
import karate.com.linecorp.armeria.internal.shaded.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import karate.com.linecorp.armeria.internal.shaded.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.util.Random;

public class KeyPair {

  private static final long SEED = 2024;
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

  public byte [] getPublicKeyBytes(){
    return publicKey;
  }

  public byte [] getPrivateKeyBytes(){
    return privateKey;
  }

  public String getPublicKey(){
    return Base64Utils.encode(publicKey);
  }

  public String getPrivateKey(){
    return Base64Utils.encode(privateKey);
  }

}
