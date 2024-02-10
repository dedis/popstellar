package com.github.dedis.popstellar.model.objects.view

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.PublicKey

class LaoView(lao: Lao?) {
  private val lao: Lao

  /**
   * This class offers useful getters for Lao state to handlers and prevents changing its state. It
   * is provided as an intermediate step towards functional handling of Objects. To change the state
   * of an Lao, one can use createLaoCopy() which returns a copy of the wrapped Lao, and update the
   * repository with said updated LAO.
   *
   * @param lao the Lao to be wrapped
   */
  init {
    requireNotNull(lao)

    this.lao = Lao(lao)
  }

  fun createLaoCopy(): Lao {
    return Lao(lao)
  }

  val lastModified: Long
    get() = lao.lastModified

  val name: String?
    get() = lao.name

  val id: String
    get() = lao.id

  fun isOrganizer(publicKey: PublicKey?): Boolean {
    return organizer == publicKey
  }

  val channel: Channel
    get() = lao.channel

  val organizer: PublicKey
    get() = lao.organizer ?: PublicKey("")

  val creation: Long
    get() = lao.creation

  override fun toString(): String {
    return lao.toString()
  }
}
