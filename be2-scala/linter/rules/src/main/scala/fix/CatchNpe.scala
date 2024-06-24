/*
rule = CatchNpe
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class CatchNpe extends SemanticRule("CatchNpe") {

  private def diag(pos: Position) = Diagnostic("", "Catching NPE", pos, "Avoid using null at all cost and you shouldn't need to catch NullPointerExceptions. Prefer Option to indicate potentially missing values and use Try to materialize exceptions thrown by any external libraries.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to try { ... } catch { case e: NullPointerException => ... }
      case Term.Try(_, catches, _) => catches.collect {
          case Case(pat, _, _) => pat match {
              case Pat.Typed(_, tpe) if tpe.toString().equals("NullPointerException") => Patch.lint(diag(pat.pos))
              case _                                                                  => Patch.empty
            }
        }
    }
  }.flatten.asPatch
}
