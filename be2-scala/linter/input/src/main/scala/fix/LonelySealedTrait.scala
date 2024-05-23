/*
rule = LonelySealedTrait
 */
package fix

// Test values taken from Scapegoat test
object LonelySealedTrait {

  sealed trait NoImpl // assert: LonelySealedTrait

  sealed trait Impl1 // scalafix: ok;
  object Implemented extends Impl1

  sealed trait Impl2 // scalafix: ok;
  case object Implemented2 extends Impl2

  sealed trait Impl3 // scalafix: ok;
  case object Implemented3 extends Serializable with Impl3

  sealed trait Impl4 // scalafix: ok;
  case class Implemented4() extends Impl4

  sealed trait Impl5 // scalafix: ok;
  class Implemented extends Impl5

  sealed trait Impl6 // scalafix: ok;
  case class Implemented6(name: String) extends Serializable with Impl6

  sealed trait Impl7 // scalafix: ok;
  class Implemented7(name: String) extends Serializable with Impl7

  sealed trait Impl8 // scalafix: ok;
  case class Implemented8(name: String) extends Impl8

  sealed trait Impl9 // scalafix: ok;
  class Implemented9(name: String) extends Impl9

  sealed trait Impl10 // scalafix: ok;
  sealed trait Impl11 // scalafix: ok;
  case class Implemented10(name: String) extends Impl10
  case class Implemented11(name: String) extends Impl11

  trait AnalyzerFilter {
    def name: String
  }

  trait AnalyzerFilterDefinition {
    def filterType: String
  }

  sealed trait CharFilter extends AnalyzerFilter // scalafix: ok;

  sealed trait CharFilterDefinition extends CharFilter with AnalyzerFilterDefinition // scalafix: ok;

  case object HtmlStripCharFilter extends CharFilter {
    val name = "html_strip"
  }

  case class MappingCharFilter(name: String, mappings: (String, String)*)
    extends CharFilterDefinition {
    val filterType = "mapping"
  }

  case class PatternReplaceCharFilter(name: String, pattern: String, replacement: String)
    extends CharFilterDefinition {
    val filterType = "pattern_replace"
  }

  sealed abstract class MultiMode(val elastic: String) // scalafix: ok;
  case object MultiMode {
    case object Min extends MultiMode("min")
    case object Max extends MultiMode("max")
    case object Sum extends MultiMode("sum")
    case object Avg extends MultiMode("avg")
  }

  sealed trait A[S] // scalafix: ok;
  case object B extends A[String]
  case object C extends A[BigDecimal]

  sealed abstract class IndexOptionsNoImplementation(val value: String) // assert: LonelySealedTrait

  sealed abstract class IndexOptions(val value: String) // scalafix: ok;
  object IndexOptions {
    case object Docs extends IndexOptions("docs")
    case object Freqs extends IndexOptions("freqs")
    case object Positions extends IndexOptions("positions")
  }

  sealed trait D // scalafix: ok;
  sealed trait E extends D
  sealed trait F extends E
  object F1 extends F

  sealed class G // scalafix: ok;
  sealed class H extends G
  sealed class I extends H
  object I1 extends I

  sealed trait J // scalafix: ok;
  sealed trait K extends J
  sealed class L extends K
  object L1 extends L


}
