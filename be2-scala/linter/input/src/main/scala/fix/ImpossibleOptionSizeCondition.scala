/*
rule = ImpossibleOptionSizeCondition
 */
package fix

object ImpossibleOptionSizeCondition {
  def test(): Unit = {
    val opt = Option(23)
    val opt2 = Some(42)
    if (opt.size > 1) { // assert: ImpossibleOptionSizeCondition
      print("Size is 1")
    } else {
      print("Size is not 1")
    }
    if (opt2.size > 1) { // assert: ImpossibleOptionSizeCondition
      print("Size is 1")
    } else {
      print("Size is not 1")
    }
  }

}
