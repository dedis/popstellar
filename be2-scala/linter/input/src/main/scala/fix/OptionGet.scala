/*
rule = OptionGet
 */
package fix

object OptionGet {
  def test() = {
    val o = Option("olivia")
    o.get // assert: OptionGet

    Option("layla").get // assert: OptionGet
  }
}
