package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

//The script describing the unlock mechanism
public class ScriptTxIn {
    @SerializedName("Type")
    private final String type; //The script describing the unlock mechanism

    //TODO object PubKey ?
    @SerializedName("PubKey")
    private final String pub_key_recipient; //The recipient’s public key

    @SerializedName("Sig")
    private final String sig; //Signature on all txins and txouts using the recipient's private key

}
