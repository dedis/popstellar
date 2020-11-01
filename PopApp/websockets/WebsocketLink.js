import React from 'react';
import { w3cwebsocket as W3CWebSocket } from "websocket";


const SERVER_ANSWER_INTERVAL_MS = 25;
const SERVER_ANSWER_MAX_ATTEMPTS = 160;

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

export default class WebsocketLink {

  static #ws;
  static serverAnswer = null;


  static printServerAnswer() { console.log(this.serverAnswer); } // TODO debug function


  static sendRequestToServer(message) {
    if (this.#ws == null)
      WebsocketLink._initWebsocket();

    WebsocketLink._sendMessage(message);
  }


  static waitServerAnswer(resolveServerAnswer, rejectServerAnswer) {
    if (this.serverAnswer == null) {

      let count = 0;

      const id = window.setInterval(() => {
        if (this.serverAnswer == null && count < SERVER_ANSWER_MAX_ATTEMPTS) {
          count += 1;
        } else {
          if (count === SERVER_ANSWER_MAX_ATTEMPTS)
            rejectServerAnswer("Maximum waiting time for server answer reached : " + SERVER_ANSWER_MAX_ATTEMPTS * SERVER_ANSWER_INTERVAL_MS + "[ms] (waitServerAnswer)");
          else
            resolveServerAnswer(this.serverAnswer);
          window.clearInterval(id);
        }
      }, SERVER_ANSWER_INTERVAL_MS);

    } else {
      // server answer is already available (this.serverAnswer != null)
      resolveServerAnswer(this.serverAnswer);
    }
  }


  static _initWebsocket(address = '127.0.0.1', port =  '8000') {
    console.log("initiating web socket : " + 'ws://' + address + ':' + port);
    const ws = new W3CWebSocket('ws://' + address + ':' + port);


    ws.onopen = () => { };

    ws.onmessage = (message) => {
      const data = JSON.parse(message.data);
      //console.log('we got a reply from server (WsLink) : ', data);
      //this.answerQueue.push(data);
      this.serverAnswer = data;
    };

    this.#ws = ws;
    this.serverAnswer = null;
  }


  static _waitWebsocketReady(resolveWebsocketReady, rejectWebsocketReady) {
    if (!this.#ws.readyState) {

      let count = 0;

      const id = window.setInterval(() => {
        if (!this.#ws.readyState && count < WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
          count += 1;
        } else {
          if (count === WEBSOCKET_READYSTATE_MAX_ATTEMPTS)
            rejectWebsocketReady("Maximum waiting time for websocket to be ready reached : " + WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS + "[ms] (_waitWebsocketReady)");
          else
            resolveWebsocketReady();
          window.clearInterval(id);
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);

    } else {
      resolveWebsocketReady();
    }
  }


  static _sendMessage(message) {

    // Check that the websocket connection is ready
    if (!this.#ws.readyState) {

      let promise = new Promise((resolveWebsocketReady, rejectWebsocketReady) => {
        this._waitWebsocketReady(resolveWebsocketReady, rejectWebsocketReady);
      });

      promise.then(
        () => this._sendMessage(message),
        (error) => console.error("(TODO)", error)
      );

    } else {
      // websocket ready to be used, message can be sent
      this.serverAnswer = null;     // reset server answer
      this.#ws.send(JSON.stringify(message));
    }
  }

}


