package network

// file to serve a webserver. Comes from the chat example of github.com/gorilla

import (
	"log"
	"net/http"
	"strconv"
	"text/template"
)

// HomeHandler serves the http homepage. Set the websocket connection address to address:port
func HomeHandler(tpl *template.Template, address *string, port *int) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		err := tpl.Execute(w, *address+":"+strconv.Itoa(*port))
		if err != nil {
			log.Flags()
		}
	})
}
