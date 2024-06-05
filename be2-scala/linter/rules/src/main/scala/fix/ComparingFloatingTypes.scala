/*
rule = CatchNpe
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class ComparingFloatingTypesDiag(floats: Tree) extends Diagnostic {
  override def message: String = "Floating type comparison"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Due to minor rounding errors, it is not advisable to compare floating-point numbers using the == operator. Either use a threshold based comparison, or switch to a BigDecimal."

  override def position: Position = floats.pos
}

class ComparingFloatingTypes extends SemanticRule("ComparingFloatingTypes") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    def getType(term: Term): Symbol = {
      term.symbol.info match {
        case Some(symInfo) => symInfo.signature match {
            case ValueSignature(TypeRef(_, symbol, _)) => symbol
            case _ => null
          }
        case _ => null
      }
    }

    def isFloatOrDouble(term: Term): Boolean = {
      val floatOrDoubleMatcher = SymbolMatcher.exact("scala/Float#", "scala/Double#")
      floatOrDoubleMatcher.matches(getType(term))
    }

    doc.tree.collect {
      case t @ Term.ApplyInfix.After_4_6_0(lhs, op, _, Term.ArgClause(List(right), _)) =>
        if (isFloatOrDouble(lhs) && isFloatOrDouble(right)) {
          op match {
            case Term.Name("==") | Term.Name("!=") => Patch.lint(ComparingFloatingTypesDiag(t))
            case _                                   => Patch.empty
          }
        } else {
          Patch.empty
        }
      case t @ Term.Apply.After_4_6_0(Term.Select(lhs, Term.Name("equals")), Term.ArgClause(List(right), _)) =>
        if (isFloatOrDouble(lhs) && isFloatOrDouble(right)) {
          Patch.lint(ComparingFloatingTypesDiag(t))
        } else {
          Patch.empty
        }
    }
  }.asPatch
}
