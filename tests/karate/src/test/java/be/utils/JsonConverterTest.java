package be.utils;

import com.intuit.karate.Json;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverterTest {

  @Test
  public void testJsonDataToBase64Works(){
    JsonConverter jsonConverter = new JsonConverter();
    Map<String,Object> testMap = new LinkedHashMap<>();
    testMap.put("test1","test2");
    Json testJson = Json.of(testMap);
    System.out.println("The string of the jsonTest is : "+testJson.toString());
    Json testConverter = jsonConverter.messageFromData(testJson.toString(),"publish",2, "/root");
    System.out.println("converter gives : "+ testConverter.toString());
  }


}

