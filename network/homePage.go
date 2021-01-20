package network

// file to serve a webserver. Comes from the chat example of github.com/gorilla

import (
	"log"
	"net/http"
	"text/template"
)

// HomeHandler serves the http homepage
func HomeHandler(tpl *template.Template) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		err := tpl.Execute(w, r)
		if err != nil {
			log.Flags()
		}
	})
}
