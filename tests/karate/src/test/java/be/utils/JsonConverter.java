package be.utils;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;
import com.intuit.karate.Json;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverter {

  private String senderPk = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  private String privateKeyHex = "d257820c1a249652572974fbda9b27a85e54605551c6773504d0d2858d392874";
  private String signatureForced =
      "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==";
  private boolean isSignatureForced = false;
  private String messageIdForced = "";

  public Json fromMapToJson(Map<String, String> map) {
    return Json.of(map);
  }

  /** Produces the base64 variant of the json file passed as argument */
  public String convertJsonToBase64(Json json) {
    String stringJson = json.toString();
    byte[] jsonBytes = stringJson.getBytes();
    Base64.Encoder encoder = Base64.getEncoder();
    return encoder.encodeToString(jsonBytes);
  }

  /**
   * Produces a valid Json representation of a message given the message data, the id of the message
   * and the channel where the message is supposed to be sent
   */
  public Json publishМessageFromData(String stringData, int id, String channel) {
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
      signature = signatureForced;
      isSignatureForced = false;
    }
    String messageId = hash(messageDataBase64.getBytes(), signature.getBytes());
    String[] witness = new String[0];

    messagePart.put("data", messageDataBase64);
    messagePart.put("sender", senderPk);
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
      byte[] privateKeyBytes = getPrivateKeyBytes();
      PublicKeySign publicKeySign = new Ed25519Sign(privateKeyBytes);
      byte[] signBytes = publicKeySign.sign(messageData.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(signBytes);
  }

  /** If want to test a sender that is not the organizer we can change the sender public key */
  public void setSenderPk(String newSenderPk) {
    this.senderPk = newSenderPk;
  }

  /**
   * If we want to test having a secret key that does not match the private key we can set a
   * different private key
   */
  public void setSenderSk(String newSenderSkHex) {
    this.privateKeyHex = newSenderSkHex;
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
  public static String hash(byte[]... allData) throws NoSuchAlgorithmException {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (byte[] data : allData) {
        String dataLength = Integer.toString(data.length);
        digest.update(dataLength.getBytes());
        digest.update(data);
      }
      return Base64.getUrlEncoder().encodeToString(digest.digest());
  }

  private byte[] getPrivateKeyBytes(){
    byte[] privateKeyBytes = new byte[privateKeyHex.length() / 2];

    for (int i = 0; i < privateKeyBytes.length; i++) {
      int index = i * 2;

      int val = Integer.parseInt(privateKeyHex.substring(index, index + 2), 16);
      privateKeyBytes[i] = (byte) val;
    }
    return privateKeyBytes;
  }
}
