package ch.epfl.pop.json

object MsgField extends Enumeration {
  type MsgField = Value
  val MESSAGE_ID, SENDER, SIGNATURE, DATA, WITNESS_SIGNATURES = Value
}
