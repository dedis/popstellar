package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

  /**
   * @param version The version of the transaction inputs
   * @param TxIns [Array[Objects]] array of output transactions to use as inputs
   * @param TxOuts [Array[Objects]] array of outputs from this transactions
   * @param timestamp TimeStamp
   */
  public Transaction(int version, List<TxIn> TxIns, List<TxOut> TxOuts, long timestamp) {
        this.version = version;
        //TODO : SAFE LIST COPY
        this.TxIns = TxIns;
        this.TxOuts = TxOuts;
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public List<TxIn> getTxIns() {
        //TODO: Safe Return List
        return TxIns;
    }

    public List<TxOut> getTxOuts() {
        //TODO: Safe Return List
        return TxOuts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        //TODO : Problem by check equality of two list ?
        return version == that.version && timestamp == that.timestamp && TxIns.equals(that.TxIns) && TxOuts.equals(that.TxOuts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, TxIns, TxOuts, timestamp);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "version=" + version + '\'' +
                ", TxIns=" + Arrays.toString(TxIns.toArray()) + '\'' +
                ", TxOuts=" + Arrays.toString(TxOuts.toArray()) + '\''+
                ", timestamp=" + timestamp +
                '}';
    }
}
