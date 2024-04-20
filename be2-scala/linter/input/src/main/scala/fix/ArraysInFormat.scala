/*
rule = ArraysInFormat
*/
package fix

import java.io.FileNotFoundException

object ArraysInFormat {
  def test() = {
    val array = new Array[String](3)
    String.format("Here are my cool elements %d", array) // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) /* assert: ArraysInFormat
                                                  ^^^^^
    Array passed to String.format
    */
  }


}
