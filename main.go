package main

import (
	"flag"
	"os"
	"strings"
	"student20_pop/WebSocket"
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
		h := WebSocket.NewOrganizerHub(os.Args[3])
		router := http.NewServeMux()
		router.Handle("/", WebSocket.HomeHandler(tpl))
		router.Handle("/ws", WebSocket.NewWSHandler(h))
		log.Printf("serving organizer on address " + os.Args[2])
		log.Fatal(http.ListenAndServe(os.Args[2], router)) //here to change the srv address

	case "w":
		h := WebSocket.NewWitnessHub(os.Args[3])
		router := http.NewServeMux()
		router.Handle("/", WebSocket.HomeHandler(tpl))
		router.Handle("/ws", WebSocket.NewWSHandler(h))
		log.Printf("serving witness on adress " + os.Args[2])
		log.Fatal(http.ListenAndServe(os.Args[2], router)) //here to change the srv address

	}

}
