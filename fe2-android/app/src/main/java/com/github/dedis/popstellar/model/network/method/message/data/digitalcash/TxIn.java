package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

// "Object representing a transaction output to use as an input for this transaction"
public class TxIn {
    @SerializedName("TxOutHash")
    private final String txOutHash; //Previous (to-be-used) transaction hash

    @SerializedName("TxOutIndex")
    private final int txOutIndex; //index of the previous to-be-used transaction

    @SerializedName("Script")
    private final ScriptTxIn script; //The script describing the unlock mechanism
}
