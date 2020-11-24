package main

import (
	"student20_pop/WebSocket"
	"text/template"

	"flag"
	"github.com/boltdb/bolt"
	"log"
	"net/http"
)

// this function basically makes the webserver run
func main() {
	db, err1 := bolt.Open("test.db", 0600, nil)
	if err1 != nil {
		log.Fatal(err1)
	}
	defer db.Close()

	flag.Parse()
	tpl := template.Must(template.ParseFiles("index.html"))
	h := WebSocket.NewHub()
	router := http.NewServeMux()
	router.Handle("/", WebSocket.HomeHandler(tpl))
	router.Handle("/ws", WebSocket.NewWSHandler(h))
	log.Printf("serving on port 8080")
	log.Fatal(http.ListenAndServe(":8080", router)) //here to change the srv address
}
