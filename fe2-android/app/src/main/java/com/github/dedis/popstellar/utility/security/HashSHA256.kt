package com.github.dedis.popstellar.utility.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64
import timber.log.Timber

/** SHA256 Hashing Class */
object HashSHA256 {
  val TAG: String = HashSHA256::class.java.simpleName

  /**
   * Hash some objects using SHA256. Concatenate the object's string representation following the
   * protocol's directive. Then hash the obtained string
   *
   * @param strs : the strings to hash
   * @return the hashed data or null if failed to hash
   * @throws IllegalArgumentException if the data is null or empty
   * @throws UnsupportedOperationException if SHA-256 MessageDigest is unavailable
   */
  @JvmStatic
  fun hash(vararg strs: String?): String {
    require(strs.isNotEmpty()) { "cannot hash an empty/null array" }

    try {
      val digest = MessageDigest.getInstance("SHA-256")

      for (str in strs) {
        require(!str.isNullOrEmpty()) { "cannot hash an empty/null string" }

        val buf = str.toByteArray(StandardCharsets.UTF_8)
        val length = buf.size.toString()
        digest.update(length.toByteArray(StandardCharsets.UTF_8))
        digest.update(buf)
      }

      val digestBuf = digest.digest()
      return Base64.getUrlEncoder().encodeToString(digestBuf)
    } catch (e: NoSuchAlgorithmException) {
      Timber.tag(TAG).e(e, "failed to hash")
      throw UnsupportedOperationException("failed to retrieve SHA-256 instance", e)
    }
  }
}
