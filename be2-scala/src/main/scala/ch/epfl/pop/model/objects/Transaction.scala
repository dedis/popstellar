package ch.epfl.pop.model.objects

import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.Parsable
import spray.json._

final case class LockScript(
    `type`: String,
    pubkeyHash: Address
)

final case class UnlockScript(
    `type`: String,
    pubkey: PublicKey,
    sig: Base64Data
)

final case class Transaction(
    version: Int,
    inputs: List[Transaction.Input],
    outputs: List[Transaction.Output],
    lockTime: Int
) {
  lazy val transactionId: Hash = {
    val strings = collection.mutable.ListBuffer.empty[String]
    strings ++= inputs.foldRight(List.empty[String]) { (txin, acc) =>
      txin.script.pubkey.base64Data.toString ::
        txin.script.sig.toString ::
        txin.script.`type` ::
        txin.txOutHash.toString ::
        txin.txOutIndex.toString :: acc
    }
    strings += lockTime.toString
    strings ++= outputs.foldRight(List.empty[String]) { (txout, acc) =>
      txout.script.pubkeyHash.base64Data.toString ::
        txout.script.`type` ::
        txout.value.toString :: acc
    }
    strings += version.toString
    Hash.fromStrings(strings.toSeq: _*)
  }

  private def signaturePayload =
    Base64Data.encode(inputs.map { txin => s"${txin.txOutHash}${txin.txOutIndex}" }.reduce(_ + _) +
      outputs.map { txout => s"${txout.value}${txout.script.`type`}${txout.script.pubkeyHash.base64Data}" }.reduce(_ + _))

  /** This ensures the validity of the signatures, not that the funds are unspent.
    */
  def checkSignatures(): Boolean =
    inputs.forall { txin =>
      Signature(txin.script.sig).verify(txin.script.pubkey, signaturePayload)
    }

  def sumOutputs(): Either[Error, Uint53] =
    Uint53.safeSum(outputs.map(_.value))

  def sign(keypairs: Seq[KeyPair]): Transaction = {
    val privateKeyIndex = Map.from(keypairs.map(p => p.publicKey -> p.privateKey))
    copy(inputs = inputs.map { txin =>
      val pk = txin.script.pubkey
      val k = privateKeyIndex.getOrElse(pk, throw new IllegalArgumentException(s"No private key for $pk"))
      val sig = k.signData(signaturePayload).signature
      txin.copy(script = txin.script.copy(sig = sig))
    })
  }
}

object Transaction extends Parsable {
  override def buildFromJson(payload: String): Transaction = payload.parseJson.asJsObject.convertTo[Transaction]

  final case class Input(
      txOutHash: Hash,
      txOutIndex: Int,
      script: UnlockScript
  )

  final case class Output(
      value: Uint53,
      script: LockScript
  )
}
