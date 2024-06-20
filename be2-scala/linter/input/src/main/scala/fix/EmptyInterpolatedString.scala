/*
rule = EmptyInterpolatedString
 */
package fix

object EmptyInterpolatedString {
  def test(): Unit = {
    print(f"Here's my cute interpolation") // assert: EmptyInterpolatedString
    print(s"Here's my amazing interpolation") // assert: EmptyInterpolatedString
    String.format("I'm hungry!") // assert: EmptyInterpolatedString
    val str = "I'm hungry!"
    str.format() // assert: EmptyInterpolatedString
    "I'm hungry!".format() // assert: EmptyInterpolatedString
    "Test %s".format("test") // scalafix: ok;
  }
}
