package ch.epfl.pop.model.objects

import scala.annotation.tailrec

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
  def inRange(v: Uint53): Boolean = v >= MinValue && v <= MaxValue

  /** Compute the sum of many Uint53 values, or indicate an overflow if the sum is too large.
    *
    * Per the Either documentation, "Convention dictates that Left is used for failure and Right is used for success."
    */
  def safeSum(seq: IterableOnce[Uint53]): Either[ArithmeticOverflowError, Uint53] = {
    @tailrec
    def safeSumRec(it: Iterator[Uint53], acc: Long): Option[Long] =
      if acc > MaxValue then
        return None

      it.nextOption() match
        case Some(x) =>
          require(inRange(x), s"value $x out of range for uint53")
          safeSumRec(it, acc + x)
        case _ => Some(acc)

    safeSumRec(seq.iterator, 0L) match
      case Some(x) => Right(x)
      case _       => Left(new ArithmeticOverflowError("uint53 addition overflow"))
  }
}
