package be.utils;


import com.intuit.karate.Json;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Base64;

public class JsonConverter {

  public Json fromMapToJson(Map<String,String> map){
    return Json.of(map);
  }

  private String senderPk = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  private String signature = "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==";
  private String messageIdForced = "";

  public String convertJson(Json json){

    String stringJson = json.toString();
    byte[] jsonBytes = stringJson.getBytes(StandardCharsets.UTF_8);
    Base64.Encoder encoder = Base64.getEncoder();
    String base64 = encoder.encodeToString(jsonBytes);
    return base64;
  }


  public Json messageFromData(String stringData,String type, int id, String channel){
    Json messageData = Json.of(stringData);
    String messageDataBase64 = convertJson(messageData);
    System.out.println("data : "+messageDataBase64+" type: "+ type+ " id : "+ id+ "cannel : "+ channel);
    Map<String,Object> messageJson = new LinkedHashMap<>();
    messageJson.put("method",type);
    messageJson.put("id",id);
    Map <String,Object> paramsPart = new LinkedHashMap<>();
    paramsPart.put("channel",channel);
    Map<String,Object> messagePart  = new LinkedHashMap<>();
    messagePart.put("data",messageDataBase64);
    messagePart.put("sender",senderPk);
    messagePart.put("signature",signature);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String concat = messageDataBase64+signature;
      String messageId =bytesToHex(digest.digest(concat.getBytes(StandardCharsets.UTF_8)));
      messagePart.put("message_id",messageId);
      System.out.println("message id is : "+messageId);
    }catch (Exception e){

    }
    String[] witness = new String[0];
    messagePart.put("witness_signatures",witness);
    paramsPart.put("message",messagePart);
    messageJson.put("params",paramsPart);
    messageJson.put("jsonrpc","2.0");
    return Json.of(messageJson);
  }

  private String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if(hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  public void setSenderPk(String newSenderPk){
    this.senderPk = newSenderPk;
  }

  public void setSignature(String newSignature) {
    this.signature = newSignature;
  }

  public void setMessageIdForced(String messageIdForced){
    this.messageIdForced = messageIdForced;
  }
}
