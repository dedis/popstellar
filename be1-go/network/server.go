package network

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"student20_pop/hub"
	"student20_pop/network/socket"
	"sync"

	"github.com/gorilla/websocket"
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
	st  socket.SocketType
	srv *http.Server

	// used in tests
	Started chan struct{}
	Stopped chan struct{}

	wg *sync.WaitGroup
}

// NewServer creates a new Server which is used to handle requests for
// /<hubType>/<socketType> endpoint. Please use the Start() method to
// start listening for connections.
func NewServer(ctx context.Context, h hub.Hub, port int, st socket.SocketType, wg *sync.WaitGroup) *Server {
	server := &Server{
		ctx:     ctx,
		h:       h,
		st:      st,
		wg:      wg,
		Started: make(chan struct{}, 1),
		Stopped: make(chan struct{}, 1),
	}

	path := fmt.Sprintf("/%s/%s/", h.Type(), st)
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

		log.Printf("starting to listen at: %s", s.srv.Addr)
		s.Started <- struct{}{}
		err := s.srv.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			log.Fatalf("failed to start the server: %v", err)
		}

		s.Stopped <- struct{}{}
		log.Println("stopped the server...")
	}()
}

// ServeHTTP handles a websocket connection based on the socket type.
func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("failed to upgrade connection: %v", err)
		return
	}

	switch s.st {
	case socket.ClientSocketType:
		client := socket.NewClientSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg)

		go client.ReadPump(s.ctx)
		go client.WritePump(s.ctx)
	case socket.WitnessSocketType:
		witness := socket.NewWitnessSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg)

		go witness.ReadPump(s.ctx)
		go witness.WritePump(s.ctx)
	}
}

func (s *Server) Shutdown() error {
	return s.srv.Shutdown(s.ctx)
}
