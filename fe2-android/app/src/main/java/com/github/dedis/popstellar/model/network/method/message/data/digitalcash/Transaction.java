package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//the transaction object
public class Transaction {
    @SerializedName(value="Version")
    private final int version; //The version of the transaction inputs

    @SerializedName(value="TxIn")
    private final List<TxIn> TxIns; //[Array[Objects]] array of output transactions to use as inputs

    @SerializedName(value="TxOut")
    private final List<TxOut> TxOuts; //[Array[Objects]] array of outputs from this transactions

    @SerializedName("LockTime")
    private final long timestamp; //TimeStamp

}
