version=$(shell git describe --abbrev=0 --tags || echo '0.0.0')
versionFlag="popstellar.Version=$(version)"
versionFile=$(shell echo $(version) | tr . _)
timeFlag="popstellar.BuildTime=$(shell date +'%d/%m/%y_%H:%M')"
shortsha=$(shell git rev-parse --short HEAD)
shaFlag="popstellar.ShortSHA=$(shortsha)"

.PHONY: test

build: export CGO_ENABLED=0
build: protocol test-data
	go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop ./cli
	GOOS=linux GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-linux-amd64-$(versionFile) ./cli
	GOOS=darwin GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-darwin-amd64-$(versionFile) ./cli
	GOOS=darwin GOARCH=arm64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-darwin-arm64-$(versionFile) ./cli
	GOOS=windows GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-windows-amd64-$(versionFile) ./cli

lint:
	@go install honnef.co/go/tools/cmd/staticcheck@latest
	@go mod tidy
	staticcheck ./...

test: protocol test-data
	go test -v -race ./...

test-no-cache: protocol test-data
	go test -v -race ./... -count=1

test-cov: protocol test-data
	go test -v ./... -coverprofile=coverage.out -json > report.json

vet: protocol test-data
	go vet ./...

check: test test-cov lint vet

protocol:
	cp -r ../protocol ./internal/validation

test-data:
	cp -r ../protocol/examples/answer/. ./internal/handler/answer/manswer/test-data

	cp -r ../protocol/examples/query/broadcast/. ./internal/handler/method/broadcast/mbroadcast/test-data

	cp -r ../protocol/examples/query/catchup/. ./internal/handler/method/catchup/mcatchup/test-data

clean:
	rm -rf ./internal/validation/protocol
	rm -rf ./internal/handler/answer/manswer/test-data

fmt:
	gofmt -s -w ./

check-fmt:
	gofmt -d -e -s ./