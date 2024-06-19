/*
rule = MapGetAndGetOrElse
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

case class MapGetAndGetOrElseDiag(map: Tree) extends Diagnostic {
  override def message: String = "Using of Map.get().getOrElse instead of Map.getOrElse()"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Map.get(key).getOrElse(value) can be replaced with Map.getOrElse(key, value), which is more concise."

  override def position: Position = map.pos
}

class MapGetAndGetOrElse extends SemanticRule("MapGetAndGetOrElse") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case Term.Apply.After_4_6_0(Term.Select(Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("get")), _), Term.Name("getOrElse")), _)
        if Util.matchType(qual, "scala/collection/immutable/Map", "scala/collection/mutable/Map", "scala/Predef.Map")
          => Patch.lint(MapGetAndGetOrElseDiag(qual))
      case _ => Patch.empty
    }.asPatch
  }
}
