package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.network.serializer.base64.JsonBase64DataSerializer
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.GsonBuilder
import org.junit.Assert
import org.junit.Test

class MainPublicKeyDataTest {
  private val gson =
    GsonBuilder()
      .registerTypeAdapter(
        PublicKey::class.java,
        JsonBase64DataSerializer { data: String -> PublicKey(data) }
      )
      .create()
  private val pk = Base64DataUtils.generatePublicKey()

  @Test
  fun constructorAndGetterAreCoherent() {
    val data = MainPublicKeyData(pk)
    Assert.assertEquals(pk, data.publicKey)
  }

  @Test
  fun extractDataTest() {
    val jsonFormat = gson.toJson(MainPublicKeyData(pk))
    Assert.assertEquals(pk, MainPublicKeyData.extractFrom(gson, jsonFormat).publicKey)
  }
}
