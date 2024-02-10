package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R
import java.security.GeneralSecurityException

class KeyGenerationException(e: GeneralSecurityException?) :
    KeyException("Could not generate key", e) {
  override val userMessage: Int
    get() = R.string.key_generation_exception

  override val userMessageArguments: Array<Any?>
    get() = arrayOfNulls(0)
}
