/*
rule = ImpossibleOptionSizeCondition
 */
package fix

import fix.Util.getType
import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class ImpossibleOptionSizeConditionDiag(option: Tree) extends Diagnostic {
  override def message: String = "Impossible Option.size condition"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Option.size > 1 can never be true, did you mean to use Option.nonEmpty instead?"

  override def position: Position = option.pos
}

class ImpossibleOptionSizeCondition extends SemanticRule("ImpossibleOptionSizeCondition") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    doc.tree.collect {
      case t @ Term.ApplyInfix.After_4_6_0(Term.Select(qual, Term.Name("size")), Term.Name(">") | Term.Name(">="),
      _, Term.ArgClause(List(comparedValue), _))
        if SymbolMatcher.exact("scala/Option#", "scala/Some#").matches(getType(qual)) =>
        comparedValue match {
          case Lit.Int(actualValue) if actualValue >= 1 => Patch.lint(ImpossibleOptionSizeConditionDiag(t))
          case _          => Patch.empty
        }
      case _ => Patch.empty
    }
  }.asPatch
}
