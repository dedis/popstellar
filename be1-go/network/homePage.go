package network

// file to serve a webserver. Comes from the chat example of github.com/gorilla

import (
	"log"
	"net/http"
	"strconv"
	"student20_pop/config"
	"text/template"
)

// HomeHandler serves the http homepage
func HomeHandler(tpl *template.Template) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		err := tpl.Execute(w, config.ADDRESS+":"+strconv.Itoa(config.PORT))
		if err != nil {
			log.Flags()
		}
	})
}
