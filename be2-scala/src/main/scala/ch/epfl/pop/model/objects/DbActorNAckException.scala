package ch.epfl.pop.model.objects


final case class DbActorNAckException(private val code: Int, private val message: String, private val cause: Throwable = None.orNull) extends Exception(message, cause){
    def getCode: Int = code
}
