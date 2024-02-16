package com.github.dedis.popstellar.utility.security

import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test

class HashSHA256Test {
  @Test
  fun hashNotNullTest() {
    Assert.assertNotNull(hash("Data to hash"))
  }

  @Test
  fun hashNullTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { hash() }
    Assert.assertThrows(IllegalArgumentException::class.java) { hash(null as String?) }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      hash(null as String?, null as String?)
    }
  }

  @Test
  fun hashEmptyStringTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { hash("") }
    Assert.assertThrows(IllegalArgumentException::class.java) { hash("", "") }
  }

  @Test
  fun hashUTF8Test() {
    val expected = "bkkql8ZyOdbqrWY1QJHPGiz29zNMOEtaXXBHK1aWgjY="
    Assert.assertEquals(expected, hash("你们是真的", "好学生！"))
  }

  @Test
  fun hashEmojiTest() {
    val expected = "8BMmJjQMPhtD0QwVor1uVB3B_PyMMyIbIvaDHcOQnTg="
    Assert.assertEquals(expected, hash("test \uD83D\uDE00"))
  }

  @Test
  fun hashPureEmojiTest() {
    val expected = "ht7cQAkPdd6o-ZFVW6gTbt0gEIEUcr5FTDgOaeW8BOU="
    Assert.assertEquals(expected, hash("🫡"))
  }

  @Test
  fun hashMixEmojiTest() {
    val expected = "wANKJFj9q_ncRKalYmK4yozUpet33JaFXVQEpMcHdfU="
    Assert.assertEquals(
      expected,
      hash("text \uD83E\uDD70", "\uD83C\uDFC9", "more text\uD83C\uDF83️", "♠️")
    )
  }
}
