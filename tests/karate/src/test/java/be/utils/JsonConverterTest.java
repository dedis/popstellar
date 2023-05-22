package be.utils;

import com.intuit.karate.Json;
import common.utils.Base64Utils;
import org.checkerframework.checker.units.qual.K;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverterTest {

  private Json constructJsonDataForValidLao() {
    Map<String, Object> laoData = new LinkedHashMap<>();
    laoData.put("object", "lao");
    laoData.put("action", "create");
    laoData.put("name", "LAO");
    laoData.put("creation", 1633035721);
    laoData.put("organizer", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=");
    String[] witness = new String[0];
    laoData.put("witnesses", witness);
    laoData.put("id", "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=");
    return Json.of(laoData);
  }

  @Test
  public void testJsonDataToBase64PrintsInDesiredFormat() {
    KeyPair keyPair = new KeyPair();
    JsonConverter jsonConverter = new JsonConverter(keyPair.getPublicKey(), keyPair.getPrivateKey());
    Map<String, Object> testMap = new LinkedHashMap<>();
    testMap.put("test1", "test2");
    Json testJson = Json.of(testMap);
    Json testConverter = jsonConverter.publishMessageFromData(testJson.toString(), 2, "/root");
    String jsonString = testConverter.toString();
    System.out.println(jsonString);
  }

  @Test
  public void testIfMessageFromDataCorrespondsToTrueMessageForValidLao() {
    String rawBase64ValidLaoData =
        "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9";
    Json laoDataJson = constructJsonDataForValidLao();

    String constructedData = Base64Utils.convertJsonToBase64(laoDataJson);
    assert constructedData.equals(rawBase64ValidLaoData);
  }

  @Test
  public void constructJsonMessageFromDataCorrespondsToTrueJsonMessage() {
    String laoDataJsonString = constructJsonDataForValidLao().toString();
    JsonConverter jsonConverter = new JsonConverter();
    Json jsonValidLaoMessage = jsonConverter.publishMessageFromData(laoDataJsonString, 1, "/root");
    Map<String, Object> validStringMessage = new LinkedHashMap<>();
    validStringMessage.put("method", "publish");
    validStringMessage.put("id", 1);
    Map<String, Object> paramsMap = new LinkedHashMap<>();
    paramsMap.put("channel", "/root");
    Map<String, Object> messageMap = new LinkedHashMap<>();

    // fields are taken from valid_lao_create.json
    messageMap.put(
        "data",
        "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9");
    messageMap.put("sender", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=");
    messageMap.put(
        "signature",
        "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==");
    messageMap.put("message_id", "2mAAevx61TZJi4groVGqqkeLEQq0e-qM6PGmTWuShyY=");

    String[] witness = new String[0];
    messageMap.put("witness_signatures", witness);
    paramsMap.put("message", messageMap);
    validStringMessage.put("params", paramsMap);
    validStringMessage.put("jsonrpc", "2.0");
    Json toCompare = Json.of(validStringMessage);

    assert toCompare.toString().equals(jsonValidLaoMessage.toString());
  }

  @Test
  public void testIfHashIsCorrect() throws NoSuchAlgorithmException {
    // The message examined is a valid message sent by fe2 and can be found
    // in data/lao/valid_lao_create_fe2.json folder
    // This data was collected from a message sent by fe-2 and can be found in
    // data/lao/data/valid_lao_create_fe2_data.json folder
    String signature_sent =
        "QZMdg27-pdQKUDQMlsQY1uAiIgHtT5s29n7kUUI4cHdAGVRMNopC7wVJHvpC1j6p95RN0IqZyC-VhldE0bMgAA==";
    String message_id_sent = "ZV_OUCYDlF2ScULlLDmjZuiqzcbujEgjcaoFmLsArpU=";
    String dataBase64_sent =
        "eyJjcmVhdGlvbiI6MTY0ODgxOTE0OCwiaWQiOiJoTVNoM2t2a0lHMGdBcWpLQ3ZrajBQV2Eza3VHd21PdnR5SEVua2d4UzdrPSIsIm5hbWUiOiJsYW80Iiwib3JnYW5pemVyIjoiWUdaR25NUkFkS2J3MDZpX1lZczFxVDA3TlBZTEFLMGNubnpDcnNDTFE0QT0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==";

    // Testing if the hash function works
    String message_id_check =
        Hash.hash(dataBase64_sent.getBytes(), signature_sent.getBytes());

    assert message_id_sent.equals(message_id_check);
  }

  @Test
  public void checkRegisteredVoteFieldIsValid() throws NoSuchAlgorithmException {
    // registered votes filed present in valid_election_data.json
    String registeredVotes = "GX9slST3yY_Mltkjimp-eNq71mfbSbQ9sruABYN8EoM=";
    // registered votes constructed with the id of the cast vot present in valid_cast_vote_2_data.json
    String realRegisteredVotes = Hash.hash("8L2MWJJYNGG57ZOKdbmhHD9AopvBaBN26y1w5jL07ms=".getBytes());
    assert registeredVotes.equals(realRegisteredVotes);
  }

}
