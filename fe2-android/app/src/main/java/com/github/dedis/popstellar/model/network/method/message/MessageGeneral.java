package com.github.dedis.popstellar.model.network.method.message;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PrivateKey;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Container of a high level message.
 *
 * <p>It is encapsulated inside low level messages
 */
public final class MessageGeneral {

  private final String TAG = MessageGeneral.class.getSimpleName();

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
    Log.d(TAG, gson.toJson(data, Data.class));
    this.dataBuf = new Base64URLData(gson.toJson(data, Data.class).getBytes());

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
    return sender;
  }

  public Signature getSignature() {
    return signature;
  }

  public List<PublicKeySignaturePair> getWitnessSignatures() {
    return this.witnessSignatures;
  }

  public Data getData() {
    return data;
  }

  public Base64URLData getDataEncoded() {
    return dataBuf;
  }

  public boolean verify() {
    if (!this.sender.verify(signature, dataBuf)) return false;

    if (data instanceof WitnessMessageSignature) {
      WitnessMessageSignature witness = (WitnessMessageSignature) data;

      Signature signature = new Signature(witness.getSignature());
      MessageID messageID = new MessageID(witness.getMessageId());

      return sender.verify(signature, messageID);
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "MessageGeneral{"
        + "sender="
        + getSender()
        + '\''
        + ", data="
        + getData()
        + ", signature='"
        + getSignature()
        + '\''
        + ", messageId='"
        + getMessageId()
        + '\''
        + ", witnessSignatures="
        + Arrays.toString(witnessSignatures.toArray())
        + '}';
  }
}
