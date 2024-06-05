/*
rule = EmptyInterpolatedString
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class EmptyInterpolatedString extends SemanticRule("EmptyInterpolatedString") {

  private def diag(pos: Position) = Diagnostic("", "Empty interpolated / format string", pos, "String declared as interpolated but has no parameters or usage of String.format with no parameters can be turned into a regular string.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to String.format(formatString, _) and "str".format(_)
      case t @ Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(List(Lit.String(_)), _) | Term.ArgClause(Nil, _)) => Patch.lint(diag(t.pos))
      // Corresponds to f"str" and s"str"
      case t @ Term.Interpolate(_, _, Nil)                                                                         => Patch.lint(diag(t.pos))
    }.asPatch
  }
}
