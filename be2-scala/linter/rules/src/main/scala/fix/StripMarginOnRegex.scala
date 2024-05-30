/*
rule = StripMarginOnRegex
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

case class StripMarginOnRegexDiag(tree: Tree) extends Diagnostic {
  override def message: String = "Strip margin on regex"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Strip margin will strip | from regex - possible corrupted regex."

  override def position: Position = tree.pos
}

class StripMarginOnRegex extends SemanticRule("StripMarginOnRegex") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    println(doc.tree.structure)
    doc.tree.collect {
      case t @ Term.Select(Term.Select(str@Lit.String(_), Term.Name("stripMargin")), Term.Name("r")) if str.value.contains('|') => Patch.lint(StripMarginOnRegexDiag(t))
      case _ => Patch.empty
    }.asPatch
  }
}
