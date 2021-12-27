.PHONY: test

build: protocol
	go build -o pop ./cli

lint:
	@go get -v honnef.co/go/tools/cmd/staticcheck
	@go mod tidy
	staticcheck ./...

test: protocol
	go test -v -race -timeout 15s ./...

test-cov: protocol
	go test -v -coverpkg=./... -coverprofile=coverage.out ./... -json > report.json

vet: protocol
	go vet ./...

check: test test-cov lint vet

protocol:
	cp -r ../protocol ./validation 

clean:
	rm -rf validation/protocol

fmt:
	gofmt -s -w ./

check-fmt:
	gofmt -d -e -s ./