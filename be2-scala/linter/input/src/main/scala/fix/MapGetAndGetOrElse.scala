/*
rule = MapGetAndGetOrElse
 */
package fix

object MapGetAndGetOrElse {
  def test() = {
    val numMap1 = Map(1 -> "one", 2 -> "two")
    numMap1.get(1).getOrElse("unknown") // assert: MapGetAndGetOrElse


    val numMap2 = scala.collection.mutable.Map("one" -> 1, "two" -> 2)
    numMap2.get("one").getOrElse(-1) // assert: MapGetAndGetOrElse

    Map("John" -> "Smith", "Peter" -> "Rabbit").get("Sarah").getOrElse("-") // assert: MapGetAndGetOrElse

  }

}
