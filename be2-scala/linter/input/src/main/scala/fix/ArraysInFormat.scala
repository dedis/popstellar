/*
rule = ArraysInFormat
 */
package fix

object ArraysInFormat {
  def test() = {
    val array = new Array[String](3)
    val str = f"Here are my cool elements ${array}" // assert: ArraysInFormat
    val str2 = s"Here are my cool elements ${array}" // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) /* assert: ArraysInFormat
                                                  ^^^^^
    Array passed to format / interpolate string
     */
  }

}
