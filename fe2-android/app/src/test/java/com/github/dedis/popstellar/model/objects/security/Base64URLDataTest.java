package com.github.dedis.popstellar.model.objects.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class Base64URLDataTest {

  private static final byte[] DATA_1 = new byte[] {43, 12, -65, 24};
  private static final String ENCODED_1 = "Kwy_GA==";

  private static final byte[] DATA_2 = new byte[] {45, 127, -65, 31};
  private static final String ENCODED_2 = "LX-_Hw==";

  @Test
  public void simpleDataGivesRightEncoding() {
    Base64URLData data1 = new Base64URLData(DATA_1);
    Base64URLData data2 = new Base64URLData(DATA_2);

    assertArrayEquals(DATA_1, data1.getData());
    assertEquals(ENCODED_1, data1.getEncoded());

    assertArrayEquals(DATA_2, data2.getData());
    assertEquals(ENCODED_2, data2.getEncoded());
  }

  @Test
  public void simpleEndodedGivesRightData() {
    Base64URLData data1 = new Base64URLData(ENCODED_1);
    Base64URLData data2 = new Base64URLData(ENCODED_2);

    assertArrayEquals(DATA_1, data1.getData());
    assertEquals(ENCODED_1, data1.getEncoded());

    assertArrayEquals(DATA_2, data2.getData());
    assertEquals(ENCODED_2, data2.getEncoded());
  }

  @Test
  public void equalsAndHashcodeWorksWhenSame() {
    Base64URLData data1 = new Base64URLData(DATA_1);
    Base64URLData data2 = new Base64URLData(ENCODED_1);

    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void equalsAndHashcodeWorksWhenDifferent() {
    Base64URLData data1 = new Base64URLData(DATA_1);
    Base64URLData data2 = new Base64URLData(DATA_2);

    assertNotEquals(data1, data2);
    assertNotEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void equalsSpecialCases() {
    Base64URLData data = new Base64URLData(DATA_1);
    Signature signature = new Signature(DATA_1);

    assertNotEquals(data, null);
    assertNotEquals(data, signature);
  }
}
