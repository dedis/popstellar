/*
rule = EmptyInterpolatedString
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class EmptyInterpolatedStringDiag(string: Tree) extends Diagnostic {
  override def message: String = "Empty interpolated / format string"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "String declared as interpolated but has no parameters or usage of String.format with no parameters can be turned into a regular string."

  override def position: Position = string.pos
}

class EmptyInterpolatedString extends SemanticRule("EmptyInterpolatedString") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), Term.ArgClause(List(Lit.String(_)), _)) => Patch.lint(EmptyInterpolatedStringDiag(t))
      case t @ Term.Interpolate(_, _, Nil)                                                                         => Patch.lint(EmptyInterpolatedStringDiag(t))
    }.asPatch
  }
}
