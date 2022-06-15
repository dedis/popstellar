package common.utils;

import com.intuit.karate.Json;

import java.util.Map;

public class JsonUtils {

  /**
   * @param json the json whose Json field we want
   * @param key the key with which the Json is stored in json
   * @return The Json contained in json with key
   */
  public static Json getJSON(Json json, String key){
    Map<String, String> map = json.get(key);
    return Json.of(map);
  }
}
