/*
rule = IllegalFormatString
 */
package fix

object IllegalFormatString {
  def test(): Unit = {
    val name: String = "John"
    val age: Integer = 30
    val illegalFormatString1 = "%s is %d years old, %d"
    String.format(illegalFormatString1, name, age) // assert: IllegalFormatString

    String.format("%d is %d years old", age, name) // assert: IllegalFormatString

    illegalFormatString1.format(name, age) // assert: IllegalFormatString

    "%s is %d years old, %d".format(name, age) // assert: IllegalFormatString

    "%5.5q".format("sam") // assert: IllegalFormatString

    "% s".format("sam") // assert: IllegalFormatString

    "%qs".format("sam") // assert: IllegalFormatString

    "%.-5s".format("sam") // assert: IllegalFormatString

    "%.s".format("sam") // assert: IllegalFormatString

    "%<s %s".format("sam", "sam") // assert: IllegalFormatString

    "%.2f %s".format(14.5, "sammmmmmmmm") // scalafix: ok;
    "%010d".format(0) // scalafix: ok;

    "%s is %d years old, %d".format(name, age, 145) // scalafix: ok;


  }
}
