/*
rule = MapGetAndGetOrElse
 */
package fix

import scalafix.lint.LintSeverity

import scala.meta._
import scalafix.v1._

class MapGetAndGetOrElse extends SemanticRule("MapGetAndGetOrElse") {

  private def diag(pos: Position) = Diagnostic("", "Using of Map.get().getOrElse instead of Map.getOrElse()", pos, "Map.get(key).getOrElse(value) can be replaced with Map.getOrElse(key, value), which is more concise.", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to map.get(key).getOrElse(value)
      case Term.Apply.After_4_6_0(Term.Select(Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("get")), _), Term.Name("getOrElse")), _)
        // Maps can either be immutable, mutable or simply a Map(k,v) from Predef that we use as Map(k,v).get(key).getOrElse(value)
        if Util.matchType(qual, "scala/collection/immutable/Map", "scala/collection/mutable/Map", "scala/Predef.Map")
          => Patch.lint(diag(qual.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
