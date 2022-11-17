package be;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.junit5.Karate;
import common.utils.Reporting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BackEndTest {

  /**
   * This test will execute all back-end tests with the Go back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testGo() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("go");
  }

  /**
   * This test will execute all back-end tests with the Scala back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testScala() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("scala");
  }

  /**
   * This is the main front-end test, it is made to be executed from the command line :
   * <p>
   * <code>mvn test -DargLine=-Dkarate.env=env -Dtest=BackEndTest#fullTest</code>
   * <p>
   * It will execute all backend tests and generate a report
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


