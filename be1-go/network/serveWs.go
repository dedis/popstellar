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

// Server represents a Websocket Server for an organizer or a witness
// and it may listen to requests from organizer, witness or an attendee.
type Server struct {
	ctx context.Context

	h   hub.Hub
	st  hub.SocketType
	srv *http.Server

	wg *sync.WaitGroup
}

// NewServer creates a new Server which is used to handle requests for
// /<hubType>/<socketType> endpoint. Please use the Start() method to
// start listening for connections.
func NewServer(ctx context.Context, h hub.Hub, port int, st hub.SocketType, wg *sync.WaitGroup) *Server {
	server := &Server{
		ctx: ctx,
		h:   h,
		st:  st,
		wg:  wg,
	}

	path := fmt.Sprintf("/%s/%s", h.Type(), st)
	mux := http.NewServeMux()
	mux.Handle(path, server)

	httpServer := &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: mux,
	}

	server.srv = httpServer
	return server
}

// Start spawns a new go-routine which invokes ListenAndServe on the http.Server
func (s *Server) Start() {
	s.wg.Add(1)
	go func() {
		defer s.wg.Done()

		err := s.srv.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			log.Fatalf("failed to start the server: %v", err)
		}

		log.Println("stopped the server...")
	}()
}

// serveWs handles a websocket connection based on the socket type.
func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("failed to upgrade connection: %v", err)
		return
	}

	switch s.st {
	case hub.ClientSocketType:
		client := hub.NewClientSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg)

		go client.ReadPump(s.ctx)
		go client.WritePump(s.ctx)
	case hub.WitnessSocketType:
		witness := hub.NewWitnessSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg)

		go witness.ReadPump(s.ctx)
		go witness.WritePump(s.ctx)
	}
}
