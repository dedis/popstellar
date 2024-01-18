package com.github.dedis.popstellar.model.qrcode

import android.net.Uri
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.utility.MessageValidator.verify

@Immutable
class PoPCHAQRCode(data: String?, laoId: String) {
  val clientId: String
  val nonce: String
  val state: String?
  val responseMode: String?
  val host: String

  init {
    verify().isValidPoPCHAUrl(data, laoId)

    val uri = Uri.parse(data)
    clientId = uri.getQueryParameter(FIELD_CLIENT_ID)!!
    nonce = uri.getQueryParameter(FIELD_NONCE)!!
    state = uri.getQueryParameter(FIELD_STATE)
    responseMode = uri.getQueryParameter(FIELD_RESPONSE_MODE)
    host = "${uri.host}${if (uri.port == -1) "" else ":${uri.port}"}"
  }

  override fun toString(): String {
    return ("PoPCHAQRCode{" +
        "clientId='" +
        clientId +
        '\'' +
        ", nonce='" +
        nonce +
        '\'' +
        ", state='" +
        state +
        '\'' +
        ", responseMode='" +
        responseMode +
        '\'' +
        ", host='" +
        host +
        '\'' +
        '}')
  }

  companion object {
    const val FIELD_CLIENT_ID = "client_id"
    const val FIELD_NONCE = "nonce"
    const val FIELD_REDIRECT_URI = "redirect_uri"
    const val FIELD_SCOPE = "scope"
    const val FIELD_STATE = "state"
    const val FIELD_RESPONSE_TYPE = "response_type"
    const val FIELD_RESPONSE_MODE = "response_mode"
    const val FIELD_LOGIN_HINT = "login_hint"
  }
}
