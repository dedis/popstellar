package be;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.junit5.Karate;
import common.utils.Reporting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BackEndTest {

  /**
   * This test will execute the tests for back-ends connected to a frontend with the Go back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testGoWithFrontend() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("go_client");
  }


  /**
   * This test will execute the tests for back-ends connected to another back-end with the Go back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testGoWithServer() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("go_server");
  }

  /**
   * This test will execute the tests for back-ends connected to a frontend with the Scala back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testScalaWithFrontend() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("scala_client");
  }

  /**
   * This test will execute the tests for back-ends connected to another back-end with the Scala back-end
   * It will not generate a clean report, but it can be used in development
   *
   * @return the Karate builder
   */
  @Karate.Test
  Karate testScalaWithServer() {
    return Karate.run()
      .relativeTo(getClass())
      .karateEnv("scala_server");
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


