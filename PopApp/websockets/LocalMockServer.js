const webSocketsServerPort = 8000;
const webSocketServer = require('websocket').server;
const http = require('http');

// https://blog.logrocket.com/websockets-tutorial-how-to-go-real-time-with-node-and-react-8e4693fbf843/
// Spinning the http server and the websocket server.
const server = http.createServer();
server.listen(webSocketsServerPort);
console.log('listening on port 8000 :)');


const wsServer = new webSocketServer({
  httpServer: server
});

const clients = {};

// This code generates unique user id for every user.
const getUniqueID = () => {
  const s4 = () => Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
  return s4() + s4() + '-' + s4();
};

wsServer.on('request', function (request) {
  const userID = getUniqueID();
  console.log((new Date()) + ' Received a new connection from origin ' + request.origin + '.');

  // You can rewrite this part of the code to accept only the requests from allowed origin
  const connection = request.accept(null, request.origin);
  clients[userID] = connection;
  console.log('connected: ' + userID + ' in ' + Object.getOwnPropertyNames(clients));

  connection.on('message', function(message) {
    if (message.type === 'utf8') {
      console.log('Received Message :) : ', message.utf8Data);

      const answers = [{
        success: 'true',
        error: 'null'
      }, {
        success: 'false',
        error: 'Error occurred (dummy error)!'
      }];
      const idx = (Math.floor(Math.random() * (1000000))) % answers.length;
      //var answers = [{type: "answer", msg: message.utf8Data}];
      //const idx = 0;

      // broadcasting message to all connected clients
      for(let key in clients) {
        clients[key].sendUTF(JSON.stringify(answers[idx]));
        console.log('sent Message to: ', clients[key]);
      }
    }
  })
});
