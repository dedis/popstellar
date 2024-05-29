/*
rule = OptionSize
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

case class OptionSizeDiag(tree: Tree) extends Diagnostic {
  override def message: String = "Prefer Option.isDefined instead of Option.size"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Prefer to use Option.isDefined, Option.isEmpty or Option.nonEmpty instead of Option.size."

  override def position: Position = tree.pos
}

class OptionSize extends SemanticRule("OptionSize") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Select(Term.Apply.After_4_6_0(Term.Name("Option"), _), Term.Name("size")) => Patch.lint(OptionSizeDiag(t))
      case t @ Term.Select(qual, Term.Name("size")) =>
        val qualType = Util.getType(qual)
        if (qualType != null && SymbolMatcher.exact("scala/Option#").matches(qualType)) {
          Patch.lint(OptionSizeDiag(t))
        }
        else Patch.empty
      case _ => Patch.empty
    }.asPatch
  }
}
