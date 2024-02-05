package com.github.dedis.popstellar.model.network

import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.verifyJson
import com.google.gson.JsonParseException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Objects
import java.util.stream.Collectors
import org.junit.Assert

object JsonTestUtils {
  val GSON = provideGson(buildRegistry())

  @JvmStatic
  fun loadFile(path: String?): String {
    val `is` =
      Objects.requireNonNull(JsonTestUtils::class.java.classLoader).getResourceAsStream(path)
    val reader = BufferedReader(InputStreamReader(`is`))
    return reader.lines().collect(Collectors.joining("\n"))
  }

  /**
   * Convert the given data to a json String, convert it back to a Data and compare them.
   *
   * @param data the Data to test
   * @throws JsonParseException if it failed to parse
   * @throws AssertionError if the converted data is not equals to data
   */
  @JvmStatic
  @Throws(JsonParseException::class)
  fun testData(data: Data?) {
    val json = GSON.toJson(data, Data::class.java)
    verifyJson(JsonUtils.DATA_SCHEMA, json)
    Assert.assertEquals(data, GSON.fromJson(json, Data::class.java))
  }

  @JvmStatic
  @Throws(JsonParseException::class)
  fun parse(json: String?): Data {
    return GSON.fromJson(json, Data::class.java)
  }
}
