/* file that serves a webpage. Comes from the chat example of github.com/websocket with minor changes */

package WebSocket

import (
	"net/http"
	"text/template"
)

// Serves the http homepage
func HomeHandler(tpl *template.Template) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tpl.Execute(w, r)
	})
}
