package com.github.dedis.popstellar.model.objects.security.elGamal;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;

import ch.epfl.dedis.lib.crypto.*;
import ch.epfl.dedis.lib.exception.CothorityCryptoException;
import io.reactivex.annotations.NonNull;
import timber.log.Timber;

/** Represents a private key meant for El-Gam */
public class ElectionPublicKey {

  private static final String TAG = ElectionPublicKey.class.getSimpleName();

  // Point is generate with given public key
  private final Point publicKey;

  public ElectionPublicKey(@NonNull Base64URLData base64publicKey) {
    try {
      publicKey = new Ed25519Point(base64publicKey.getData());
    } catch (CothorityCryptoException e) {
      throw new IllegalArgumentException(
          "Could not create the point for elliptic curve, please provide another key");
    }
  }

  @Override
  public String toString() {
    return publicKey.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionPublicKey that = (ElectionPublicKey) o;
    return that.getPublicKey().equals(getPublicKey());
  }

  /**
   * @return the string encoded public key in Base64 format
   */
  public String encodeToBase64() {
    Base64URLData encodedKey = new Base64URLData(publicKey.toBytes());
    return encodedKey.getEncoded();
  }

  /**
   * @return the public key in Base64 format (not encoded)
   */
  public Base64URLData toBase64() {
    return new Base64URLData(publicKey.toBytes());
  }

  public Point getPublicKey() {
    return publicKey;
  }

  /** Added this hash behavior by default */
  @Override
  public int hashCode() {
    return Objects.hash(getPublicKey().toString());
  }

  /**
   * Encrypts with ElGamal under Ed25519 curve
   *
   * @param message message a 2 byte integer corresponding to the chosen vote
   * @return Returns 2 32 byte strings which should be appended together and base64 encoded. For
   *     clarification, the first 32 bytes is point K and the second 32 bytes is point C.
   */
  public String encrypt(@NonNull byte[] message) {
    if (message.length > 29) {
      throw new IllegalArgumentException("The message should contain at maximum 29 bytes");
    }
    // Follows this implementation:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L15-L23
    // An example for embedding was found here:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/external/js/kyber/spec/group/edwards25519/point.spec.ts#L203

    // Proper embedding with overflowing byte array (len > 32) will need to be changed later
    Point M = Ed25519Point.embed(message);

    // ElGamal-encrypt the point to produce ciphertext (K,C).
    byte[] seed = new byte[Ed25519.field.getb() / 8];
    (new SecureRandom()).nextBytes(seed);
    Scalar k = new Ed25519Scalar(seed);
    Point K = Ed25519Point.base().mul(k);
    Point S = getPublicKey().mul(k);
    Point C = S.add(M);

    // Concat K and C and encodes it in Base64
    byte[] result;
    // IO exception no testable
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      output.write(K.toBytes());
      output.write(C.toBytes());
      result = output.toByteArray();
    } catch (IOException e) {
      Timber.tag(TAG)
          .d(
              "Something happened during the encryption, could concatenate the final result into a byte array");
      result = null;
    }
    if (Objects.isNull(result)) {
      return null;
    }
    Base64URLData encodedResult = new Base64URLData(result);
    return encodedResult.getEncoded();
  }
}
