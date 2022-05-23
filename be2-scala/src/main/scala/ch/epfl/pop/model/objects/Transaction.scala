package ch.epfl.pop.model.objects

import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.Parsable
import spray.json._

final case class LockScript(
  Type: String,
  PubkeyHash: Address,
)

final case class TxOut(
  Value: Long,
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
  version: Int,
  inputs: List[TxIn],
  outputs: List[TxOut],
  lockTime: Int,
) {
  lazy val transactionId = {
    val strings = collection.mutable.ListBuffer.empty[String]
    strings ++= inputs.foldRight(List.empty[String]) { (txin, acc) =>
        txin.Script.Pubkey.base64Data.toString ::
        txin.Script.Sig.toString ::
        txin.Script.Type ::
        txin.TxOutHash.toString ::
        txin.TxOutIndex.toString :: acc
      }
    strings += lockTime.toString
    strings ++= outputs.foldRight(List.empty[String]) { (txout, acc) =>
        txout.Script.PubkeyHash.base64Data.toString ::
        txout.Script.Type ::
        txout.Value.toString :: acc
      }
    strings += version.toString
    Hash.fromStrings(strings.toSeq: _*)
  }
}

object Transaction extends Parsable {
  override def buildFromJson(payload: String): Transaction = payload.parseJson.asJsObject.convertTo[Transaction]
}
