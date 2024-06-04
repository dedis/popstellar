/*
rule = CatchNpe
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class ComparingFloatingTypes extends SemanticRule("ComparingFloatingTypes") {

  private def diag(pos: Position) = Diagnostic("", "Floating type comparison", pos, "Due to minor rounding errors, it is not advisable to compare floating-point numbers using the == operator. Either use a threshold based comparison, or switch to a BigDecimal.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {

    def isFloatOrDouble(term: Term): Boolean = {
      Util.matchType(term, "scala/Float", "scala/Double")
    }

    doc.tree.collect {
      case t @ Term.ApplyInfix.After_4_6_0(lhs, op, _, Term.ArgClause(List(right), _)) =>
        if (isFloatOrDouble(lhs) && isFloatOrDouble(right)) {
          op match {
            case Term.Name("==") | Term.Name("!=") => Patch.lint(diag(t.pos))
            case _                                   => Patch.empty
          }
        } else {
          Patch.empty
        }
      case t @ Term.Apply.After_4_6_0(Term.Select(lhs, Term.Name("equals")), Term.ArgClause(List(right), _)) =>
        if (isFloatOrDouble(lhs) && isFloatOrDouble(right)) {
          Patch.lint(diag(t.pos))
        } else {
          Patch.empty
        }
    }
  }.asPatch
}
