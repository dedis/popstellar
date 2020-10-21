package main

import (
	"flag"
	"github.com/boltdb/bolt"
	"log"
	"net/http"
	"text/template"
)

func homeHandler(tpl *template.Template) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tpl.Execute(w, r)
	})
}

func main() {

	//initiating the database
	db, err1 := bolt.Open("test.db", 0600, nil)
	if err1 != nil {
		log.Fatal(err1)
	}
	defer db.Close()

	flag.Parse()
	tpl := template.Must(template.ParseFiles("index.html"))
	h := newHub()
	router := http.NewServeMux()
	router.Handle("/", homeHandler(tpl))
	router.Handle("/ws", wsHandler{h: h, database: db})
	log.Printf("serving on port 8080")
	log.Fatal(http.ListenAndServe(":8080", router))
}
