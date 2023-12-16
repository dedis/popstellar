package com.github.dedis.popstellar.model.objects.digitalcash

import com.github.dedis.popstellar.model.Immutable

@Immutable
class ScriptOutputObject
/**
 * @param type Type of script
 * @param pubKeyHash Hash of the recipient’s public key
 */(// Type of script
        @JvmField val type: String, // a function that given a public key verify it is the goof public key
        // Hash of the recipient’s public key
        @JvmField val pubKeyHash: String)