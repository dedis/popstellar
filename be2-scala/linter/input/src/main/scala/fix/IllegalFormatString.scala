/*
rule = IllegalFormatString
 */
package fix

object IllegalFormatString {
  def test(): Unit = {
    val name: String = "John"
    val age: Integer = 30
    val illegalFormatString1 = "%s is %d years old, %d"
    println(String.format(illegalFormatString1, name, age)) // assert: IllegalFormatString


    println(illegalFormatString1.format(name, age)) // assert: IllegalFormatString

    println("%s is %d years old, %d".format(name, age)) // assert: IllegalFormatString

  }
}
