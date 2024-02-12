package com.github.dedis.popstellar.utility.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

public class HashSHA256Test {

  @Test
  public void hashNotNullTest() {
    assertNotNull(HashSHA256.hash("Data to hash"));
  }

  @Test
  public void hashNullTest() {
    Assert.assertThrows(IllegalArgumentException.class, HashSHA256::hash);
    Assert.assertThrows(IllegalArgumentException.class, () -> HashSHA256.hash((String) null));
    Assert.assertThrows(
        IllegalArgumentException.class, () -> HashSHA256.hash((String) null, (String) null));
  }

  @Test
  public void hashEmptyStringTest() {
    Assert.assertThrows(IllegalArgumentException.class, () -> HashSHA256.hash(""));
    Assert.assertThrows(IllegalArgumentException.class, () -> HashSHA256.hash("", ""));
  }

  @Test
  public void hashUTF8Test() {
    String expected = "bkkql8ZyOdbqrWY1QJHPGiz29zNMOEtaXXBHK1aWgjY=";
    assertEquals(expected, HashSHA256.hash("你们是真的", "好学生！"));
  }

  @Test
  public void hashEmojiTest() {
    String expected = "8BMmJjQMPhtD0QwVor1uVB3B_PyMMyIbIvaDHcOQnTg=";
    assertEquals(expected, HashSHA256.hash("test \uD83D\uDE00"));
  }

  @Test
  public void hashPureEmojiTest() {
    String expected = "ht7cQAkPdd6o-ZFVW6gTbt0gEIEUcr5FTDgOaeW8BOU=";
    assertEquals(expected, HashSHA256.hash("🫡"));
  }

  @Test
  public void hashMixEmojiTest() {
    String expected = "wANKJFj9q_ncRKalYmK4yozUpet33JaFXVQEpMcHdfU=";
    assertEquals(
        expected,
        HashSHA256.hash("text \uD83E\uDD70", "\uD83C\uDFC9", "more text\uD83C\uDF83️", "♠️"));
  }
}
