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
    port: 0000,  //Port 
    path:  'URI path', //URI Path
    pk: 'PK orgnizer', //PK of the oranizer 
    wsUrl: 'ws/url/port/path', //Server url 
    timeout: 5000, //Timeout for websocket responce
    serverCmd: 'Command to launch the server', // Cmd to launch the server
    serverDIR: 'Path to server source directory',
    logPath: 'path/to/log/output/file',
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
     //Directory to launch the server from
     config.serverDIR = 'C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\be1-go' ;
     config.serverCmd = ['bash', '-c', `./pop organizer --pk ${config.pk} serve`];
     config.logPath = 'C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\tests\\karate\\karateTest\\create.log';
      
  
  } else if (env == 'scala') {
    // customize
    config.host= '127.0.0.1';
    config.port= 8000;
    config.path= '';
    config.wsUrl= `ws://${config.host}:${config.port}/${config.path}`;
    config.serverDIR = 'C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\be2-scala';
    var pathConfig = `${config.serverDIR}\\src\\main\\scala\\ch\\epfl\\pop\\config`;
    config.serverCmd = ['sbt.bat', `-Dscala.config=${pathConfig}`, 'run'];
    config.logPath = 'C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\tests\\karate\\karateTest\\create.log';
  
  }
  return config;
}