/*
rule = OptionGet
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._
class OptionGet extends SemanticRule("OptionGet") {

  private def diag(pos: Position) = Diagnostic("", "Use of Option.Get", pos, "Using Option.get defeats the purpose of using Option in the first place. Use the following instead: Option.getOrElse, Option.fold, pattern matching or don't take the value out of the container and map over it to transform it", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to Option(_).get or o.get
      case t @ Term.Select(qual, Term.Name("get")) if Util.matchType(qual, "scala/Option") => Patch.lint(diag(t.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
