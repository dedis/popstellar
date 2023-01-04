package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

@Immutable
public class OutputObject {

  private final long value;
  private final ScriptOutputObject script;

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public OutputObject(long value, ScriptOutputObject script) {
    this.value = value;
    this.script = script;
  }

  public long getValue() {
    return value;
  }

  public ScriptOutputObject getScript() {
    return script;
  }

  public String getPubKeyHash() {
    return script.getPubKeyHash();
  }

  public boolean isUserOutputRecipient(PublicKey user) {
    return script.getPubKeyHash().equals(user.computeHash());
  }

  @NonNull
  @Override
  public String toString() {
    return "OutputObject{" + "value=" + value + ", keyHash=" + getPubKeyHash() + '}';
  }
}
