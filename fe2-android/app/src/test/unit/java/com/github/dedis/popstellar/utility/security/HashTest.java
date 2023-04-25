package com.github.dedis.popstellar.utility.security;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HashTest {

  @Test
  public void hashNotNullTest() {
    assertNotNull(Hash.hash("Data to hash"));
  }

  @Test
  public void hashNullTest() {
    Assert.assertThrows(IllegalArgumentException.class, Hash::hash);
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash((String) null));
    Assert.assertThrows(
        IllegalArgumentException.class, () -> Hash.hash((String) null, (String) null));
  }

  @Test
  public void hashEmptyStringTest() {
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash(""));
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash("", ""));
  }

  @Test
  public void hashUTF8Test() {
    String expected = "bkkql8ZyOdbqrWY1QJHPGiz29zNMOEtaXXBHK1aWgjY=";
    assertEquals(expected, Hash.hash("你们是真的", "好学生！"));
  }

  @Test
  public void hashEmojiTest() {
    String expected = "8BMmJjQMPhtD0QwVor1uVB3B_PyMMyIbIvaDHcOQnTg=";
    assertEquals(expected, Hash.hash("test \uD83D\uDE00"));
  }

  @Test
  public void hashPureEmojiTest() {
    String expected = "ht7cQAkPdd6o-ZFVW6gTbt0gEIEUcr5FTDgOaeW8BOU=";
    assertEquals(expected, Hash.hash("🫡"));
  }

  @Test
  public void hashMixEmojiTest() {
    String expected = "wANKJFj9q_ncRKalYmK4yozUpet33JaFXVQEpMcHdfU=";
    assertEquals(
        expected, Hash.hash("text \uD83E\uDD70", "\uD83C\uDFC9", "more text\uD83C\uDF83️", "♠️"));
  }
}
