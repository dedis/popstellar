/*
rule = IllegalFormatString
 */
package fix

object IllegalFormatString {
  def test(): Unit = {
    val name = "John"
    val age = 30
    val illegalFormatString1 = "%s is %d years old, %d"
    println(illegalFormatString1.format(name, age)) // assert: IllegalFormatString

    val balance = 1000.50
    val illegalFormatString2 = "%2.2f CHF"
    println(illegalFormatString2.format(balance)) // assert: IllegalFormatString

    val illegalFormatString3 = "Age: %s"
    println(illegalFormatString3.format(age)) // assert: IllegalFormatString

  }
}
