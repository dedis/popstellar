/*
rule = NanComparison
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

class NanComparison extends SemanticRule("NanComparison") {

  def diag(pos: Position) = Diagnostic("", "Comparison with NaN", pos, "NaN comparison will always fail. Use value.isNaN instead.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {

    def matcher(l: Any, r: Any, t: Tree): Patch = {
      // Either we have definitions to extract or we have direct terms and matching is the same
      // => matcher function avoids repetition. Type of arguments is any since findDefinition will return Any,
      // but it doesn't matter since we do pattern matching
      (l, r) match {
        case (Term.Select(Term.Name("Double"), Term.Name("NaN")), _) | (_, Term.Select(Term.Name("Double"), Term.Name("NaN"))) => Patch.lint(diag(t.pos))
        case _ => Patch.empty
      }
    }

    doc.tree.collect {
      case t @ Term.ApplyInfix.After_4_6_0(lhs, Term.Name("=="), _, Term.ArgClause(List(rhs), _)) =>
        (lhs,rhs) match {
          case (Term.Name(_), Term.Name(_)) => Util.findDefinitions(doc.tree, Set(lhs, rhs)) match {
              case List((_, ld), (_, rd)) => matcher(ld, rd, t)
              case _ => Patch.empty
            }
          // Extract definitions and match in the case both are variables
          case _ => matcher(lhs, rhs, t)
        }
    }.asPatch
  }
}
