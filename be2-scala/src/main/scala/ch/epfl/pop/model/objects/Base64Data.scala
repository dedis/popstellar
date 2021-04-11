package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

case class Base64Data(data: String) {
  val DECODER: Base64.Decoder = Base64.getDecoder

  def decode(): String = DECODER.decode(data.getBytes(UTF_8)).map(_.toChar).mkString
}

object Base64Data {
  val ENCODER: Base64.Encoder = Base64.getEncoder

  def encode(data: String): Base64Data = Base64Data(ENCODER.encode(data.getBytes(UTF_8)).map(_.toChar).mkString)
}
