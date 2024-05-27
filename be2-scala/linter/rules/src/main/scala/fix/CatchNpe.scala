/*
rule = CatchNpe
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class CatchNpeDiag(catch_tree: Tree) extends Diagnostic {
  override def message: String = "Catching NPE"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Avoid using null at all cost and you shouldn't need to catch NullPointerExceptions. Prefer Option to indicate potentially missing values and use Try to materialize exceptions thrown by any external libraries."

  override def position: Position = catch_tree.pos
}

class CatchNpe extends SemanticRule("CatchNpe") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case Term.Try(_, catches, _) => catches.collect {
          case Case(pat, _, _) => pat match {
              case Pat.Typed(_, tpe) if tpe.toString().equals("NullPointerException") => Patch.lint(CatchNpeDiag(pat))
              case _                                                                  => Patch.empty
            }
        }
    }
  }.flatten.asPatch
}
