package com.github.dedis.popstellar.model.objects.view;

import com.github.dedis.popstellar.model.objects.Lao;

public final class LaoView {

  private final Lao lao;

  public LaoView(Lao lao) {
    if (lao == null) {
      throw new IllegalArgumentException();
    }
    this.lao = lao;
  }
}
