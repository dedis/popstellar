/*
rule = IncorrectNumberOfArgsToFormat
 */
package fix

object IncorrectNumberOfArgsToFormat {
  def test() = {
    val name: String = "John"
    val age: Integer = 30
    val illegalFormatString1 = "%s is %d years old, %d"
    illegalFormatString1.format(name, age, 134, 54, "Olivia") // assert: IncorrectNumberOfArgsToFormat
    illegalFormatString1.format(name) // assert: IncorrectNumberOfArgsToFormat

    "%s is %d years old, %d".format(name, age, 134, 54, "Olivia") // assert: IncorrectNumberOfArgsToFormat
    "%s is %d years old, %d".format(name) // assert: IncorrectNumberOfArgsToFormat

    String.format(illegalFormatString1, name) // assert: IncorrectNumberOfArgsToFormat

    illegalFormatString1.format(name, age, 134) // scalafix: ok;
    "%s is %d years old, %d".format(name, age, 134) // scalafix: ok;
  }

}
