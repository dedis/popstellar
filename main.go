/*makes the server run. Insipired from the chat example of github.com/gorilla/websocket */
package main

import (
	"flag"
	"os"
	"strings"
	"student20_pop/network"
	"text/template"

	"log"
	"net/http"
)

// this function basically makes the webserver run
func main() {

	mode := os.Args[1]
	flag.Parse()
	tpl := template.Must(template.ParseFiles("index.html"))

	switch strings.ToLower(mode) {
	case "o":
		h := network.NewOrganizerHub(os.Args[3], os.Args[4])
		router := http.NewServeMux()
		router.Handle("/", network.HomeHandler(tpl))
		router.Handle("/ws", network.NewWSHandler(h))
		log.Printf("serving organizer on address " + os.Args[2])
		log.Fatal(http.ListenAndServe(os.Args[2], router)) //here to change the srv address

	case "w":
		h := network.NewWitnessHub(os.Args[3], os.Args[4])
		router := http.NewServeMux()
		router.Handle("/", network.HomeHandler(tpl))
		router.Handle("/ws", network.NewWSHandler(h))
		log.Printf("serving witness on adress " + os.Args[2])
		log.Fatal(http.ListenAndServe(os.Args[2], router)) //here to change the srv address

	}

}
