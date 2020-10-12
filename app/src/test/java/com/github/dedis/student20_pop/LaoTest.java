package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.Lao;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

public class LaoTest {
    private final String lao1_name = "LAO name 1";
    private final String lao2_name = "LAO name 2";
    private final Lao lao1 = new Lao(lao1_name);
    private final Lao lao2 = new Lao(lao2_name);

    @Test
    public void getNameTest() {
        Assert.assertThat(lao1.getName(), is(lao1_name));
    }

    @Test
    public void equalsTest() {
        Assert.assertEquals(lao1, lao1);
        Assert.assertNotEquals(lao1, lao2);
    }

    @Test
    public void hashCodeTest() {
        Assert.assertEquals(lao1.hashCode(), lao1.hashCode());
        Assert.assertNotEquals(lao1.hashCode(), lao2.hashCode());
    }
}
