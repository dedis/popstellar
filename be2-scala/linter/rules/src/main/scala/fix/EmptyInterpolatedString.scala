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
      // Corresponds to String.format(formatString, _) and String.format(), str.format()
      case t @ Term.Apply.After_4_6_0(Term.Select(Term.Name("String"), Term.Name("format")), Term.ArgClause(List(Lit.String(_)) | Nil, _)) => Patch.lint(diag(t.pos))
      // Corresponds to "str".format()
      case t @ Term.Apply.After_4_6_0(Term.Select(Lit.String(_) | Term.Name(_), Term.Name("format")), Term.ArgClause(Nil, _)) => Patch.lint(diag(t.pos))
      // Corresponds to f"str" and s"str"
      case t @ Term.Interpolate(_, _, Nil)                                                                         => Patch.lint(diag(t.pos))
    }.asPatch
  }
}
