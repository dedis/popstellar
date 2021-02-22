package com.github.dedis.student20_pop.model.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ModificationSignature {

    @PrimaryKey
    @NonNull
    public String signature;

    public String witnessPublicKey;

    @NonNull
    @ColumnInfo(index = true)
    public String identifier;
}
