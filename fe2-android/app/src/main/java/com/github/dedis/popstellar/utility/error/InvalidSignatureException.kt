package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.security.Signature

class InvalidSignatureException(data: Data, signature: Signature) :
    InvalidDataException(data, "signature", signature.encoded)
