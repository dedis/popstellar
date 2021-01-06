/*makes the server run. Insipired from the chat example of github.com/gorilla/websocket */
package main

import (
	"flag"
	"strconv"
	"strings"
	"student20_pop/network"
	"text/template"

	"log"
	"net/http"
)

// this function basically makes the webserver run
func main() {

	var mode = flag.String("m", "o", "server mode")
	var address = flag.String("a", "", "IP on which to run the server")
	var port = flag.Int("p", 8080, "port on which the server listens for websocket connections")
	var pkey = flag.String("k", "oui", "actor's public key")
	var file = flag.String("f", "default", "file for the actor to store it's database. Must end with \".db\" ")

	flag.Parse()
	tpl := template.Must(template.ParseFiles("index.html"))

	if strings.ToLower(*mode) != "o" && strings.ToLower(*mode) != "w" {
		log.Fatal("Mode not recognized")
	}

	if *file == "default" {
		switch strings.ToLower(*mode) {
		case "o":
			*file = "org.db"
		case "w":
			*file = "wit.db"
		default:
			log.Fatal("Mode not recognized")
		}
	} else if !strings.HasSuffix(*file, ".db") {
		log.Fatal("File for the Actor's database must end with \".db\" ")
	}

	switch strings.ToLower(*mode) {
	case "o":
		h := network.NewOrganizerHub(*pkey, *file)
		router := http.NewServeMux()
		router.Handle("/", network.HomeHandler(tpl))
		router.Handle("/ws", network.NewWSHandler(h))
		log.Printf("serving organizer on address " + *address + ":" + strconv.Itoa(*port))
		log.Fatal(http.ListenAndServe(*address+":"+strconv.Itoa(*port), router)) //here to change the srv address

	case "w":
		h := network.NewWitnessHub(*pkey, *file)
		router := http.NewServeMux()
		router.Handle("/", network.HomeHandler(tpl))
		router.Handle("/ws", network.NewWSHandler(h))
		log.Printf("serving witness on adress " + *address + ":" + strconv.Itoa(*port))
		log.Fatal(http.ListenAndServe(*address+":"+strconv.Itoa(*port), router)) //here to change the srv address

	}

}
