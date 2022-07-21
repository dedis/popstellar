package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;

import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.*;

public class MessageTest {
  private static final Channel CHANNEL = Channel.fromString("root/foo");
  private static final Message MESSAGE =
      new Message(CHANNEL) {
        @Override
        public String getMethod() {
          return "bar";
        }
      };

  @Test
  public void testEquals() {
    assertEquals(MESSAGE, MESSAGE);
    assertNotEquals(null, MESSAGE);

    Message message2 =
        new Message(Channel.fromString("some channel")) {
          @Override
          public String getMethod() {
            return "bar";
          }
        };
    assertNotEquals(MESSAGE, message2);
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hash(CHANNEL), MESSAGE.hashCode());
  }

  @Test
  public void constructorThrowsExceptionForNullChannel() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Message(null) {
              @Override
              public String getMethod() {
                return "bar";
              }
            });
  }
}
