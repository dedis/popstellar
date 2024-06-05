/*
rule = StripMarginOnRegex
 */
package fix

object StripMarginOnRegex {
  def test() = {
    val regex = "match|this".stripMargin.r // assert: StripMarginOnRegex
    val myRegex = "match|this"
    myRegex.stripMargin.r // assert: StripMarginOnRegex
    "match|that".stripMargin.r // assert: StripMarginOnRegex
    "match_this".stripMargin.r // scalafix: ok;
    "match|this".r // scalafix: ok;
  }
}
