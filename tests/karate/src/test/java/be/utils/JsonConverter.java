package be.utils;


import com.google.crypto.tink.subtle.Ed25519Sign;
import com.intuit.karate.Json;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Base64;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;

public class JsonConverter {

  public Json fromMapToJson(Map<String,String> map){
    return Json.of(map);
  }

  private String senderPk = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  private String senderSk = "0leCDBokllJXKXT72psnqF5UYFVRxnc1BNDShY05KHQn18HMlXvQmTlz6LfbvtSrgKZ4vi3ndYN9SCForQel4w==";
  private String signature = "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==";
  private String messageIdForced = "";
//  private byte[] VALID_PRIVATE_KEY =
//    Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66");
  private byte[] bytesSecretKeyRaw = {59,
  40,-76,-85,47,-29,85,-95,61,123,36,-7,8,22,-1,6,118, -9,-105,-117,-12,98,-4,-124
    ,-15
    ,-43
    ,-39
  ,72
   , -79
  ,25
   , -20
  ,102};

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
  public Json publishМessageFromData(String stringData, int id, String channel){
    Json messageData = Json.of(stringData);
    String messageDataBase64 = convertJson(messageData);
    System.out.println("data : "+messageDataBase64+" type: "+ "publish"+ " id : "+ id+ "channel : "+ channel);
    Map<String,Object> messageJson = new LinkedHashMap<>();
    messageJson.put("method","publish");
    messageJson.put("id",id);
    Map <String,Object> paramsPart = new LinkedHashMap<>();
    paramsPart.put("channel",channel);
    Map<String,Object> messagePart  = new LinkedHashMap<>();
    messagePart.put("data",messageDataBase64);
    messagePart.put("sender",senderPk);

    try {
      byte[] bytes = senderSk.getBytes(StandardCharsets.UTF_8);
      String senderSk2 = "d257820c1a249652572974fbda9b27a85e54605551c6773504d0d2858d39287427d7c1cc957bd0993973e8b7dbbed4ab80a678be2de775837d482168ad07a5e3";
      byte[] bytes2 = senderSk2.getBytes(StandardCharsets.UTF_8);
      System.out.println("length is "+ bytes2.length);
      PublicKeySign signer = new Ed25519Sign(bytesSecretKeyRaw);
      System.out.println("*************************************************************************");
      String signature = new String(signer.sign(stringData.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      System.out.println("Singature is "+ signature);
    }catch (Exception e){
      System.out.println("CANNOT SIGN");
      e.printStackTrace();
    }
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
