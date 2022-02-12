package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets
import java.util.Base64

sealed case class Base64Data(data: String) {
  val DECODER: Base64.Decoder = Base64.getUrlDecoder

  def decodeToString(): String = new String(this.decode(), StandardCharsets.UTF_8)

  def decode(): Array[Byte] = DECODER.decode(this.getBytes)

  def getBytes: Array[Byte] = data.getBytes(StandardCharsets.UTF_8)

  def equals(that: Base64Data): Boolean = data == that.data

  override def toString: String = data.toString

  try {
    DECODER.decode(data)
  } catch {
    case _: IllegalArgumentException => throw new IllegalArgumentException(s"String $data is not Base64")
  }
}

object Base64Data {
  val ENCODER: Base64.Encoder = Base64.getUrlEncoder

  def encode(data: String): Base64Data = Base64Data(ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8)))

  def encode(data: Array[Byte]): Base64Data = Base64Data(ENCODER.encodeToString(data))
}
