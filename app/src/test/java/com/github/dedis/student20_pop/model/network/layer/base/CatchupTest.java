package com.github.dedis.student20_pop.model.network.layer.base;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CatchupTest {

    private static final Catchup catchup = new Catchup("channel", 0);

    @Test
    public void getRequestIdTest() {
        assertThat(catchup.getRequestId(), is(0));
    }

    @Test
    public void getMethodTest() {
        assertThat(catchup.getMethod(), is(Method.CATCHUP.getMethod()));
    }

    @Test
    public void equalsTest() {
        assertEquals(catchup, catchup);
        assertNotEquals(catchup, new Catchup("channel", 1));
    }

    @Test
    public void hashCodeTest() {
        assertEquals(catchup.hashCode(), catchup.hashCode());
        assertNotEquals(catchup.hashCode(), (new Catchup("channel", 2)).hashCode());
    }
}
