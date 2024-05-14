/*
rule = CatchNpe
 */
package fix

object CatchNpe {
  def test(): Unit = {
    try {
      val array = new Array[String](3)
    } catch {
      case e: NullPointerException      => print("NPE!") // assert: CatchNpe
      case e: IndexOutOfBoundsException => print("Out of bounds") // scalafix: ok;
    }
  }
}
