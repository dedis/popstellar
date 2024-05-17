/*
rule = IllegalFormatString
 */
package fix

import fix.Util.{findDefinition, findDefinitions, findDefinitionsOrdered}
import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

import java.util.{IllegalFormatException, MissingFormatArgumentException, UnknownFormatConversionException}

case class IllegalFormatStringDiag(string: Tree) extends Diagnostic {
  override def message: String = "Illegal format string"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "An unchecked exception will be thrown when a format string contains an illegal syntax or a format specifier that is incompatible with the given arguments"

  override def position: Position = string.pos
}

class IllegalFormatString extends SemanticRule("IllegalFormatString") {
  override def fix(implicit doc: SemanticDocument): Patch = {

    //Term parameter is simply used to display the rule at the correct place
    def rule(term: Term, value: String, args: List[Any]): Patch = {
      try value.format(args: _*)
      catch {
        case _: IllegalFormatException | _: MissingFormatArgumentException | _: UnknownFormatConversionException => return Patch.lint(IllegalFormatStringDiag(term))
      }
      Patch.empty
    }


    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("format")), Term.ArgClause(args, _)) =>
        val mappedArgs = findDefinitionsOrdered(doc.tree, args) ++ args.collect { case Lit(value) => value } //literals will not be found in the tree
        qual match {
          case Term.Name("String") =>
            //This case corresponds to String.format(format, args)
            //String.format argclause has format string as first argument and rest are arguments,
            // we can safely assume first argument is a string if we are doing a String.format()
            rule(t, mappedArgs.head.toString, mappedArgs.tail)
          case strName @ Term.Name(_) =>
            //This case corresponds to a string declared as a val or var and used in a format call, e.g.
            // val myStr = "Hello %s"
            // myStr.format("world")
            val str = findDefinition(doc.tree, strName)
            str match {
              case value: String => rule(t, value, mappedArgs)
              case _ => Patch.empty
            }
          case Lit.String(value) =>
            //This case corresponds to a string literal used in a format call, e.g. "Hello %s".format("world")
            rule(t, value, mappedArgs)
          case _ => Patch.empty
        }
    }
  }.asPatch


}
