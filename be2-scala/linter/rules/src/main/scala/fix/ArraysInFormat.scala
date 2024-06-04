/*
rule = ArraysInFormat
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class ArraysInFormat extends SemanticRule("ArraysInFormat") {

  private def diag(pos: Position) = Diagnostic("", "Array passed to format / interpolate string", pos, "An Array passed to String.format or interpolated string might result in an incorrect formatting", LintSeverity.Error)

  def rule(args: List[Stat])(implicit doc: SemanticDocument): Patch = {
    args.collect {
      case a if Util.matchType(a, "scala/Array") => Patch.lint(diag(a.pos))
      case _ => Patch.empty
    }.asPatch
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(args, _)) => rule(args)
      case Term.Interpolate(_, _, args) => args.collect { case Term.Block(stats) => rule(stats) }.asPatch
      case _ => Patch.empty
    }.asPatch
  }

}
