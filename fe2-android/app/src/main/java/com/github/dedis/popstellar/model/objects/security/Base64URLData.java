package com.github.dedis.popstellar.model.objects.security;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Base64;

/** Represents a data that can be encoded into a Base64 form */
public class Base64URLData {

  protected final byte[] data;

  public Base64URLData(byte[] data) {
    // Deep copy of byte array
    this.data = Arrays.copyOf(data, data.length);
  }

  public Base64URLData(String data) {
    this(decode(data));
  }

  public byte[] getData() {
    return Arrays.copyOf(data, data.length);
  }

  /**
   * @return the Base64 - encoded string representation of the data
   */
  public String getEncoded() {
    return encode(data);
  }

  private static byte[] decode(String data) {
    return Base64.getUrlDecoder().decode(data);
  }

  private static String encode(byte[] data) {
    return Base64.getUrlEncoder().encodeToString(data);
  }

  @NonNull
  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getEncoded() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Base64URLData that = (Base64URLData) o;
    return Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
