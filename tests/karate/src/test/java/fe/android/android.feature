Feature: android page object

  Background: Android Preset
    * configure driver = { type: 'android', webDriverPath : "/wd/hub", start: false, httpConfig : { readTimeout: 120000 }}
    * def driverOptions = { webDriverSession: { desiredCapabilities : "#(android.desiredConfig)"} }
    * configure retry = { count: 5, interval: 500 }

    * call read('classpath:fe/utils/mockbackend.feature')
    * def backendURL = 'ws://10.0.2.2:' + backend.getPort()


    Scenario: Connect to backend
      Given driver driverOptions
      # As the settings tab does not have an id, this is how we click on it.
      # If this breaks, use this code to log the page hierarchy :
      # karate.log(driver.getHttp().path("source").get().value)

      * driver.click('//*[@content-desc="More options"]')
      * driver.click('#com.github.dedis.popstellar:id/title')

      # Input the mock backend url and connect to it
      * driver.input('#com.github.dedis.popstellar:id/entry_box_server_url', backendURL)
      * driver.click('#com.github.dedis.popstellar:id/button_apply')
      * match backend.waitForConnection(5000) == true
