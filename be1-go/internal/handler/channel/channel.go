package channel

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
)

const (
	RootType         = "root"
	LaoType          = "lao"
	ElectionType     = "election"
	ChirpType        = "chirp"
	ReactionType     = "reaction"
	ConsensusType    = "consensus"
	CoinType         = "coin"
	AuthType         = "auth"
	PopChaType       = "popcha"
	GeneralChirpType = "generalChirp"
	FederationType   = "federation"
)

type channelHandler struct {
	db     repository.ChannelRepository
	schema *validation.SchemaValidator

	root       *rootHandler
	lao        *laoHandler
	election   *electionHandler
	chirp      *chirpHandler
	reaction   *reactionHandler
	coin       *coinHandler
	federation *federationHandler
}

func createChannelHandler(conf repository.ConfigManager, subs repository.SubscriptionManager,
	socket repository.SocketManager, db repository.Repository, hub repository.HubManager, schema *validation.SchemaValidator) *channelHandler {
	root := createRootHandler(conf, db, schema)
	lao := createLaoHandler(conf, subs, db, schema)
	election := createElectionHandler(conf, subs, db, schema)
	chirp := createChripHandler(conf, subs, db, schema)
	reaction := createReactionHandler(subs, db, schema)
	coin := createCoinHandler(subs, db, schema)
	federation := createFederationHandler(db, subs, socket, hub, schema)

	return &channelHandler{
		db:         db,
		schema:     schema,
		root:       root,
		lao:        lao,
		election:   election,
		chirp:      chirp,
		reaction:   reaction,
		coin:       coin,
		federation: federation,
	}
}

func (c *channelHandler) Handle(channelPath string, msg message.Message, fromRumor bool) error {
	err := msg.VerifyMessage()
	if err != nil {
		return err
	}

	msgAlreadyExists, err := c.db.HasMessage(msg.MessageID)
	if err != nil {
		return err
	}
	if msgAlreadyExists && fromRumor {
		return nil
	}
	if msgAlreadyExists {
		return errors.NewDuplicateResourceError("message %s was already received", msg.MessageID)
	}

	channelType, err := c.db.GetChannelType(channelPath)
	if err != nil {
		return err
	}

	switch channelType {
	case RootType:
		err = c.root.handleChannelRoot(msg)
	case LaoType:
		err = c.lao.handle(channelPath, msg)
	case ElectionType:
		err = c.election.handle(channelPath, msg)
	case ChirpType:
		err = c.chirp.handle(channelPath, msg)
	case ReactionType:
		err = c.reaction.handle(channelPath, msg)
	case CoinType:
		err = c.coin.handle(channelPath, msg)
	case FederationType:
		err = c.federation.handleChannelFederation(channelPath, msg)
	default:
		err = errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}

	return err
}

// util for the channels

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}
