package com.github.dedis.student20_pop;
import com.github.dedis.student20_pop.model.Keys;

import org.junit.Test;

import java.security.GeneralSecurityException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class KeysTest {

    Keys keyPair1 = new Keys();
    Keys keyPair2 = new Keys();


    @Test
    public void getMethodsReturnInTheCorrectFormat(){
        System.out.print(keyPair1.getPrivateKey());
        System.out.print(keyPair1.getPublicKey());
    }

    @Test
    public void doesNotCreateTheSameKeypairTwice(){
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
