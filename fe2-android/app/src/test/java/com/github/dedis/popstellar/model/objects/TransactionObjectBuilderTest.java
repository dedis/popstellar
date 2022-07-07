package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder;

import org.junit.Test;

import java.util.ArrayList;

public class TransactionObjectBuilderTest {
  private static final TransactionObjectBuilder builder = new TransactionObjectBuilder();

  @Test
  public void buildWithoutChannelThrowsException() {
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildWithoutInputsThrowsException() {
    builder.setChannel(Channel.fromString("/root/stuff"));
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildWithoutOutputsThrowsException() {
    builder.setChannel(Channel.fromString("/root/stuff"));
    builder.setInputs(new ArrayList<>());
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildWithoutTransactionIdThrowsException() {
    builder.setChannel(Channel.fromString("/root/stuff"));
    builder.setInputs(new ArrayList<>());
    builder.setOutputs(new ArrayList<>());
    assertThrows(IllegalStateException.class, builder::build);
  }
}
