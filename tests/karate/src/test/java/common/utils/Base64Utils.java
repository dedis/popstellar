package common.utils;

import java.util.Base64;

public class Base64Utils {
    public static byte[] convertB64URLToString(String text){
    return Base64.getUrlDecoder().decode(text.getBytes());
  }
}
