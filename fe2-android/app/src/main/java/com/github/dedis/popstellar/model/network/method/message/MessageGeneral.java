package com.github.dedis.popstellar.model.network.method.message;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.utility.security.Hash;
import com.google.android.gms.common.util.Hex;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Container of a high level message.
 *
 * <p>It is encapsulated inside low level messages
 */
public final class MessageGeneral {

  private final String TAG = MessageGeneral.class.getSimpleName();

  private final byte[] sender;

  private final byte[] dataBuf;

  private Data data;

  private byte[] signature;

  private byte[] messageId;

  private List<PublicKeySignaturePair> witnessSignatures = new ArrayList<>();

  private PublicKeyVerify verifier;

  public MessageGeneral(byte[] sender, Data data, PublicKeySign signer, Gson gson) {
    this.sender = sender;
    this.data = data;
    Log.d(TAG, gson.toJson(data, Data.class));
    this.dataBuf = gson.toJson(data, Data.class).getBytes();
    this.verifier = new Ed25519Verify(sender);

    generateSignature(signer);
    generateId();
  }

  public MessageGeneral(
      byte[] sender,
      Data data,
      List<PublicKeySignaturePair> witnessSignatures,
      PublicKeySign signer,
      Gson gson) {
    this(sender, data, signer, gson);
    this.witnessSignatures = witnessSignatures;
  }

  public MessageGeneral(
      byte[] sender,
      byte[] dataBuf,
      Data data,
      byte[] signature,
      byte[] messageId,
      List<PublicKeySignaturePair> witnessSignatures) {

    byte[] decodedMessageId = Base64.getUrlDecoder().decode(messageId);

    Log.d(
        TAG,
        "new MessageGeneral with messageId encoded as: "
            + new String(messageId, StandardCharsets.UTF_8)
            + " decoded as: "
            + Hex.bytesToStringUppercase(decodedMessageId));

    this.sender = sender;
    this.messageId = messageId;
    this.dataBuf = dataBuf;
    this.signature = signature;
    this.witnessSignatures = witnessSignatures;
    this.data = data;
    this.verifier = new Ed25519Verify(sender);
  }

  private void generateSignature(PublicKeySign signer) {
    try {
      this.signature = signer.sign(this.dataBuf);
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to generate signature", e);
    }
  }

  private void generateId() {
    this.messageId =
        Hash.hash(
                Base64.getUrlEncoder().encodeToString(this.dataBuf),
                Base64.getUrlEncoder().encodeToString(this.signature))
            .getBytes(StandardCharsets.UTF_8);
  }

  public String getMessageId() {
    return new String(this.messageId, StandardCharsets.UTF_8);
  }

  public String getSender() {
    return Base64.getUrlEncoder().encodeToString(this.sender);
  }

  public String getSignature() {
    return Base64.getUrlEncoder().encodeToString(this.signature);
  }

  public List<PublicKeySignaturePair> getWitnessSignatures() {
    return this.witnessSignatures;
  }

  public Data getData() {
    return data;
  }

  public String getDataEncoded() {
    return Base64.getUrlEncoder().encodeToString(this.dataBuf);
  }

  public boolean verify() {
    try {
      verifier.verify(signature, dataBuf);

      if (data instanceof WitnessMessageSignature) {
        WitnessMessageSignature witness = (WitnessMessageSignature) data;

        byte[] signatureBuf = Base64.getUrlDecoder().decode(witness.getSignature());
        byte[] messageIdBuf = Base64.getUrlDecoder().decode(witness.getMessageId());

        verifier.verify(signatureBuf, messageIdBuf);
      }

      return true;
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to verify signature", e);
      return false;
    }
  }

  @Override
  public String toString() {
    return "MessageGeneral{"
        + "sender="
        + getSender()
        + ", data="
        + getData()
        + ", signature="
        + getSignature()
        + ", messageId="
        + getMessageId()
        + ", witnessSignatures="
        + Arrays.toString(witnessSignatures.toArray())
        + ", verifier="
        + verifier
        + '}';
  }
}
