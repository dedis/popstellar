package com.github.dedis.popstellar.model.network.method.message;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.security.*;
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Container of a high level message.
 *
 * <p>It is encapsulated inside low level messages
 */
@Immutable
public final class MessageGeneral {

  private static final Logger logger = LogManager.getLogger(Message.class);

  private final PublicKey sender;
  private final Base64URLData dataBuf;
  private final Data data;
  private final MessageID messageId;

  private Signature signature;
  private List<PublicKeySignaturePair> witnessSignatures = new ArrayList<>();

  public MessageGeneral(
      PublicKey sender,
      Base64URLData dataBuf,
      Data data,
      Signature signature,
      MessageID messageID,
      List<PublicKeySignaturePair> witnessSignatures) {
    this.sender = sender;
    this.dataBuf = dataBuf;
    this.data = data;
    this.messageId = messageID;
    this.signature = signature;
    this.witnessSignatures = new ArrayList<>(witnessSignatures);
  }

  public MessageGeneral(KeyPair keyPair, Data data, Gson gson) {
    sender = keyPair.getPublicKey();
    this.data = data;
    String dataJson = gson.toJson(data, Data.class);

    logger.debug(dataJson);
    dataBuf = new Base64URLData(dataJson.getBytes(StandardCharsets.UTF_8));

    generateSignature(keyPair.getPrivateKey());
    messageId = new MessageID(dataBuf, signature);
  }

  public MessageGeneral(
      KeyPair keyPair, Data data, List<PublicKeySignaturePair> witnessSignatures, Gson gson) {
    this(keyPair, data, gson);
    this.witnessSignatures = witnessSignatures;
  }

  private void generateSignature(PrivateKey signer) {
    try {
      signature = signer.sign(dataBuf);
    } catch (GeneralSecurityException e) {
      logger.debug("failed to generate signature", e);
    }
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public PublicKey getSender() {
    return sender;
  }

  public Signature getSignature() {
    return signature;
  }

  public List<PublicKeySignaturePair> getWitnessSignatures() {
    return new ArrayList<>(witnessSignatures);
  }

  public Data getData() {
    return data;
  }

  public Base64URLData getDataEncoded() {
    return dataBuf;
  }

  public boolean verify() {
    if (!sender.verify(signature, dataBuf)) {
      return false;
    }

    if (data instanceof WitnessMessageSignature) {
      WitnessMessageSignature witness = (WitnessMessageSignature) data;

      Signature witnessSignature = witness.getSignature();
      MessageID messageID = witness.getMessageId();

      return sender.verify(witnessSignature, messageID);
    }

    return true;
  }

  @NonNull
  @Override
  public String toString() {
    return "MessageGeneral{"
        + "sender='"
        + getSender()
        + '\''
        + ", data='"
        + getData()
        + "', signature='"
        + getSignature()
        + '\''
        + ", messageId='"
        + getMessageId()
        + '\''
        + ", witnessSignatures='"
        + Arrays.toString(witnessSignatures.toArray())
        + "'}";
  }
}
