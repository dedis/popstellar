package com.github.dedis.popstellar.model.network.method.message;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.security.*;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Container of a high level message.
 *
 * <p>It is encapsulated inside low level messages
 */
public final class MessageGeneral {

  private static final String TAG = MessageGeneral.class.getSimpleName();

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
    this.witnessSignatures = witnessSignatures;
  }

  public MessageGeneral(KeyPair keyPair, Data data, Gson gson) {
    this.sender = keyPair.getPublicKey();
    this.data = data;
    String dataJson = gson.toJson(data, Data.class);
    Log.d(TAG, dataJson);
    this.dataBuf = new Base64URLData(dataJson.getBytes(StandardCharsets.UTF_8));

    generateSignature(keyPair.getPrivateKey());
    this.messageId = new MessageID(this.dataBuf, this.signature);
  }

  public MessageGeneral(
      KeyPair keyPair, Data data, List<PublicKeySignaturePair> witnessSignatures, Gson gson) {
    this(keyPair, data, gson);
    this.witnessSignatures = witnessSignatures;
  }

  private void generateSignature(PrivateKey signer) {
    try {
      this.signature = signer.sign(this.dataBuf);
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to generate signature", e);
    }
  }

  public MessageID getMessageId() {
    return this.messageId;
  }

  public PublicKey getSender() {
    return this.sender;
  }

  public Signature getSignature() {
    return this.signature;
  }

  public List<PublicKeySignaturePair> getWitnessSignatures() {
    return this.witnessSignatures;
  }

  public Data getData() {
    return this.data;
  }

  public Base64URLData getDataEncoded() {
    return this.dataBuf;
  }

  public boolean verify() {
    if (!this.sender.verify(this.signature, this.dataBuf)) return false;

    if (this.data instanceof WitnessMessageSignature) {
      WitnessMessageSignature witness = (WitnessMessageSignature) this.data;

      Signature witnessSignature = witness.getSignature();
      MessageID messageID = witness.getMessageId();

      return this.sender.verify(witnessSignature, messageID);
    } else {
      return true;
    }
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
