package ch.epfl.pop.model.objects

class ArithmeticOverflowError(msg: String) extends Error(msg)

/** Operations on 53-bits non-negative integers.
  *
  * These values can be handled without loss of precision in IEEE754 64-bit floating numbers, and thus in JSON.
  */
object Uint53 {

  /** Internal representation in this implementation */
  type Repr = Long

  /** Smallest allowed value */
  final val MinValue: Uint53 = 0

  /** Largest allowed value */
  final val MaxValue: Uint53 = 0x1fffffffffffffL

  /** Whether the value is in-range */
  def inRange(v: Uint53) = v >= MinValue && v <= MaxValue

  /** Compute the sum of many Uint53 values, or indicate an overflow if the sum is too large.
    *
    * Per the Either documentation, "Convention dictates that Left is used for failure and Right is used for success."
    */
  def safeSum(seq: IterableOnce[Uint53]): Either[ArithmeticOverflowError, Uint53] = {
    var acc = 0L
    val it = seq.iterator
    while (it.hasNext) {
      val v = it.next
      require(inRange(v), s"value $v out of range for uint53")
      acc += v
      if (acc > MaxValue)
        return Left(new ArithmeticOverflowError("uint53 addition overflow"))
    }
    Right(acc)
  }
}
