package com.github.dedis.popstellar.model.objects.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature

@Immutable
class ScriptInputObject
/**
 * @param type The script describing the unlock mechanism
 * @param pubKeyRecipient The recipient’s public key
 * @param sig Signature on all txins and txouts using the recipient's private key
 */
(val type: String, val pubKey: PublicKey, val sig: Signature)
