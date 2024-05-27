/*
rule = EmptyInterpolatedString
 */
package fix

object EmptyInterpolatedString {
  def test(): Unit = {
    print(f"Here's my cute interpolation") // assert: EmptyInterpolatedString
    print(s"Here's my amazing interpolationg") // assert: EmptyInterpolatedString
    print(String.format("I'm hungry!")) // assert: EmptyInterpolatedString
  }
}
