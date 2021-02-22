package com.github.dedis.student20_pop;

import org.junit.Test;

import io.reactivex.Flowable;

public class RxTest {

    @Test
    public void testLongEvents() {
        Flowable.just(1, 2, 3, 4, 5)
                .subscribe(x -> {
                    if (x == 2) {
                        Thread.sleep(10000);
                    }

                    System.out.println(x);
                });
    }
}
