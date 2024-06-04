/*
rule = ArraysInFormat
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class ArraysInFormat extends SemanticRule("ArraysInFormat") {

  private def diag(pos: Position) = Diagnostic("", "Array passed to format / interpolate string", pos, "An Array passed to String.format or interpolated string might result in an incorrect formatting", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(args, _)) if args.exists(Util.matchType(_, "scala/Array")) => Patch.lint(diag(args.head.pos))
      case Term.Interpolate(_, _, args) if args.flatMap {case Term.Block(stats) => stats }.exists(Util.matchType(_, "scala/Array")) => Patch.lint(diag(args.head.pos)) // Args in Term.Interpolate come in Term.Block requiring to extract the stats
      case _ => Patch.empty
    }.asPatch
  }
}
