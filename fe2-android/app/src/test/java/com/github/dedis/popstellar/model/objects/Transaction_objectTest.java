package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class Transaction_objectTest {

  @Test
  public void createTransactionWithNull() {
    assertThrows(IllegalArgumentException.class, () -> new Transaction_object());
  }
}
