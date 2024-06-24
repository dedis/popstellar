/*
rule = NanComparison
 */
package fix

object NanComparison {

  def test() = {
    val d = 0.5d
    d == Double.NaN // assert: NanComparison
    Double.NaN == d // assert: NanComparison
    d.equals(Double.NaN) // assert: NanComparison
    Double.NaN.equals(d) // assert: NanComparison

    val f = 0.5f
    f == Double.NaN // assert: NanComparison
    Double.NaN == f // assert: NanComparison

    val g = Double.NaN
    f == g // assert: NanComparison
    g == f // assert: NanComparison

    d == g // assert: NanComparison
    g == d // assert: NanComparison
    d.equals(g) // assert: NanComparison
    g.equals(d) // assert: NanComparison
  }

}
