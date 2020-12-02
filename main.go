package main

import (
	"log"
	"net/http"
	"student20_pop/WebSocket"
	"text/template"
)

// this function basically makes the webserver run
func main() {

	/*path, err := os.Getwd()
	if err != nil {
		log.Println(err)
	}
	fmt.Println(path)

	flag.Parse()
	*/
	tpl := template.Must(template.ParseFiles("student20_pop/index.html"))
	/*
	flag.Parse()
	*/
	//tpl := template.Must(template.ParseFiles("index.html"))
	h := WebSocket.NewHub()
	router := http.NewServeMux()
	router.Handle("/", WebSocket.HomeHandler(tpl))
	router.Handle("/ws", WebSocket.NewWSHandler(h))
	log.Printf("serving on port 8080")
	log.Fatal(http.ListenAndServe(":8080", router)) //here to change the srv address

}
