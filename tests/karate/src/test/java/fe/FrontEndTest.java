package fe;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.junit5.Karate;
import common.utils.Reporting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FrontEndTest {

  /**
   * This test will execute all front-end tests with the web front-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testWeb() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("web");
  }

  /**
   * This test will execute all front-end tests with android front-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testAndroid() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("android");
  }

  /**
   * This is the main front-end test, it is made to be executed from the command line :
   * <p>
   * <code>mvn test -DargLine=-Dkarate.env=env -Dtest=FrontEndTest#fullTest</code>
   * <p>
   * It will execute all frontend tests and generate a report
   */
  @Test
  void fullTest() {
    Results results = Runner
      .builder()
      .relativeTo(getClass())
      .outputCucumberJson(true)
      .parallel(1);

    Reporting.generateReport(results.getReportDir());
    assertEquals(0, results.getFailCount(), results.getErrorMessages());
  }
}
