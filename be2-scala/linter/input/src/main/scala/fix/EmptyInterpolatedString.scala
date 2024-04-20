/*
rule = EmptyInterpolatedString
 */
package fix

object EmptyInterpolatedString {
  val interpolate = f"Here's my cute interpolation" // assert: EmptyInterpolatedString
  val interpolate_two = s"Here's my amazing interpolationg" // assert: EmptyInterpolatedString
  String.format("I'm hungry!") // assert: EmptyInterpolatedString

}
