package com.github.dedis.popstellar.model.objects.security;

import android.util.Base64;

import java.util.Arrays;

/** Represents a data that can be encoded into a Base64 form */
public class Base64URLData {

  private static final int BASE64_FLAGS = Base64.NO_WRAP | Base64.URL_SAFE;

  protected final byte[] data;

  public Base64URLData(byte[] data) {
    this.data = data;
  }

  public Base64URLData(String data) {
    this(decode(data));
  }

  public byte[] getData() {
    return Arrays.copyOf(data, data.length);
  }

  /** @return the Base64 - encoded string representation of the data */
  public String getEncoded() {
    return encode(data);
  }

  public static byte[] decode(String data) {
    return Base64.decode(data, BASE64_FLAGS);
  }

  private static String encode(byte[] date) {
    return Base64.encodeToString(date, BASE64_FLAGS);
  }
}
