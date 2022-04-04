package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

// The script describing the TxOut unlock mechanism
public class ScriptTxOut {
    @SerializedName("Type")
    private final String type; //Type of script

    @SerializedName("PubkeyHash")
    private final String pub_key_hash; //Hash of the recipient’s public key
}
