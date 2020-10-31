import React from 'react';
import { w3cwebsocket as W3CWebSocket } from "websocket";


const TIMEOUT_MS = 25;
const MAX_ATTEMPTS = 80;

export default class WebsocketLink {

  static #ws;
  static serverAnswer;


  static printServerAnswer() { console.log(this.serverAnswer); }

  static sendMessageToServer(message) {
    if (this.#ws == null)
      WebsocketLink._initWebsocket();

    WebsocketLink._sendMessage(message);
  }

  static waitServerAnswer(callback, count = 0) {

    if (this.serverAnswer == null && count < MAX_ATTEMPTS) {
      window.setTimeout(() => this.waitServerAnswer(callback,count + 1), TIMEOUT_MS);

    } else {
      if (count === MAX_ATTEMPTS) {
        console.error("(TODO) MAXIMUM ATTEMPTS REACHED : " + MAX_ATTEMPTS + " (waitServerAnswer)");
      } else {
        console.log("answer at count = " + count +  " :", this.serverAnswer);
        callback();
      }
    }
  }


  static _initWebsocket(address = '127.0.0.1', port =  '8000') {
    console.log("initiating web socket : " + 'ws://' + address + ':' + port);
    const ws = new W3CWebSocket('ws://' + address + ':' + port);


    ws.onopen = () => { };

    ws.onmessage = (message) => {
      const data = JSON.parse(message.data);
      //console.log('we got a reply from server (comm) : ', data);
      //this.answerQueue.push(data);
      this.serverAnswer = data;
    };

    this.#ws = ws;
    this.serverAnswer = null;
  }

  static _waitWebsocketReady(message, count = 0) {

    if (!this.#ws.readyState && count < MAX_ATTEMPTS) {
      window.setTimeout(() => this._waitWebsocketReady(message, count + 1), TIMEOUT_MS);

    } else {
      if (count === MAX_ATTEMPTS) {
        console.error("(TODO) MAXIMUM ATTEMPTS REACHED : " + MAX_ATTEMPTS + " (_waitWebsocketReady)");
      } else {
        this._sendMessage(message);
      }
    }
  }

  static _sendMessage(message) {

    // Check that the websocket connection is ready
    if (!this.#ws.readyState) {
      this._waitWebsocketReady(message);
    } else {
      this.serverAnswer = null;
      this.#ws.send(JSON.stringify({
        msg: message
      }));

    }
  }

}


