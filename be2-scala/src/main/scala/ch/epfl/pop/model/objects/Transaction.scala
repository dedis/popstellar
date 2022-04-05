package ch.epfl.pop.model.objects

import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.Parsable
import spray.json._

final case class LockScript(
  Type: String,
  PubkeyHash: Address,
)

final case class TxOut(
  Value: Int,
  Script: LockScript,
)

final case class UnlockScript(
  Type: String,
  Pubkey: PublicKey,
  Sig: Base64Data,
)

final case class TxIn(
  TxOutHash: Hash,
  TxOutIndex: Int,
  Script: UnlockScript,
)

final case class Transaction(
  Version: Int,
  TxIn: List[TxIn],
  TxOut: List[TxOut],
  )

object Transaction extends Parsable {
  override def buildFromJson(payload: String): Transaction = payload.parseJson.asJsObject.convertTo[Transaction]
}
