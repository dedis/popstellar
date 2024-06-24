/*
rule = LonelySealedTrait
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

import scala.collection.mutable

class LonelySealedTrait extends SemanticRule("LonelySealedTrait") {

  // This method finds the base classes for a given sealed trait / class, since we have collected the hierarchy of
  // sealed traits / classes with our bottom-up traversal
  private def diag(pos: Position) = Diagnostic("", "Lonely sealed trait", pos, "A sealed trait that is not extended is considered dead code.", LintSeverity.Error)

  private def findBaseClasses(parent: String, sealedTraitsHierarchy: mutable.Map[String, Set[String]]): Set[String] = {
    sealedTraitsHierarchy.get(parent) match {
      case Some(baseClasses) =>
        // We recursively find the base classes of the base classes: we first get the direct children and then look
        // at the children of the children
        baseClasses ++ baseClasses.flatMap(findBaseClasses(_, sealedTraitsHierarchy))
      case None =>
        Set.empty[String]
    }
  }


  override def fix(implicit doc: SemanticDocument): Patch = {
    val sealedTraits = mutable.Map[String, Defn]() // Map to store the Defn and position the warning correctly
    val parents = mutable.Set[String]()
    val sealedTraitsHierarchy = mutable.Map[String, Set[String]]()


    // Use display name to handle the case A[B] where A is a trait and B is a type parameter
    def prettyInits(inits: List[Init]): List[String] = inits.collect { case i if i.symbol.info.isDefined => i.symbol.info.get.displayName }

    /* Function to handle base class collection
    * It works by adding the passed sealed class as a child to its parents children, i.e. bottom-up traversal.
    * Getting the base classes from the parent would be much harder and would require a tree traversal since the parents do not store their base classes.
    * However, children store their parents so a bottom-up solution is easy. */
    def handleSealedTrait(inits: List[Init], cl: Defn, clname: String): Unit = {
      if(inits.isEmpty) sealedTraitsHierarchy += (clname -> Set())
      else {
        prettyInits(inits).foreach(i => sealedTraitsHierarchy += (i -> (sealedTraitsHierarchy.getOrElse(i, Set()) + clname)))
      }
      sealedTraits += (clname -> cl)
    }

    /* We first traverse the doc, collecting the sealed traits and classes.
    * In this traversal, we also store the base classes of the parents of the sealed traits and classes
    * For the non-sealed classes and objects, we collect their parents to later match sealed and non sealed */
    doc.tree.traverse {
      // Corresponds to a sealed trait declaration
      case tr @ Defn.Trait.After_4_6_0(mods, name, _, _, Template.After_4_4_0(_, inits, _, _, _))
        if mods.exists(m => m.toString.equals("sealed")) => handleSealedTrait(inits, tr, name.value)
      // Corresponds to a sealed class declaration
      case cls @ Defn.Class.After_4_6_0(mods, name, _, _, Template.After_4_4_0(_, inits, _, _, _))
        if mods.exists(m => m.toString.equals("sealed")) => handleSealedTrait(inits, cls, name.value)
      // Corresponds to a class declaration (not sealed)
      case cl @ Defn.Class.After_4_6_0(_, _, _, _, _) => cl.symbol.info.get.signature match {
        case ClassSignature(_, pars, _, _) => pars.foreach(p => parents += p.toString())
        case _ => () // Class should have class signature
      }
      // Corresponds to an object declaration (cannot be sealed)
      case Defn.Object(_, _, Template.After_4_4_0(_, inits, _, _, _)) => parents ++= prettyInits(inits)
    }

    // Now that we know the sealed traits and classes and their children, we can check if any of their child classes
    // have an implementation in the same file. If not, the sealed trait / class is not extended and is considered dead code
    sealedTraits.collect({
      case (name, cl) if !parents.contains(name)
        && parents.intersect(findBaseClasses(name,sealedTraitsHierarchy)).isEmpty => Patch.lint(diag(cl.pos))
      //Either this sealed trait is directly extended or one of its sealed base classes / traits is being extended
      case _ => Patch.empty
    }).asPatch
  }
}
