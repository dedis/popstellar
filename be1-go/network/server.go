package network

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"student20_pop/concurrent"
	"student20_pop/hub"
	"student20_pop/network/socket"

	"github.com/gorilla/websocket"
	"golang.org/x/xerrors"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

// Server represents a Websocket Server for an organizer or a witness
// and it may listen to requests from organizer, witness or an attendee.
type Server struct {
	h   hub.Hub
	st  socket.SocketType
	srv *http.Server

	Started chan struct{}
	Stopped chan struct{}

	wg   concurrent.WaitGroup
	done chan struct{}
}

// NewServer creates a new Server which is used to handle requests for
// /<hubType>/<socketType> endpoint. Please use the Start() method to
// start listening for connections.
func NewServer(h hub.Hub, port int, st socket.SocketType) *Server {
	server := &Server{
		h:       h,
		st:      st,
		Started: make(chan struct{}, 1),
		Stopped: make(chan struct{}, 1),
		// The use of a `sync.WaitGroup` will cause a data race on Add and Wait
		// since ServeHTTP and Shutdown may be concurrent. Refer to
		// `TestConnectToWitnessSocket`.
		wg:   concurrent.NewRendezvous(),
		done: make(chan struct{}),
	}

	path := fmt.Sprintf("/%s/%s/", h.Type(), st)
	mux := http.NewServeMux()
	mux.HandleFunc(path, server.ServeHTTP)

	httpServer := &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: mux,
	}

	server.srv = httpServer
	return server
}

// Start spawns a new go-routine which invokes ListenAndServe on the http.Server
func (s *Server) Start() {
	go func() {
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
		client := socket.NewClientSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg, s.done)
		s.wg.Add(2)

		go client.ReadPump()
		go client.WritePump()
	case socket.WitnessSocketType:
		witness := socket.NewWitnessSocket(s.h.Receiver(), s.h.OnSocketClose(), conn, s.wg, s.done)
		s.wg.Add(2)

		go witness.ReadPump()
		go witness.WritePump()
	}
}

// Shutdown shuts the HTTP server, signals the read and write pumps to
// close and waits for them to finish.
func (s *Server) Shutdown() error {
	err := s.srv.Shutdown(context.Background())
	if err != nil {
		return xerrors.Errorf("failed to shutdown server: %v", err)
	}

	close(s.done)

	s.wg.Wait()
	return nil
}
