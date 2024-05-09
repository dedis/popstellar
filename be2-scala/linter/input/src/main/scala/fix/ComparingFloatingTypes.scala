/*
rule = ComparingFloatingTypes
 */
package fix

object ComparingFloatingTypes {
  def test(): Boolean = {
    val f1 = 1.46456f
    val f2 = 1.46456f
    if (f1 == f2) { // assert: ComparingFloatingTypes
      print("Equal!")
    } else {
      print("Not equal")
    }
    val d1 = 1.546456
    val d2 = 1.546456
    if (d1 == d2) { // assert: ComparingFloatingTypes
      print("Equal!")
    } else {
      print("Not equal!")
    }
    f1 == d1 // assert: ComparingFloatingTypes
    f1.equals(f2) // assert: ComparingFloatingTypes
    d1.equals(d2) // assert: ComparingFloatingTypes
  }

}
