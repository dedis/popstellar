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
      case a if Util.matchType(a, "scala/Array", "scala/Array.empty") => Patch.lint(diag(a.pos))
      case _ => Patch.empty
    }.asPatch
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to String.format(formatString, args), myStr.format(args) and "str".format(args)
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(args, _)) => rule(args)
      // Corresponds to f"str $args" and s"str $args"
      // Args in Term.Interpolate come in Term.Block requiring to extract the stats
      case Term.Interpolate(_, _, args) => args.collect { case Term.Block(stats) => rule(stats) }.asPatch
      case _ => Patch.empty
    }.asPatch
  }

}
