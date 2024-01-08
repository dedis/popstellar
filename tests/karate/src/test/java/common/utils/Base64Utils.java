package common.utils;

import com.intuit.karate.Json;

import java.util.Base64;

public class Base64Utils {

  /**
   * @param text the Base64URL text to decode and convert
   * @return the converted byte array
   */
    public static byte[] convertB64URLToByteArray(String text){
    return Base64.getUrlDecoder().decode(text.getBytes());
  }

  public static String encode(byte[] data) {
    return Base64.getUrlEncoder().encodeToString(data);
  }

  public static byte[] decode(String data) {
    return Base64.getUrlDecoder().decode(data);
  }


  /** Produces the base64 variant of the json file passed as argument */
  public static String convertJsonToBase64(Json json) {
    String stringJson = json.toString();
    return Base64Utils.encode(stringJson.getBytes());
  }

  /** Extracts the Base64 encoded data of a (possibly nested) field in a Json and decodes it */
  public static String decodeBase64JsonField(Json json, String fieldPath) {
    String base64Data = json.get(fieldPath);
    return new String(decode(base64Data));
  }
}
