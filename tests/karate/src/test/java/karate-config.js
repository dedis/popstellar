function fn() {
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    env = 'go_client'
  }
  karate.log('karate.env system property is set to', env);
  var config = {
    env: env,
    host: 'hostname',    //Host of the server
    port: 0o000,  //Port
    path: 'URI path', //URI Path
    wsURL: 'ws/url/port/path', //Server url
    timeout: 5000, //Timeout for websocket response
    args: [],
  }
  if (env === 'go') {
    // customize
    config.host = '127.0.0.1';
    config.frontendPort = 9000;
    config.backendPort = 9001;
    config.frontendPath = 'client';
    config.backendPath = 'server';
    config.frontendWsURL = `ws://${config.host}:${config.frontendPort}/${config.frontendPath}`;
    config.backendWsURL = `ws://${config.host}:${config.backendPort}/${config.backendPath}`;
  } else if (env === 'scala') {
    // customize
    config.host = '127.0.0.1';
    config.frontendPort = 8000;
    config.backendPort = 8000; // Scala back-end does not have a specific server port
    config.frontendPath = 'client';
    config.backendPath = 'server';
    config.frontendWsURL = `ws://${config.host}:${config.frontendPort}/${config.frontendPath}`;
    config.backendWsURL = `ws://${config.host}:${config.backendPort}/${config.backendPath}`;
  } else if (env === 'web') {
    config.serverURL = karate.properties['serverURL'] || 'ws://localhost:9000/client';
    config.platformServerURL = karate.properties['platformServerURL'] || 'ws://localhost:9000/client';
    config.frontendURL = karate.properties['url'] || `file://${karate.toAbsolutePath('file:../../fe1-web/web-build/index.html')}`;
    config.screenWidth = karate.properties['screenWidth'] || 1920;
    config.screenHeight = karate.properties['screenHeight'] || 1080;

    let platform = karate.properties['platform'] || karate.os.type;
    if (platform === 'macosx') {
      platform = 'mac';
    }

    const browser = karate.properties['browser'] || 'chrome';
    const browserOptions = {
      chrome: {
        type: 'chromedriver',
        capabilities: {
          alwaysMatch: {
            'platformName': platform,
            'appium:automationName': 'Chromium',
            'browserName': 'chrome'
          }
        }
      },
      edge: {
        type: 'chromedriver',
        capabilities: {
          alwaysMatch: {
            'platformName': platform,
            'appium:automationName': 'Chromium',
            'browserName': 'MicrosoftEdge'
          }
        }
      },
      firefox: {
        type: 'geckodriver',
        capabilities: {
          alwaysMatch: {
            'platformName': platform,
            'appium:automationName': 'Gecko',
            'browserName': 'firefox'
          }
        }
      },
      safari: {
        type: 'safaridriver',
        capabilities: {
          alwaysMatch: {
            'platformName': platform,
            'appium:automationName': 'Safari',
            'browserName': 'safari'
          }
        }
      }
    };

    const { type, capabilities } = browserOptions[browser];


    karate.configure('driver', { type, port: 4723, webDriverPath : "/", start: false });
    config.webDriverOptions = {
      webDriverSession: {
        capabilities,
        // desiredCapabilities is there for compatibility with karate
        desiredCapabilities: {}
      }
    };
  } else if (env === 'android') {
    config.serverURL = karate.properties['serverURL'] || 'ws://localhost:9000/client';
    config.platformServerURL = karate.properties['platformServerURL'] || 'ws://10.0.2.2:9000/client';
    karate.configure('driver', { type: 'android', webDriverPath : "/", start: false });
    const app = karate.properties['app'] || '../../fe2-android/app/build/outputs/apk/debug/app-debug.apk';
    config.webDriverOptions = {
      webDriverSession: {
        capabilities: {
          alwaysMatch: {
            'appium:platformName': 'Android',
            'appium:automationName': 'uiautomator2',
            'appium:app': `${karate.toAbsolutePath(`file:${app}`)}`,
            'appium:autoGrantPermissions': true,
            'appium:avd': karate.properties['avd'],
          }
        },
        // desiredCapabilities is there for compatibility with karate
        desiredCapabilities: {
        }
      }
    };
  }

  return config;
}
