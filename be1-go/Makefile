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
	# Answer
	cp -r ../protocol/examples/answer/. ./internal/handler/answer/manswer/testdata

	# Methods
	cp -r ../protocol/examples/query/broadcast/. ./internal/handler/method/broadcast/mbroadcast/testdata
	cp -r ../protocol/examples/query/catchup/. ./internal/handler/method/catchup/mcatchup/testdata
	cp -r ../protocol/examples/query/get_messages_by_id/. ./internal/handler/method/getmessagesbyid/mgetmessagesbyid/testdata
	cp -r ../protocol/examples/query/greet_server/. ./internal/handler/method/greetserver/mgreetserver/testdata
	cp -r ../protocol/examples/query/heartbeat/. ./internal/handler/method/heartbeat/mheartbeat/testdata
	cp -r ../protocol/examples/query/publish/. ./internal/handler/method/publish/mpublish/testdata
	cp -r ../protocol/examples/query/subscribe/. ./internal/handler/method/subscribe/msubscribe/testdata
	cp -r ../protocol/examples/query/unsubscribe/. ./internal/handler/method/unsubscribe/munsubscribe/testdata

	# Message
	cp -r ../protocol/examples/messageData/lao_create/. ./internal/handler/message/mmessage/testdata

	# MessageData

	# Authentification
	cp -r ../protocol/examples/messageData/popcha_authenticate/. ./internal/handler/messagedata/authentication/mauthentication/testdata

	# Chirp
	cp -r ../protocol/examples/messageData/chirp_add_publish/. ./internal/handler/messagedata/chirp/mchirp/testdata
	cp -r ../protocol/examples/messageData/chirp_delete_publish/. ./internal/handler/messagedata/chirp/mchirp/testdata
	cp -r ../protocol/examples/messageData/chirp_notify_add/. ./internal/handler/messagedata/chirp/mchirp/testdata
	cp -r ../protocol/examples/messageData/chirp_notify_delete/. ./internal/handler/messagedata/chirp/mchirp/testdata

	# Coin
	cp -r ../protocol/examples/messageData/coin/. ./internal/handler/messagedata/coin/mcoin/testdata

	# Consensus
	cp -r ../protocol/examples/messageData/consensus_accept/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_elect/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_elect_accept/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_failure/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_learn/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_prepare/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_promise/. ./internal/handler/messagedata/consensus/mconsensus/testdata
	cp -r ../protocol/examples/messageData/consensus_propose/. ./internal/handler/messagedata/consensus/mconsensus/testdata

	# Election
	cp -r ../protocol/examples/messageData/election_end/. ./internal/handler/messagedata/election/melection/testdata
	cp -r ../protocol/examples/messageData/election_key/. ./internal/handler/messagedata/election/melection/testdata
	cp -r ../protocol/examples/messageData/election_open/. ./internal/handler/messagedata/election/melection/testdata
	cp -r ../protocol/examples/messageData/election_setup/. ./internal/handler/messagedata/election/melection/testdata
	cp -r ../protocol/examples/messageData/election_result.json ./internal/handler/messagedata/election/melection/testdata
	cp -r ../protocol/examples/messageData/vote_cast_vote/. ./internal/handler/messagedata/election/melection/testdata


clean:
	rm -rf ./internal/validation/protocol

	# Answer
	rm -rf ./internal/handler/answer/manswer/testdata

	# Methods
	rm -rf ./internal/handler/method/broadcast/mbroadcast/testdata
	rm -rf ./internal/handler/method/catchup/mcatchup/testdata
	rm -rf ./internal/handler/method/getmessagesbyid/mgetmessagesbyid/testdata
	rm -rf ./internal/handler/method/greetserver/mgreetserver/testdata
	rm -rf ./internal/handler/method/heartbeat/mheartbeat/testdata
	rm -rf ./internal/handler/method/publish/mpublish/testdata
	rm -rf ./internal/handler/method/subscribe/msubscribe/testdata
	rm -rf ./internal/handler/method/unsubscribe/munsubscribe/testdata

	# Message
	rm -rf ./internal/handler/message/mmessage/testdata

	# MessageData

	# Authentication
	rm -rf ./internal/handler/messagedata/authentication/mauthentication/testdata

	# Chirp
	rm -rf ./internal/handler/messagedata/chirp/mchirp/testdata

	# Coin
	rm -rf ./internal/handler/messagedata/coin/mcoin/testdata

	# Consensus
	rm -rf ./internal/handler/messagedata/consensus/mconsensus/testdata

	# Election
	rm -rf ./internal/handler/messagedata/election/melection/testdata

fmt:
	gofmt -s -w ./

check-fmt:
	gofmt -d -e -s ./