package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.method.Message;
import io.reactivex.Flowable;

public class LAORemoteDataSource implements LAODataSource.Remote {
  private static LAORemoteDataSource INSTANCE;

  private LAOService laoService;

  public static LAORemoteDataSource getInstance(LAOService laoService) {
    if (INSTANCE == null) {
      INSTANCE = new LAORemoteDataSource(laoService);
    }
    return INSTANCE;
  }

  private LAORemoteDataSource(LAOService service) {
    this.laoService = service;
  }

  public Flowable<GenericMessage> observeMessage() {
    return laoService.observeMessage();
  }

  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }
}
