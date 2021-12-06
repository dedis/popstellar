Feature: android test

  Background: App Preset
    * configure driver = { type: 'android', webDriverPath : "/wd/hub", start: false, httpConfig : { readTimeout: 120000 }}

  Scenario: android mobile app UI tests
    Given driver { webDriverSession: { desiredCapabilities : "#(android.desiredConfig)"} }
    And driver.click('#com.github.dedis.popstellar:id/tab_launch')
    And driver.input('#com.github.dedis.popstellar:id/entry_box_launch', "Lao")
    Then match driver.text('#com.github.dedis.popstellar:id/entry_box_launch') == 'Lao'
