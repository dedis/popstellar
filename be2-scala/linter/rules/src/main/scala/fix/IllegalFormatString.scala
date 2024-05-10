/*
rule = IllegalFormatString
 */
package fix

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
    doc.tree.collect {
      case t @ Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("format")), Term.ArgClause(_, _)) =>
        println(qual.symbol.value)
        //TODO see if worth implementing, since we would also have to handle cases:
        // 1. String.format(format, args)
        // 2. mystr.format(args)
        // 3. "str".format(args)
//        try String.format(format, args: _*)
//        catch {
//          case _: IllegalFormatException | _: MissingFormatArgumentException | _: UnknownFormatConversionException => Patch.lint(IllegalFormatStringDiag(t))
//        }
        Patch.empty
    }
  }.asPatch
}
