package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.network.GenericMessage;

import io.reactivex.Flowable;

public interface LAODataSource {

    Flowable<GenericMessage> observeMessage();

}
