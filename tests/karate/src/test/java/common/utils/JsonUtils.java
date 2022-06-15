package common.utils;

import com.intuit.karate.Json;

import java.util.Map;

public class JsonUtils {

  public static Json getJSON(Json json, String key){
    Map<String, String> map = json.get(key);
    return Json.of(map);
  }
}
