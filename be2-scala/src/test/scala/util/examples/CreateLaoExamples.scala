package util.examples

import ch.epfl.pop.model.network.requests.lao.JsonRpcRequestCreateLao
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.validator.SchemaValidatorSuite
import ch.epfl.pop.model.network.JsonRpcMessage
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Base64Data
import ch.epfl.pop.model.objects.PublicKey
import ch.epfl.pop.model.objects.Signature
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.model.objects.WitnessSignaturePair
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.objects.Channel


object CreateLaoExamples{

  def getJsonRequestFromMessage(msg: Message): JsonRpcRequest = {
       JsonRpcRequest("2.0",MethodType.PUBLISH, new ParamsWithMessage(Channel.rootChannel,msg), Some(1))
  }

  final val createLao : Message =  Message(
    Base64Data("eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9"),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==")),
    Hash(Base64Data("2mAAevx61TZJi4groVGqqkeLEQq0e-qM6PGmTWuShyY=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val laoCreateEmptyName  = Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogInA4VFcwOEFXbEJTY3M5RkdYSzNLYkxRWDdGYmd6OF9nTHdYLUI1VkVXUzA9IiwKICAgICJuYW1lIjogIiIsCiAgICAiY3JlYXRpb24iOiAxNjMzMDk4MjM0LAogICAgIm9yZ2FuaXplciI6ICJKOWZCekpWNzBKazVjLWkzMjc3VXE0Q21lTDR0NTNXRGZVZ2hhSzBIcGVNPSIsCiAgICAid2l0bmVzc2VzIjogW10KfQo="),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("zPS8K38Hd-a2WYhOZja7kKTIefvmOVC6aOdsS1jHBVsCjONfca1QAkbIQto4ACjQaokl9tLAz8Wr6BRy626fCA==")),
    Hash(Base64Data("U3-x36ScWAbDewkGwKJ1Dv6XN-RCo53GT9hi7qUpSDY=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )


  final lazy val laoCreateIdInvalid =  Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09IiwKICAgICJuYW1lIjogIkxBTyIsCiAgICAiY3JlYXRpb24iOiAxNjMzMDk4MjM0LAogICAgIm9yZ2FuaXplciI6ICJKOWZCekpWNzBKazVjLWkzMjc3VXE0Q21lTDR0NTNXRGZVZ2hhSzBIcGVNPSIsCiAgICAid2l0bmVzc2VzIjogW10KfQo="),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("feZQ14nttNzHgJ83WT-mtdBx98Zs3hG9kX2me5eUuDHfWLiLfu4rn5BuJrbuDJ5glZLvqWqiI7M9My-bYIWcDg==")),
    Hash(Base64Data("-eerbRQOpC9gr2ASi7cQigqa-HJMw3kXjXvGjMHw0DA=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val laoCreateAdditionalParam  = Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogImZ6SlNaaktmLTJjYlhIN2tkczlIOE5PUnV1RklSTGtldkpsTjdxUWVtam89IiwKICAgICJuYW1lIjogIkxBTyIsCiAgICAiY3JlYXRpb24iOiAxNjMzMDk4MjM0LAogICAgIm9yZ2FuaXplciI6ICJKOWZCekpWNzBKazVjLWkzMjc3VXE0Q21lTDR0NTNXRGZVZ2hhSzBIcGVNPSIsCiAgICAid2l0bmVzc2VzIjogW10sCiAgICAiYWRkaXRpb25hbF9wYXJhbXMiOiAwCn0K"),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("fFIuZq8QjCGwVBtpatStsIc9G3K37B9L9jwrwLDSRhQTJZue56Z6QxzQfC6G6GJr5MYoMBzaP2wWjyLQhpIVDg==")),
    Hash(Base64Data("_gUgfgQwuGfpjAXdaiEoYF6lARFf07Do6IRajyM7-FE=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val laoCreateMissingParams  =  Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogImZ6SlNaaktmLTJjYlhIN2tkczlIOE5PUnV1RklSTGtldkpsTjdxUWVtam89Igp9Cg=="),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("GOm3WIaKn5E9kgAKSsCGlN5cs4hBvwBwHGDaEbnYChPMql3gXFk7-x-7H54_p_zeIAKsH4aNfWzQOZg1sSODBA==")),
    Hash(Base64Data("KEN_I352JmnkrctiL1TSwx4t3DE8Sgqiaj0ogljJVtI=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val laoCreateOrgNot64  = Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogImtHeFZCTXM2WWpFSEJiUngwMUIxbU0zUGZyTkt6NmlDUU9NUWhIc3p3Yms9IiwKICAgICJuYW1lIjogIkxBTyIsCiAgICAiY3JlYXRpb24iOiAxNjMzMDk4MjM0LAogICAgIm9yZ2FuaXplciI6ICJAQEAiLAogICAgIndpdG5lc3NlcyI6IFtdCn0K"),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("LhGZaij6q-B_vQTRT36WwBWeA9nyAE8g5eH5MVGRTWpAsQzb-IYeBu1sXrQXLzmpxU-xxPWrsvWd-qErUYSBAQ==")),
    Hash(Base64Data("ebwKFUehGWcMefvwp0RS78B1voo9yfptUZ9UtK0c6ZI=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val laoCreateWitNot64 = Message(
    Base64Data("ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogImZ6SlNaaktmLTJjYlhIN2tkczlIOE5PUnV1RklSTGtldkpsTjdxUWVtam89IiwKICAgICJuYW1lIjogIkxBTyIsCiAgICAiY3JlYXRpb24iOiAxNjMzMDk4MjM0LAogICAgIm9yZ2FuaXplciI6ICJKOWZCekpWNzBKazVjLWkzMjc3VXE0Q21lTDR0NTNXRGZVZ2hhSzBIcGVNPSIsCiAgICAid2l0bmVzc2VzIjogWyJAQEAiXQp9Cg=="),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("U9MFuyI7RWG4Hjv9vfhgV40cq1utbYURdC28z2iN788Jn6bzIY1zLnGr6M7QaLZeQQqthVxt9CNftp4MzKC6Ag==")),
    Hash(Base64Data("YhyL2wT0dywNvMclrzGVNJjxzSsaM_t9P8kAzfJfHZQ=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final lazy val allCreateReq = LazyList(createLao, laoCreateIdInvalid,
                                         laoCreateAdditionalParam,
                                         laoCreateMissingParams,
                                         laoCreateOrgNot64)

}
