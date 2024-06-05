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
      // Corresponds to lhs == rhs or lhs != rhs
      // We then check if both of the operands are a Float or a Double and if so, we lint
      case t @ Term.ApplyInfix.After_4_6_0(lhs, Term.Name("==") | Term.Name("!="), _, Term.ArgClause(List(right), _))
        if isFloatOrDouble(lhs) && isFloatOrDouble(right) => Patch.lint(diag(t.pos))
      // Corresponds to lhs.equals(rhs)
      case t @ Term.Apply.After_4_6_0(Term.Select(lhs, Term.Name("equals")), Term.ArgClause(List(right), _))
        if isFloatOrDouble(lhs) && isFloatOrDouble(right) => Patch.lint(diag(t.pos))
      case _ => Patch.empty
    }
  }.asPatch
}
