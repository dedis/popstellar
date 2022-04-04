package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

//Object representing an output of this transaction
public class TxOut {
    @SerializedName("Value")
    private final int value; //the value of the output transaction, expressed in miniLAOs

    @SerializedName("Script")
    private final ScriptTxOut script; //The script describing the TxOut unlock mechanism

}
