package common.utils;

import java.util.Arrays;
import java.util.Base64;

public class Base64Utils {

  public static String convertB64ToString(String text){
    return new String(Base64.getDecoder().decode(text.getBytes()));
  }
}
