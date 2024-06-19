/*
rule = IllegalFormatString
 */
package fix

import fix.Util.{findDefinition, findDefinitions, findDefinitionsOrdered}
import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

import java.util.{IllegalFormatException, MissingFormatArgumentException, UnknownFormatConversionException}

class IllegalFormatString extends SemanticRule("IllegalFormatString") {

  private def diag(pos: Position) = Diagnostic("", "Illegal format string", pos, "An unchecked exception will be thrown when a format string contains an illegal syntax or a format specifier that is incompatible with the given arguments", LintSeverity.Error)

  // Rule tries to format the string to check if it ends in an exception, and if so lints
  //Term parameter is simply used to display the rule at the correct place
  private def rule(t: Term, value: String, args: List[Any]): Patch = {
    try value.format(args: _*)
    catch {
      case _: IllegalFormatException | _: MissingFormatArgumentException | _: UnknownFormatConversionException => return Patch.lint(diag(t.pos))
    }
    Patch.empty
  }

  override def fix(implicit doc: SemanticDocument): Patch = {

    // Method to get the args with their corresponding definitions
    def getMappedArgs(args: List[Term]): List[Any] = {
      (findDefinitionsOrdered(doc.tree, args) ++ args).collect { case Lit(value) => value }
    }

    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("format")), Term.ArgClause(args, _)) =>
        qual match {
          case Term.Name("String") =>
            //This case corresponds to String.format(format, args)
            //String.format argclause has format string as first argument and rest are arguments,
            // we can safely assume first argument is a string if we are doing a String.format()
            args match {
              case Lit.String(format) :: rest => rule(t, format, getMappedArgs(rest))
              case Term.Name(_) :: _ =>
                val mappedArgs = getMappedArgs(args)
                rule(t, mappedArgs.head.toString, mappedArgs.tail)
              case _ => Patch.empty
            }
          case strName @ Term.Name(_) =>
            //This case corresponds to a string declared as a val or var and used in a format call, e.g.
            // val myStr = "Hello %s"
            // myStr.format("world")
            val str = findDefinition(doc.tree, strName)
            str match {
              case Lit.String(value) => rule(t, value, getMappedArgs(args))
              case _ => Patch.empty
            }
          case Lit.String(value) =>
            //This case corresponds to a string literal used in a format call, e.g. "Hello %s".format("world")
            rule(t, value, getMappedArgs(args))
          case _ => Patch.empty
        }
    }
  }.asPatch


}
