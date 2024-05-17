package fix

import scalafix.v1._

import scala.meta._

object Util {
  def getType(term: Term)(implicit doc: SemanticDocument): Symbol = {
    term.symbol.info match {
      case Some(symInfo) => symInfo.signature match {
        case ValueSignature(TypeRef(_, symbol, _)) => symbol
        case _ => null
      }
      case _ => null
    }
  }

  def findDefinition(tree: Tree, name: Term): Any = {
    tree.collect {
      case Defn.Val(_, List(Pat.Var(varName)), _, Lit(value))
        if varName.value.equals(name.toString) => value
      case Defn.Var.After_4_7_2(_, List(Pat.Var(varName)), _, value)
        if varName == name => value
    }.headOption.orNull
  }

  def findDefinitions(tree: Tree, nameSet: Set[Term]): List[(Term, Any)] = {
    tree.collect {
      case Defn.Val(_, List(Pat.Var(varName)), _, Lit(value)) if nameSet.exists(_.toString().equals(varName.value)) => nameSet.find(_.toString().equals(varName.value)).get -> value
      case Defn.Var.After_4_7_2(_, List(Pat.Var(varName)), _, value) if nameSet.exists(_.toString().equals(varName.value)) => nameSet.find(_.toString().equals(varName.value)).get -> value
    }
  }

  // Finds multiple definitions in one tree traversal
  def findDefinitionsMap(tree: Tree, nameSet: Set[Term]): Map[Term, Any] = {
    findDefinitions(tree, nameSet).toMap
  }

  def findDefinitionsOrdered(tree: Tree, nameSet: List[Term]): List[Any] = {
    findDefinitions(tree, nameSet.toSet).sortBy { case (term, _) => nameSet.indexOf(term) }.map { case (_, value) => value }
  }
}
