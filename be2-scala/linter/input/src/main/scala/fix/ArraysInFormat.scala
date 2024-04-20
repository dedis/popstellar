/*
rule = ArraysInFormat
*/
package fix

import java.io.FileNotFoundException

object ArraysInFormat {
  def test() = {
    val array = new Array[String](3)
    print(f"Here are my cool elements ${array}") // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) // assert: ArraysInFormat
    String.format("Here are my cool elements %d", array) /* assert: ArraysInFormat
                                                  ^^^^^
    An Array passed to String.format might result in an incorrect formatting.
    */
  }


}
