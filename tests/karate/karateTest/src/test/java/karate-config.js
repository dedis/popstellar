function fn() {    
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    env = 'go';
  }
  var config = {
    env: env,
    wsUrl: 'ws/url/port/path', //Server url 
    timeout: 5000, //Timeout for websocket responce
    serverScript: 'src/test/java/pop/createLAO/launchServer.sh', //Path start server script
    args: []
  }
  if (env == 'go') {
    // customize
     config.wsUrl= 'ws://127.0.0.1:9000/organizer/client';
  
  } else if (env == 'scala') {
    // customize
    config.wsUrl= 'ws://127.0.0.1:8000/';

  }
  return config;
}