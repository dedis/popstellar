/*
rule = OptionSize
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

class OptionSize extends SemanticRule("OptionSize") {

  private def diag(pos: Position) = Diagnostic("", "Use of Option.Size", pos, "Prefer to use Option.isDefined, Option.isEmpty or Option.nonEmpty instead of Option.size.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to Option(_).size or o.size
      case t @ Term.Select(qual, Term.Name("size")) if Util.matchType(qual, "scala/Option") => Patch.lint(diag(t.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
