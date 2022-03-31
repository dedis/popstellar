package be.utils;

import com.intuit.karate.Json;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverterTest {

  private Json constructJsonDataForValidLao(){
    Map<String,Object> laoData = new LinkedHashMap<>();
    laoData.put("object","lao");
    laoData.put("action","create");
    laoData.put("name","LAO");
    laoData.put("creation",1633035721);
    laoData.put("organizer","J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=");
    String witness[] = new String[0];
    laoData.put("witnesses",witness);
    laoData.put("id","p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=");
    Json laoDataJson = Json.of(laoData);
    return laoDataJson;
  }


  @Test
  public void testJsonDataToBase64PrintsInDesiredFormat(){
    JsonConverter jsonConverter = new JsonConverter();
    Map<String,Object> testMap = new LinkedHashMap<>();
    testMap.put("test1","test2");
    Json testJson = Json.of(testMap);
    Json testConverter = jsonConverter.publishМessageFromData(testJson.toString(),2, "/root");
    String jsonString = testConverter.toString();
    System.out.println(jsonString);
  }

  @Test
  public void testIfMessageFromDataCorrespondsToTrueMessageForValidLao(){
    JsonConverter jsonConverter = new JsonConverter();

    String rawBase64ValidLaoData = "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9";
    Json laoDataJson = constructJsonDataForValidLao();

    String constructedData = jsonConverter.convertJson(laoDataJson);
    assert constructedData.equals(rawBase64ValidLaoData);
  }

  @Test
  public void constructJsonMessageFromDataCorrespondsToTrueJsonMessage(){
    String laoDataJsonString = constructJsonDataForValidLao().toString();
    JsonConverter jsonConverter = new JsonConverter();
    Json jsonValidLaoMessage= jsonConverter.publishМessageFromData(laoDataJsonString,1,"/root");
    Map<String,Object> validStringMessage = new LinkedHashMap<>();
    validStringMessage.put("method","publish");
    validStringMessage.put("id",1);
    Map<String,Object> paramsMap = new LinkedHashMap<>();
    paramsMap.put("channel","/root");
    Map<String,Object> messageMap  = new LinkedHashMap<>();
    messageMap.put("data","eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9");
    messageMap.put("sender","J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=");
    messageMap.put("signature","PLGEtiVT2KFFIxJGJLdrNF6dK5wkrRkGeOS0ALdYdbWm1jJE4srWDNMg3vB0EaDWK-07WqSDWNBYVajF66MGCw==");
    messageMap.put("message_id","LitOGt6JuFSctbPlL-VB-4lv88UxHjfV2E0R7kUXE6k=");
    String[] witness = new String[0];
    messageMap.put("witness_signatures",witness);
    paramsMap.put("message",messageMap);
    validStringMessage.put("params",paramsMap);
    validStringMessage.put("jsonrpc","2.0");
    Json toCompare = Json.of(validStringMessage);

    System.out.println("////////////////////////////////////////////////////////");
    System.out.println("the sending message is "+ jsonValidLaoMessage.toString());
    System.out.println("the sending message is "+ toCompare.toString());
    System.out.println("////////////////////////////////////////////////////////");

    assert toCompare.toString().equals(jsonValidLaoMessage.toString());

  }
}
