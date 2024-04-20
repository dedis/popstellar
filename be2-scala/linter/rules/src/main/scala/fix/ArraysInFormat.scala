/*
rule = ArraysInFormat
*/
package fix

import scalafix.lint.LintSeverity

import scala.meta.{Stat, _}
import scalafix.v1._

import scala.meta.Term.unapply
import scala.meta.internal.semanticdb.Scala.Names.TermName


case class ArraysInFormatDiag(array: Tree) extends Diagnostic {
  override def message: String = "Array passed to String.format"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "An Array passed to String.format might result in an incorrect formatting"

  override def position: Position = array.pos
}



class ArraysInFormat extends SemanticRule("ArraysInFormat") {


  override def fix(implicit doc: SemanticDocument): Patch = {
    def getType(symbol: Symbol): SemanticType =
      symbol.info.get.signature match {
        case MethodSignature(_, _, returnType) =>
          returnType
      }

    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(_, Term.Name("format")), args)
        if args.values.exists(t => getType(t.symbol).toString().equals("Array")) =>
        Patch.lint(ArraysInFormatDiag(doc.tree))
    }
  }.asPatch
}
