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

  /*
    Produces the base64 variant of the json file passed as argument
   */
  public String convertJson(Json json){
    String stringJson = json.toString();
    byte[] jsonBytes = stringJson.getBytes(StandardCharsets.UTF_8);
    Base64.Encoder encoder = Base64.getEncoder();
    String base64 = encoder.encodeToString(jsonBytes);
    return base64;
  }

  /*
    Produces a valid Json representation of a message given the message data, the id of the message
    and the channel where the message is supposed to be sent
   */
  public Json messageFromData(String stringData, String method, int id, String channel){
    Json messageData = Json.of(stringData);
    String messageDataBase64 = convertJson(messageData);
    System.out.println("data : "+messageDataBase64+" type: "+ method+ " id : "+ id+ "channel : "+ channel);
    Map<String,Object> messageJson = new LinkedHashMap<>();
    messageJson.put("method",method);
    messageJson.put("id",id);
    Map <String,Object> paramsPart = new LinkedHashMap<>();
    paramsPart.put("channel",channel);
    Map<String,Object> messagePart  = new LinkedHashMap<>();
    messagePart.put("data",messageDataBase64);
    messagePart.put("sender",senderPk);
    messagePart.put("signature",signature);

    String messageId = hashDataSignature(messageDataBase64.getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8));
    messagePart.put("message_id",messageId);
    System.out.println("message id is : "+messageId);

    String[] witness = new String[0];
    messagePart.put("witness_signatures",witness);
    paramsPart.put("message",messagePart);
    messageJson.put("params",paramsPart);
    messageJson.put("jsonrpc","2.0");
    return Json.of(messageJson);
  }
  /*
    Produces the hexadecimal representation of a hash (given as an array of bytes)
   */
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

  private String hashDataSignature(byte[] data, byte[] signature){
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String dataLength = Integer.toString(data.length);
      String signatureLength = Integer.toString(signature.length);
      digest.update(dataLength.getBytes(StandardCharsets.UTF_8));
      digest.update(data);
      digest.update(signatureLength.getBytes(StandardCharsets.UTF_8));
      digest.update(signature);
      return Base64.getUrlEncoder().encodeToString(digest.digest());
    }catch (Exception e){
      e.printStackTrace();
    }
    return "Hash is not constructed correctly";
  }

}
