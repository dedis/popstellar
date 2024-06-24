/*
rule = ImpossibleOptionSizeCondition
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class ImpossibleOptionSizeCondition extends SemanticRule("ImpossibleOptionSizeCondition") {

  private def diag(pos: Position) = Diagnostic("", "Impossible Option.size condition", pos, "Option.size > 1 can never be true, did you mean to use Option.nonEmpty instead?", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {

    doc.tree.collect {
      // Corresponds to Option(_).size > 1 or Option(_).size >= 1, or o.size > 1 or o.size >= 1 with o a variable
      case t @ Term.ApplyInfix.After_4_6_0(Term.Select(qual, Term.Name("size")), Term.Name(">") | Term.Name(">="),
      _, Term.ArgClause(List(comparedValue), _))
        if Util.matchType(qual, "scala/Option", "scala/Some") =>
        // We then check if the value is greater than or equal to 1 and if so, we lint
        comparedValue match {
          case Lit.Int(actualValue) if actualValue >= 1 => Patch.lint(diag(t.pos))
          case _          => Patch.empty
        }
      case _ => Patch.empty
    }
  }.asPatch
}
