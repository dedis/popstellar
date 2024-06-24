/*
rule = IncorrectlyNamedExceptions
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class IncorrectlyNamedExceptions extends SemanticRule("IncorrectlyNamedExceptions") {

  private def diag(pos: Position) = Diagnostic("", "Incorrectly named exceptions", pos, "Class named exception does not derive from Exception / class derived from Exception is not named *Exception.", LintSeverity.Error)

  // Helper function to check if a class inherits from Exception, going through the ancestors
  private def inheritsFromException(symbol: Symbol)(implicit doc: SemanticDocument): Boolean = {
    symbol.info match {
      case Some(info) =>
        // Check if the current type contains an Exception in its name
        info.signature match {
          case ClassSignature(_, _, _, _) if info.displayName.contains("Exception") => true
          case ClassSignature(_, parents, _, _) =>
            // Recursively check parent types
            parents.map(_.asInstanceOf[TypeRef].symbol).exists(inheritsFromException(_))
         case TypeSignature(_, TypeRef(_, symbol1, _), TypeRef(_, symbol2, _)) =>
           // Check if inherits java.lang.Exception
           val matcher = SymbolMatcher.exact("java/lang/Exception#")
           matcher.matches(symbol1) || matcher.matches(symbol2)
          case _ => false
        }
      case None => false
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // In this rule, we check if there is an exception class that does not inherit from Exception
      // Corresponds to a class declaration
      case cl @ Defn.Class.After_4_6_0(_, Type.Name(name), _, _, _) =>
        cl.symbol.info.get.signature match {
          case ClassSignature(_, parents, _, _) =>
            // We then check the parents: either its direct parent is an Exception or one of its ancestors is an Exception
            if (!name.contains("Exception") && parents.map(_.asInstanceOf[TypeRef].symbol).exists(inheritsFromException))
              Patch.lint(diag(cl.pos))
            else Patch.empty
          case _ => Patch.empty
        }
    }.asPatch
  }

}
