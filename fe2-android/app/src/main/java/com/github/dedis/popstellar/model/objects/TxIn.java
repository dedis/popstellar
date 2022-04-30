package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptTxIn;

public class TxIn {
  private final String txOutHash;
  private final int txOutIndex;
  private final ScriptTxIn script;
}
