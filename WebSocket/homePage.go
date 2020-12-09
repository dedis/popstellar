/* file to serve a webserver. Comes from the chat example of github.com/gorilla */
package WebSocket

import (
	"net/http"
	"text/template"
)

// Serves the http homepage
func HomeHandler(tpl *template.Template) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_ = tpl.Execute(w, r)
	})
}
