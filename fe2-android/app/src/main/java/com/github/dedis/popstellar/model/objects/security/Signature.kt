package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable

/**
 * Represents the signature of some date.
 *
 * It provides authenticity and integrity of the signed data
 */
@Immutable
class Signature : Base64URLData {
  constructor(data: ByteArray) : super(data)

  constructor(data: String) : super(data)
}
