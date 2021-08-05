package network

import (
	"context"
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"net/http"
	"student20_pop/hub"
	"sync"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

// CreateAndServeWS creates a new http.Server which handles requests on
// /<hubType>/<socketType> endpoint. It spawns a new go routine which
// listens for connections.
func CreateAndServeWS(ctx context.Context, hubType hub.HubType, socketType hub.SocketType, h hub.Hub, port int, wg *sync.WaitGroup) *http.Server {
	wg.Add(1)
	srv := &http.Server{Addr: fmt.Sprintf(":%d", port)}

	path := fmt.Sprintf("/%s/%s/", hubType, socketType)
	http.HandleFunc(path, func(w http.ResponseWriter, r *http.Request) {
		serveWs(ctx, socketType, h, w, r, wg)
	})

	log.Printf("Starting the %s WS server (for %s) at %d", hubType, socketType, port)
	go func() {
		defer wg.Done()

		var err = srv.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			log.Fatalf("failed to start the server: %v", err)
		}

		log.Println("stopped the server...")
	}()

	return srv
}

// serveWs handles a websocket connection based on the socket type.
func serveWs(ctx context.Context, socketType hub.SocketType, h hub.Hub, w http.ResponseWriter, r *http.Request, wg *sync.WaitGroup) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("failed to upgrade connection: %v", err)
		return
	}

	switch socketType {
	case hub.ClientSocketType:
		client := hub.NewClientSocket(h, conn, wg)

		go client.ReadPump(ctx)
		go client.WritePump(ctx)

		// cleanup go routine that removes clients that forgot to unsubscribe
		go func(c *hub.ClientSocket, h hub.Hub) {
			c.Wait.Wait()
			h.RemoveClientSocket(c)
		}(client, h)
	case hub.WitnessSocketType:
		witness := hub.NewWitnessSocket(h, conn, wg)

		go witness.ReadPump(ctx)
		go witness.WritePump(ctx)
	}
}
