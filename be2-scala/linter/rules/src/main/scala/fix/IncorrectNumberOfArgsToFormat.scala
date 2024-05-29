/*
rule = IncorrectNumberOfArgsToFormat
 */
package fix

import fix.Util.findDefinition
import scalafix.lint.LintSeverity
import scalafix.v1._

import scala.meta._

case class IncorrectNumberOfArgsToFormatDiag(string: Tree) extends Diagnostic {
  override def message: String = "Incorrect number of arguments to format"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "The number of arguments passed to String.format doesn't correspond to the number of fields in the format string."

  override def position: Position = string.pos
}

class IncorrectNumberOfArgsToFormat extends SemanticRule("IncorrectNumberOfArgsToFormat") {

  private val argRegex = "%((\\d+\\$)?[-#+ 0,(\\<]*?\\d?(\\.\\d+)?[tT]?[a-zA-Z]|%)".r

  private def rule(format: String, args: List[Any], t: Term): Patch = {
    val argCount = argRegex
      .findAllIn(format)
      .matchData
      .count(m => !doesNotTakeArguments(m.matched))

    if (argCount != args.size) Patch.lint(IncorrectNumberOfArgsToFormatDiag(t)) else Patch.empty
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("format")), Term.ArgClause(args, _)) =>
        qual match {
          case Term.Name("String") =>
          args match {
            case Lit.String(format) :: rest => rule(format, rest, t)
            case (head @ Term.Name(_)) :: rest =>
              val format = findDefinition(doc.tree, head)
              format match {
                case Lit.String(value) => rule(value, rest, t)
              }
            case _ => Patch.empty
          }
            val format = findDefinition(doc.tree,args.head)
            format match {
              case Lit.String(value) => rule(value, args.tail, t)
              case _ => Patch.empty
            }
          case strName @ Term.Name(_) => val str = findDefinition(doc.tree, strName)
            str match {
              case Lit.String(value) => rule(value, args, t)
              case _ => Patch.empty
            }
          case Lit.String(value) => rule(value, args, t)

        }
    }.asPatch
  }

  private def doesNotTakeArguments(formatSpecifier: String): Boolean =
    formatSpecifier == "%%" || formatSpecifier == "%n"
}
