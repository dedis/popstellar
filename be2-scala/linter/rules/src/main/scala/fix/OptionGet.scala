/*
rule = OptionGet
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

case class OptionGetDiag(tree: Tree) extends Diagnostic {
  override def message: String = "Use of Option.Get"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Using Option.get defeats the purpose of using Option in the first place. Use the following instead: Option.getOrElse, Option.fold, pattern matching or don't take the value out of the container and map over it to transform it"

  override def position: Position = tree.pos
}

class OptionGet extends SemanticRule("OptionGet") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Select(Term.Apply.After_4_6_0(Term.Name("Option"), _), Term.Name("get")) => Patch.lint(OptionGetDiag(t))
      case t @ Term.Select(qual, Term.Name("get")) =>
        val qualType = Util.getType(qual)
        if (qualType != null && SymbolMatcher.exact("scala/Option#").matches(qualType)) {
          Patch.lint(OptionGetDiag(t))
        }
        else Patch.empty
      case _ => Patch.empty
    }.asPatch
  }
}
