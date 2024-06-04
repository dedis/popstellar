/*
rule = StripMarginOnRegex
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

class StripMarginOnRegex extends SemanticRule("StripMarginOnRegex") {

  private def diag(pos: Position) = Diagnostic("", "Strip margin on regex", pos, "Strip margin will strip | from regex - possible corrupted regex.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Select(Term.Select(Lit.String(string), Term.Name("stripMargin")), Term.Name("r")) if string.contains('|') => Patch.lint(diag(t.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
