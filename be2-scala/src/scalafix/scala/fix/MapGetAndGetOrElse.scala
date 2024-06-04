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
      case Term.Apply.After_4_6_0(Term.Select(Term.Apply.After_4_6_0(Term.Select(qual, Term.Name("get")), _), Term.Name("getOrElse")), _)
        if Util.matchType(qual, "scala/collection/immutable/Map", "scala/collection/mutable/Map", "scala/Predef.Map")
          => Patch.lint(diag(qual.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
