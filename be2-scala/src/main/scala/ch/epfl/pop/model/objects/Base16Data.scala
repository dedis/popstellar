package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets

final case class Base16Data(data: String) {

  /** Decode the hexadecimal data to a string using UTF-8 charset. */
  def decodeToString(): String = new String(this.decode(), StandardCharsets.UTF_8)

  /** Decode the hexadecimal string to a byte array. */
  def decode(): Array[Byte] = Base16Data.hexStringToByteArray(data)

  /** Get the byte array of the original string data. */
  def getBytes: Array[Byte] = data.getBytes(StandardCharsets.UTF_8)

  override def equals(that: Any): Boolean = that match {
    case that: Base16Data => this.data.equalsIgnoreCase(that.data)
    case _                => false
  }

  /** Returns the string representation of the hexadecimal data. */
  override def toString: String = data
}

object Base16Data {

  /** Convert a byte array to a hexadecimal string. */
  def byteArrayToHexString(bytes: Array[Byte]): String = {
    bytes.map("%02x".format(_)).mkString
  }

  /** Convert a string to hexadecimal. */
  def stringToHexString(s: String): String = byteArrayToHexString(s.getBytes(StandardCharsets.UTF_8))

  /** Create a Base16Data instance from a string, ensuring it represents exactly 32 bytes when decoded. */
  def encode(data: String): Base16Data = {
    val hexString = stringToHexString(data)
    Base16Data(hexString)
  }

  /** Create a Base16Data instance from a byte array, ensuring it is exactly 32 bytes long. */
  def encode(data: Array[Byte]): Base16Data = {
    Base16Data(byteArrayToHexString(data))
  }

  /** Helper method to convert a hexadecimal string into a byte array. */
  def hexStringToByteArray(hex: String): Array[Byte] = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }
}
