package hub

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func CreateAndServeWS(ctx context.Context, hubType HubType, socketType SocketType, h Hub, port int, wg *sync.WaitGroup) *http.Server {
	wg.Add(1)
	srv := &http.Server{Addr: fmt.Sprintf(":%d", port)}

	path := fmt.Sprintf("/%s/%s/", hubType, socketType)
	log.Printf("handling: %s", path)
	http.HandleFunc(path, func(w http.ResponseWriter, r *http.Request) {
		log.Println("In here")
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

func serveWs(ctx context.Context, socketType SocketType, h Hub, w http.ResponseWriter, r *http.Request, wg *sync.WaitGroup) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("failed to upgrade connection: %v", err)
		return
	}

	switch socketType {
	case ClientSocketType:
		client := NewClientSocket(h, conn, wg)

		go client.ReadPump(ctx)
		go client.WritePump(ctx)

		// cleanup go routine that removes clients that forgot to unsubscribe
		go func(c *ClientSocket, h Hub) {
			c.Wait.Wait()
			h.RemoveClientSocket(c)
		}(client, h)
	case WitnessSocketType:
		witness := NewWitnessSocket(h, conn, wg)

		go witness.ReadPump(ctx)
		go witness.WritePump(ctx)
	}
}
