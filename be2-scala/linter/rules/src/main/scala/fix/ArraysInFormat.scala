/*
rule = ArraysInFormat
*/
package fix

import scalafix.lint.LintSeverity

import scala.meta.{Stat, _}
import scalafix.v1._

import scala.meta.Term.unapply
import scala.meta.contrib.XtensionTreeOps
import scala.meta.internal.semanticdb.Scala.Names.TermName
import scala.meta.internal.semanticdb.SymbolInformation


case class ArraysInFormatDiag(array: Tree) extends Diagnostic {
  override def message: String = "Array passed to String.format"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "An Array passed to String.format might result in an incorrect formatting"

  override def position: Position = array.pos
}



class ArraysInFormat extends SemanticRule("ArraysInFormat") {


  override def fix(implicit doc: SemanticDocument): Patch = {

    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), args) =>
        args.values.collect {
          case t @Term.Name(_) =>
            t.symbol.info match {
              case Some(symInfo) => symInfo.signature match {
                case ValueSignature(TypeRef(_, symbol, _)) if symbol.toString().equals("scala/Array#") => Patch.lint(ArraysInFormatDiag(t))
                case _ => Patch.empty
              }
              case _ => Patch.empty
          }
        }
    }.flatten.asPatch
  }
}
