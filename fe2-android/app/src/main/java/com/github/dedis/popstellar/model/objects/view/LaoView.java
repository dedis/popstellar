package com.github.dedis.popstellar.model.objects.view;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

public final class LaoView {

  private final Lao lao;

  /**
   * This class offers useful getters for Lao state to handlers and prevents changing its state. It
   * is provided as an intermediate step towards functional handling of Objects. To change the state
   * of an Lao, one can use createLaoCopy() which returns a copy of the wrapped Lao, and update the
   * repository with said updated LAO.
   *
   * @param lao the Lao to be wrapped
   */
  public LaoView(Lao lao) {
    if (lao == null) {
      throw new IllegalArgumentException();
    }
    this.lao = new Lao(lao);
  }

  public Lao createLaoCopy() {
    return new Lao(lao);
  }

  public long getLastModified() {
    return lao.getLastModified();
  }

  public String getName() {
    return lao.getName();
  }

  public String getId() {
    return lao.getId();
  }

  public boolean isOrganizer(PublicKey publicKey) {
    return lao.getOrganizer().equals(publicKey);
  }

  public Channel getChannel() {
    return lao.getChannel();
  }

  public PublicKey getOrganizer() {
    return lao.getOrganizer();
  }

  public long getCreation() {
    return lao.getCreation();
  }

  @NonNull
  @Override
  public String toString() {
    return lao.toString();
  }
}
