/*
rule = IncorrectNumberOfArgsToFormat
 */
package fix

import fix.Util.findDefinition
import scalafix.lint.LintSeverity
import scalafix.v1._

import scala.meta._

class IncorrectNumberOfArgsToFormat extends SemanticRule("IncorrectNumberOfArgsToFormat") {

  private def diag(pos: Position) = Diagnostic("", "Incorrect number of arguments to format", pos, "The number of arguments passed to String.format doesn't correspond to the number of fields in the format string.", LintSeverity.Error)

  private val argRegex = "%((\\d+\\$)?[-#+ 0,(\\<]*?\\d?(\\.\\d+)?[tT]?[a-zA-Z]|%)".r

  private def rule(format: String, args: List[Any], t: Term): Patch = {
    // Compare expected argument count with the actual one, ignoring the format specifiers that don't take arguments
    val argCount = argRegex
      .findAllIn(format)
      .matchData
      .count(m => !doesNotTakeArguments(m.matched))

    if (argCount != args.size) Patch.lint(diag(t.pos)) else Patch.empty
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("format")), Term.ArgClause(args, _)) =>
        qual match {
          // Corresponds to String.format(formatString, args)
          case Term.Name("String") =>
            // The args is a list containing first the format string and then the arguments
            args match {
              // Either the format string is a literal string or a val/var string
              case Lit.String(format) :: rest => rule(format, rest, t)
              case (head @ Term.Name(_)) :: rest =>
                // If the format string is a val/var, we need to find its definition
                val format = findDefinition(doc.tree, head)
                format match {
                  case Lit.String(value) => rule(value, rest, t)
                }
              case _ => Patch.empty
            }
          // Corresponds to a string declared as a val or var and used in a format call, e.g. myStr.format(args)
          case strName @ Term.Name(_) => val str = findDefinition(doc.tree, strName)
            str match {
              case Lit.String(value) => rule(value, args, t)
              case _ => Patch.empty
            }
          // Corresponds to format directly applied on a string, e.g. "str".format(args)
          case Lit.String(value) => rule(value, args, t)

        }
      case _ => Patch.empty
    }.asPatch
  }

  private def doesNotTakeArguments(formatSpecifier: String): Boolean =
    formatSpecifier == "%%" || formatSpecifier == "%n"
}
