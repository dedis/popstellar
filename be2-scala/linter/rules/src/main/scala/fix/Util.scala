package fix

import scalafix.v1._

import scala.meta._

/** Utilities class for linter */
object Util {

  /** Get the type of a val/var.
    * @param term The term / stat to get the type of
   * @param doc The document to get the semantic information from
   * @return The type of the val/var, or Symbol.None if not found
   */
  // Type information is stored in the ValueSignature of the Term if it is a val/var.
  // We pass the term a Stat as they have the same information, we can thus handle more cases and Term is a child of Stat
  def getType(term: Stat)(implicit doc: SemanticDocument): Symbol = {
    term.symbol.info match {
      case Some(symInfo) => symInfo.signature match {
        case ValueSignature(TypeRef(_, symbol, _)) => symbol
        case _ => Symbol.None
      }
      case _ => Symbol.None
    }
  }

  /**
   * Compare the type of a term with the passed symbols
   * @param term The term to compare
   * @param symbols The symbols to compare with
   * @param doc The document to get the semantic information from
   * @return Whether or not the term matches one of the symbols
   */
  def matchType(term: Stat, symbols: String*)(implicit doc: SemanticDocument): Boolean = {
    val symbolMatcher = SymbolMatcher.normalized(symbols: _*)
    symbolMatcher.matches(term.symbol) || symbolMatcher.matches(getType(term))
    // Checks the term symbol matches that of the symbol (i.e. when we use the type directly),
    // or if the type of the variable matches.
  }

  /** Find definition of val / var.
   * @param tree The tree to search in
   * @param name The name of the val / var to search for
   * @return The definition of the val / var if found, null otherwise
   */
  // We simply explore the tree and check the definitions and return if a name matches
  def findDefinition(tree: Tree, name: Term): Any = {
    tree.collect {
      case Defn.Val(_, List(Pat.Var(varName)), _, value)
        if varName.value.equals(name.toString) => value
      case Defn.Var.After_4_7_2(_, List(Pat.Var(varName)), _, value)
        if varName == name => value
    }.headOption.orNull
  }

  /** Find multiple definitions in one tree traversal.
   * @param tree The tree to search in
   * @param nameSet The set of names to search for
   * @return An unordered list of definitions found
   */
  // Takes a set as argument as we will have to do many lookups
  def findDefinitions(tree: Tree, nameSet: Set[Term]): List[(Term, Any)] = {
    tree.collect {
      case Defn.Val(_, List(Pat.Var(varName)), _, value) if nameSet.exists(_.toString().equals(varName.value)) => nameSet.find(_.toString().equals(varName.value)).get -> value
      case Defn.Var.After_4_7_2(_, List(Pat.Var(varName)), _, value) if nameSet.exists(_.toString().equals(varName.value)) => nameSet.find(_.toString().equals(varName.value)).get -> value
    }
  }

  /**
   * Finds multiple definitions in one tree traversal, ordered with the order in the list
   * @param tree The tree to search in
   * @param nameSet The set of names to search for
   * @return A list of the definitions in the order of the names
   */
  /* Since findDefinitions take a set for efficiency, the order is lost. We use this function to restore it.
  * It is still more efficient than passing a list to findDefinitions as here we have an algorithm that depends on the
  * number of variables to find definitions, not on the number of lookups */
  def findDefinitionsOrdered(tree: Tree, nameSet: List[Term]): List[Any] = {
    findDefinitions(tree, nameSet.toSet).sortBy { case (term, _) => nameSet.indexOf(term) }.map { case (_, value) => value }
  }
}
