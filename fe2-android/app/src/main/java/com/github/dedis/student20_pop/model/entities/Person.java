package com.github.dedis.student20_pop.model.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*
Person represents an actual person in the system who holds a public key.
*/
@Entity
public class Person {

  @PrimaryKey @NonNull public String publicKey;
}
