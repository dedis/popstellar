/*makes the server run. Inspired from the chat example of github.com/gorilla/websocket */
package main

import (
	"log"
	"net/http"
	"strconv"
	"strings"
	"student20_pop/config"
	"student20_pop/network"
	"text/template"
)

// this function basically makes the webserver run
func main() {

	tpl := template.Must(template.ParseFiles("test/index.html"))

	if strings.ToLower(config.MODE) != "o" && strings.ToLower(config.MODE) != "w" {
		log.Fatal("Mode not recognized")
	}

	var file string
	if config.FILE == "default" {
		switch strings.ToLower(config.MODE) {
		case "o":
			file = "org.db"
		case "w":
			file = "wit.db"
		default:
			log.Fatal("Mode not recognized")
		}
	} else if !strings.HasSuffix(file, ".db") {
		file = config.FILE
		log.Fatal("File for the Actor's database must end with \".db\" ")
	}

	switch strings.ToLower(config.MODE) {
	case "o":
		h := network.NewOrganizerHub(config.PKEY, file)
		router := http.NewServeMux()
		router.Handle("/test", network.HomeHandler(tpl))
		router.Handle("/", network.NewWSHandler(h))
		log.Printf("serving organizer on address " + config.ADDRESS + ":" + strconv.Itoa(config.PORT))
		log.Fatal(http.ListenAndServe(config.ADDRESS+":"+strconv.Itoa(config.PORT), router))

	case "w":
		h := network.NewWitnessHub(config.PKEY, file)
		router := http.NewServeMux()
		router.Handle("/test", network.HomeHandler(tpl))
		router.Handle("/", network.NewWSHandler(h))
		log.Printf("serving witness on address " + config.ADDRESS + ":" + strconv.Itoa(config.PORT))
		log.Fatal(http.ListenAndServe(config.ADDRESS+":"+strconv.Itoa(config.PORT), router))

	}

}
