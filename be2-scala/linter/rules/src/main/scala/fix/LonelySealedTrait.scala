/*
rule = LonelySealedTrait
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

import scala.collection.mutable

case class LonelySealedTraitDiag(t: Tree) extends Diagnostic {
  override def message: String = "Lonely sealed trait"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "A sealed trait that is not extended is considered dead code."

  override def position: Position = t.pos
}

class LonelySealedTrait extends SemanticRule("LonelySealedTrait") {

  private def findBaseClasses(parent: String, sealedTraitsHierarchy: mutable.Map[String, Set[String]]): Set[String] = {
    sealedTraitsHierarchy.get(parent) match {
      case Some(baseClasses) =>
        baseClasses ++ baseClasses.flatMap(findBaseClasses(_, sealedTraitsHierarchy))
      case None =>
        Set.empty[String]
    }
  }


  override def fix(implicit doc: SemanticDocument): Patch = {
    val sealedTraits = mutable.Map[String, Defn]()
    val parents = mutable.Set[String]()
    val sealedTraitsHierarchy = mutable.Map[String, Set[String]]()


    // Use display name to handle the case A[B] where A is a trait and B is a type parameter
    def prettyInits(inits: List[Init]): List[String] = inits.collect { case i if i.symbol.info.isDefined => i.symbol.info.get.displayName }

    def handleSealedTrait(inits: List[Init], cl: Defn, clname: String): Unit = {
      if(inits.isEmpty) sealedTraitsHierarchy += (clname -> Set())
      else {
        prettyInits(inits).foreach(i => sealedTraitsHierarchy += (i -> (sealedTraitsHierarchy.getOrElse(i, Set()) + clname)))
      }
      sealedTraits += (clname -> cl)
    }


    doc.tree.traverse {
      case tr @ Defn.Trait.After_4_6_0(mods, name, _, _, Template.After_4_4_0(_, inits, _, _, _))
        if mods.exists(m => m.toString.equals("sealed")) => handleSealedTrait(inits, tr, name.value)
      case cls @ Defn.Class.After_4_6_0(mods, name, _, _, Template.After_4_4_0(_, inits, _, _, _))
        if mods.exists(m => m.toString.equals("sealed")) => handleSealedTrait(inits, cls, name.value)
      case cl @ Defn.Class.After_4_6_0(_, _, _, _, _) => cl.symbol.info.get.signature match {
        case ClassSignature(_, pars, _, _) => pars.foreach(p => parents += p.toString())
        case _ => () // Class should have class signature
      }
      case Defn.Object(_, _, Template.After_4_4_0(_, inits, _, _, _)) => parents ++= prettyInits(inits)
    }

    sealedTraits.collect({
      case (name, cl) if !parents.contains(name)
        && parents.intersect(findBaseClasses(name,sealedTraitsHierarchy)).isEmpty => Patch.lint(LonelySealedTraitDiag(cl))
      //Either this sealed trait is directly extended or one of its sealed base classes / traits is being extended
      case _ => Patch.empty
    }).asPatch
  }
}
