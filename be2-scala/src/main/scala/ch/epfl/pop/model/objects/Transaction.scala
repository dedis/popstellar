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

  private def signaturePayload =
    Base64Data.encode(inputs.map { txin => f"${txin.TxOutHash}${txin.TxOutIndex}" }.reduce(_+_) +
      outputs.map { txout => f"${txout.Value}${txout.Script.Type}${txout.Script.PubkeyHash}" }.reduce(_+_))

  /**
   * This ensures the validity of the signatures, not that the funds are unspent.
   */
  def checkSignatures(): Boolean =
    inputs.forall { txin =>
      Signature(txin.Script.Sig).verify(txin.Script.Pubkey, signaturePayload)
    }

  def sign(keypairs: Seq[KeyPair]): Transaction = {
    val privateKeyIndex = Map.from(keypairs.map(p => p.publicKey -> p.privateKey))
    copy(inputs=inputs.map { txin =>
      val pk = txin.Script.Pubkey
      val k = privateKeyIndex.getOrElse(pk, throw new IllegalArgumentException(s"No private key for $pk"))
      val sig = k.signData(signaturePayload).signature
      txin.copy(Script=txin.Script.copy(Sig=sig))
    })
  }
}

object Transaction extends Parsable {
  override def buildFromJson(payload: String): Transaction = payload.parseJson.asJsObject.convertTo[Transaction]
}
