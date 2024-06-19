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

    def rule(lhs: Term, rhs: Term, t: Term): Patch = {
      (lhs,rhs) match {
        // Either we have a comparison between two variables (e.g. g == f) or a variable and a literal (e.g. g == Double.NaN)
        // In the former case, we must find the definitions of the variables to see if one of them is Double.NaN
        case (Term.Name(_), Term.Name(_)) => Util.findDefinitions(doc.tree, Set(lhs, rhs)) match {
          case List((_, ld), (_, rd)) => matcher(ld, rd, t)
          case _ => Patch.empty
        }
        // In the latter case, we can directly match the terms
        case _ => matcher(lhs, rhs, t)
      }
    }

    doc.tree.collect {
      // Corresponds to myVar == Double.Nan, Double.Nan == myVar, myVar != Double.Nan, Double.Nan != myVar
      // We don't consider a comparison between Nan and a literal since it doesn't make sense in code
      case t @ Term.ApplyInfix.After_4_6_0(lhs, Term.Name("==") | Term.Name("!="), _, Term.ArgClause(List(rhs), _)) => rule(lhs, rhs, t)
      // Corresponds to myVar.equals(Double.Nan)
      case t @ Term.Apply.After_4_6_0(Term.Select(lhs, Term.Name("equals")), Term.ArgClause(List(right), _)) => rule(lhs, right, t)
    }.asPatch
  }
}
