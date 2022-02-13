package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.KeyPair;

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;

public interface MessageSender extends Disposable {

  Completable catchup(String channel);

  Completable publish(KeyPair keyPair, String channel, Data data);

  Completable subscribe(String channel);

  Completable unsubscribe(String channel);
}
