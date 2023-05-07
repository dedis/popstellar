package be.utils;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;
import com.intuit.karate.Json;
import common.utils.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverter {

  public String publicKey;
  public byte [] privateKey;

  private String signatureForced;
  private boolean isSignatureForced = false;
  private String messageIdForced = "";


  /** Produces the base64 variant of the json file passed as argument */
  public String convertJsonToBase64(Json json) {
    String stringJson = json.toString();
    byte[] jsonBytes = stringJson.getBytes();
    Base64.Encoder encoder = Base64.getEncoder();
    return encoder.encodeToString(jsonBytes);
  }

  public JsonConverter(){
    this.publicKey = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
    //this.privateKeyHex = "d257820c1a249652572974fbda9b27a85e54605551c6773504d0d2858d392874";
  }
  public JsonConverter(String publicKey, byte [] privateKey){
    this.publicKey = publicKey;
    this.privateKey = privateKey;


    System.out.println("privateKey in socket: " + Base64Utils.encode(privateKey));
    System.out.println("publicKey in socket: " + publicKey);
  }


  /**
   * Produces a valid Json representation of a message given the message data, the id of the message
   * and the channel where the message is supposed to be sent
   */
  public Json publishMessageFromData(String stringData, int id, String channel) {
    Json messageData = Json.object();
    messageData.set("method", "publish");
    messageData.set("id", id);
    Map<String, Object> paramsPart = new LinkedHashMap<>();
    try{
    paramsPart = constructParamsField(channel, stringData);
    messageData.set("params", paramsPart);
    }catch (GeneralSecurityException e){
      e.printStackTrace();
    }
    messageData.set("jsonrpc", "2.0");

    return messageData;
  }

  public Map<String, Object> constructMessageField(String stringData) throws GeneralSecurityException, NoSuchAlgorithmException {
    Map<String, Object> messagePart = new LinkedHashMap<>();
    Json messageData = Json.of(stringData);
    String messageDataBase64 = convertJsonToBase64(messageData);
    String signature = constructSignature(stringData);
    if(isSignatureForced){
      signature = this.signatureForced;
      isSignatureForced = false;
    }
    System.out.println("signature used was: " + signature);
    String messageId = hash(messageDataBase64.getBytes(), signature.getBytes());
    String[] witness = new String[0];

    messagePart.put("data", messageDataBase64);
    System.out.println("publicKey used was : " + publicKey);
    messagePart.put("sender", publicKey);
    messagePart.put("signature", signature);
    messagePart.put("message_id", messageId);
    System.out.println("message id is : " + messageId);
    messagePart.put("witness_signatures", witness);

    return messagePart;
  }

  public Map<String, Object> constructParamsField(String channel, String messageDataBase64) throws GeneralSecurityException {
    Map<String, Object> paramsPart = new LinkedHashMap<>();
    Map<String, Object> messagePart = constructMessageField(messageDataBase64);

    paramsPart.put("channel", channel);
    paramsPart.put("message", messagePart);

    return paramsPart;
  }

  /** Constructs a valid signature on given data */
  public String constructSignature(String messageData) throws GeneralSecurityException {
      // Hex representation of the private key
      PublicKeySign publicKeySign = new Ed25519Sign(privateKey);
      byte[] signBytes = publicKeySign.sign(messageData.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(signBytes);
  }

  /**
   * If we want to test having a signature that does not match the data and private key we can set
   * it by force
   */
  public void setSignature(String newSignature) {
    isSignatureForced = true;
    this.signatureForced = newSignature;
  }

  /**
   * If we want to test how the backend behaves with a non-valid message id we can set it by force
   */
  public void setMessageIdForced(String messageIdForced) {
    this.messageIdForced = messageIdForced;
  }

  /** Hashes an arbitrary number of arguments */
  public String hash(byte[]... allData) throws NoSuchAlgorithmException {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (byte[] data : allData) {
        String dataLength = Integer.toString(data.length);
        digest.update(dataLength.getBytes());
        digest.update(data);
      }
      return Base64.getUrlEncoder().encodeToString(digest.digest());
  }

}
