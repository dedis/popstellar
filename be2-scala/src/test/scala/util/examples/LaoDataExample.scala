package util.examples

import ch.epfl.pop.model.objects.{Base64Data, LaoData, PrivateKey, PublicKey}

object LaoDataExample {
  final val PUBLICKEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  final val PKOWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
  final val PKATTENDEE: PublicKey = PublicKey(Base64Data.encode("attendee1"))
  final val LAODATA: LaoData = LaoData(PKOWNER, List(PKATTENDEE), PRIVATEKEY, PUBLICKEY, List.empty)
}
