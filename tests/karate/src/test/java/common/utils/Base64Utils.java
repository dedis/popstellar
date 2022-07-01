package common.utils;

import java.util.Base64;

public class Base64Utils {

  /**
   * @param text the Base64URL text to decode and convert
   * @return the converted byte array
   */
    public static byte[] convertB64URLToByteArray(String text){
    return Base64.getUrlDecoder().decode(text.getBytes());
  }
}
