package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import junit.framework.TestCase
import org.junit.Assert
import java.util.Objects

class QueryTest : TestCase() {
  fun testTestEquals() {
    val query2: Query = object : Query(fromString("nonRoot/stuff"), 12) {
      override val method: String
        get() = "foo"
    }
    val query3: Query = object : Query(CHANNEL, 11) {
      override val method: String
        get() = "foo"
    }
    assertEquals(QUERY, QUERY)
    Assert.assertNotEquals(null, QUERY)
    Assert.assertNotEquals(query2, QUERY)
    Assert.assertNotEquals(query3, QUERY)
  }

  fun testTestHashCode() {
    assertEquals(Objects.hash(Objects.hash(CHANNEL), ID), QUERY.hashCode())
  }

  companion object {
    private val CHANNEL = fromString("root/stuff")
    private const val ID = 12
    private val QUERY: Query = object : Query(CHANNEL, ID) {
      override val method: String
        get() = "foo"
    }
  }
}