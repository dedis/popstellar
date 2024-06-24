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
      // Corresponds to "regex".stripMargin.r or myRegex.stripMargin.r
      case t @ Term.Select(Term.Select(qual, Term.Name("stripMargin")), Term.Name("r")) =>
        qual match {
          case name @ Term.Name(_) => Util.findDefinition(doc.tree, name) match {
            case Lit.String(value) if value.contains('|') => Patch.lint(diag(t.pos))
            case _ => Patch.empty
          }
          case Lit.String(value) if value.contains('|') => Patch.lint(diag(t.pos))
          case _ => Patch.empty
        }
      case _ => Patch.empty
    }.asPatch
  }
}
