package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;

import junit.framework.TestCase;

import java.util.Objects;

import static org.junit.Assert.assertNotEquals;

public class QueryTest extends TestCase {
  private static final Channel CHANNEL = Channel.fromString("root/stuff");
  private static final int ID = 12;
  private static final Query QUERY =
      new Query(CHANNEL, ID) {
        @Override
        public String getMethod() {
          return "foo";
        }
      };

  public void testTestEquals() {
    Query query2 =
        new Query(Channel.fromString("nonRoot/stuff"), 12) {
          @Override
          public String getMethod() {
            return "foo";
          }
        };
    Query query3 =
        new Query(CHANNEL, 11) {
          @Override
          public String getMethod() {
            return "foo";
          }
        };
    assertEquals(QUERY, QUERY);
    assertNotEquals(null, QUERY);
    assertNotEquals(query2, QUERY);
    assertNotEquals(query3, QUERY);
  }

  public void testTestHashCode() {
    assertEquals(Objects.hash(Objects.hash(CHANNEL), ID), QUERY.hashCode());
  }
}
