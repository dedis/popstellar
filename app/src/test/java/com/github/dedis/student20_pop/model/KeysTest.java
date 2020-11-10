package com.github.dedis.student20_pop.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
public class KeysTest {

    Keys keyPair1 = new Keys();
    Keys keyPair2 = new Keys();

    @Test
    public void keysAreDisplayedCorrectly(){
        System.out.println(keyPair1.getPublicKey());
        System.out.println(keyPair2.getPrivateKey());
    }

    @Test
    public void doesNotCreateTheSameKeyPair(){
        assertNotEquals(keyPair1.getPrivateKey(), keyPair2.getPrivateKey());
        assertNotEquals(keyPair1.getPublicKey(), keyPair2.getPublicKey());
    }

    @Test
    public void equalsTest(){
        assertEquals(keyPair1, keyPair1);
        assertNotEquals(keyPair1, keyPair2);
    }

    @Test
    public void hashCodeTest(){
        assertEquals(keyPair1.hashCode(), keyPair1.hashCode());
        assertNotEquals(keyPair1.hashCode(), keyPair2.hashCode());
    }

}