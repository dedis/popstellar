/*
rule = ArraysInFormat
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class ArraysInFormatDiag(array: Tree) extends Diagnostic {
  override def message: String = "Array passed to format / interpolate string"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "An Array passed to String.format or interpolated string might result in an incorrect formatting"

  override def position: Position = array.pos
}

class ArraysInFormat extends SemanticRule("ArraysInFormat") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    def rule(args: List[Stat]) = {
      args.collect {
        case t @ Term.Name(_) =>
          t.symbol.info match {
            case Some(symInfo) => symInfo.signature match {
                case ValueSignature(TypeRef(_, symbol, _)) if SymbolMatcher.exact("scala/Array#").matches(symbol) => Patch.lint(ArraysInFormatDiag(t))
                case _                                                                                            => Patch.empty
              }
            case _ => Patch.empty
          }
      }
    }

    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(args, _)) => rule(args)
      case Term.Interpolate(_, _, args)                                                         => rule(args.flatMap { case Term.Block(stats) => stats }) // Args in Term.Interpolate come in Term.Block requiring to extract the stats
    }.flatten.asPatch
  }
}
