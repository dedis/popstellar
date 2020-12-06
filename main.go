/*makes the server run. Insipired from the chat example of github.com/gorilla/websocket */
package main

import (
	"student20_pop/WebSocket"
	"text/template"

	"flag"
	"log"
	"net/http"
)

// this function basically makes the webserver run
func main() {

	flag.Parse()
	tpl := template.Must(template.ParseFiles("index.html"))
	h := WebSocket.NewHub()
	router := http.NewServeMux()
	router.Handle("/", WebSocket.HomeHandler(tpl))
	router.Handle("/ws", WebSocket.NewWSHandler(h))
	log.Printf("serving on port 8080")
	log.Fatal(http.ListenAndServe(":8080", router)) //here to change the srv address

}
