function fn() {    
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    //env = 'scala';
    env = 'go'
  }
  karate.log('karate.env system property is set to', env);
  var config = {
    env: env,
    host: 'hostname',    //Host of the server
    port: 0o000,  //Port
    path:  'URI path', //URI Path
    wsUrl: 'ws/url/port/path', //Server url
    timeout: 5000, //Timeout for websocket response
    args: [],
  }
  if (env == 'go') {
    // customize
     config.pk = 'J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM='; 
     //'g6XxoDTcz2tQZLjiK6zK24foSLSxU5P5tUYlKqhedCo=';
     config.host = '127.0.0.1';
     config.port = 9000 ;
     config.path = 'organizer/client';
     config.wsUrl = `ws://${config.host}:${config.port}/${config.path}`;
  } else if (env == 'scala') {
    // customize
    config.host= '127.0.0.1';
    config.port= 8000;
    config.path= '';
    config.wsUrl= `ws://${config.host}:${config.port}/${config.path}`;
  }
  return config;
}