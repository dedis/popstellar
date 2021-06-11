.PHONY: test

build: protocol
	go build -o pop ./cli/

lint:
	@go get -v honnef.co/go/tools/cmd/staticcheck
	@go mod tidy
	staticcheck ./...

check: protocol lint vet
	go test -v ./...
	go test -v -race ./...

vet: protocol
	go vet ./...

protocol:
	cp -r ../protocol ./validation 

clean:
	rm -rf validation/protocol
