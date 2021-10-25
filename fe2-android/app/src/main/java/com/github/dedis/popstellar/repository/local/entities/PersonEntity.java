package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*
Person represents an actual person in the system who holds a public key.
*/
@Entity
public class PersonEntity {

  @PrimaryKey @NonNull public String publicKey;
}
