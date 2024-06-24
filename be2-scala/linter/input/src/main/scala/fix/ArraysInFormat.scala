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
    "Here are my cool elements %d".format(array) // assert: ArraysInFormat
    val str3 = "Here are my cool elements %d"
    str3.format(array) // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) /* assert: ArraysInFormat
                                                  ^^^^^
    Array passed to format / interpolate string
     */

    String.format("Here are my cool elements %d", Array.empty[Int]) // assert: ArraysInFormat

    "Here are my cool elements %d".format(13) // scalafix: ok;
  }

}
