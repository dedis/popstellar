package com.github.dedis.popstellar.model.objects.security

import org.junit.Assert
import org.junit.Test

class Base64URLDataTest {
  @Test
  fun simpleDataGivesRightEncoding() {
    val data1 = Base64URLData(DATA_1)
    val data2 = Base64URLData(DATA_2)

    Assert.assertArrayEquals(DATA_1, data1.data)
    Assert.assertEquals(ENCODED_1, data1.encoded)
    Assert.assertArrayEquals(DATA_2, data2.data)
    Assert.assertEquals(ENCODED_2, data2.encoded)
  }

  @Test
  fun simpleEndodedGivesRightData() {
    val data1 = Base64URLData(ENCODED_1)
    val data2 = Base64URLData(ENCODED_2)

    Assert.assertArrayEquals(DATA_1, data1.data)
    Assert.assertEquals(ENCODED_1, data1.encoded)
    Assert.assertArrayEquals(DATA_2, data2.data)
    Assert.assertEquals(ENCODED_2, data2.encoded)
  }

  @Test
  fun equalsAndHashcodeWorksWhenSame() {
    val data1 = Base64URLData(DATA_1)
    val data2 = Base64URLData(ENCODED_1)

    Assert.assertEquals(data1, data2)
    Assert.assertEquals(data1.hashCode().toLong(), data2.hashCode().toLong())
  }

  @Test
  fun equalsAndHashcodeWorksWhenDifferent() {
    val data1 = Base64URLData(DATA_1)
    val data2 = Base64URLData(DATA_2)

    Assert.assertNotEquals(data1, data2)
    Assert.assertNotEquals(data1.hashCode().toLong(), data2.hashCode().toLong())
  }

  @Test
  fun equalsSpecialCases() {
    val data = Base64URLData(DATA_1)
    val signature = Signature(DATA_1)

    Assert.assertNotEquals(data, null)
    Assert.assertNotEquals(data, signature)
  }

  @Test
  fun toStringShowsExpectedValue() {
    val data = Base64URLData(DATA_1)
    val signature = Signature(DATA_1)
    Assert.assertEquals("Base64URLData($ENCODED_1)", data.toString())
    Assert.assertEquals("Signature($ENCODED_1)", signature.toString())
  }

  companion object {
    private val DATA_1 = byteArrayOf(43, 12, -65, 24)
    private const val ENCODED_1 = "Kwy_GA=="
    private val DATA_2 = byteArrayOf(45, 127, -65, 31)
    private const val ENCODED_2 = "LX-_Hw=="
  }
}
