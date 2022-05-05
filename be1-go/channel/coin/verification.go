package coin

import (
	"popstellar/message/messagedata"
)

const (
	laoIDBase64   = "lao id is %s, should be base64URL encoded"
	elecIDBase64  = "election id is %s, should be base64URL encoded"
	elecIDFormat  = "election channel id is %s, should be formatted as /root/laoID/electionID"
	laoIDCompare  = "lao id is %s, should be %s"
	elecIDCompare = "election id is %s, should be %s"
)

func (c *Channel) verifyMessageTransactionPost(transactionPost messagedata.TransactionPost) error {
	c.log.Info().Msgf("verifying transaction#post message transaction_id validity %s",
		transactionPost.TransactionId)

	/*

			https://github.com/dedis/popstellar/pull/972/files DOCS PR LOUIS
		https://docs.google.com/document/d/1TTDlH0zlUdegtx3EXuR9a-GiDM8JJvTWbMTVSi-75vo/edit# FILE DRIVE


				// verify transaction_id is base64URL encoded
				_, err := base64.URLEncoding.DecodeString(transactionPost.TransactionId)
				if err != nil {
					return xerrors.Errorf(laoIDBase64, electionOpen.Lao)
				}
	*/

	/*
	  lazy val transaction_id = {
	    val temp = TxOut.foldRight(List(Version.toString)) { (txout, acc) => txout.Script.PubkeyHash.base64Data.toString :: txout.Script.Type :: txout.Value.toString :: acc }
	    Hash.fromStrings(
	    LockTime.toString :: TxIn.foldRight(temp) { (txin, acc) => txin.Script.Pubkey.base64Data.toString :: txin.Script.Sig.toString :: txin.Script.Type :: txin.TxOutHash.toString :: txin.TxOutIndex.toString :: acc }
	      : _*)
	  }
	*/
	/*
		base64.URLEncoding.
			// verify election id is base64URL encoded
			_, err = base64.URLEncoding.DecodeString(electionOpen.Election)
		if err != nil {
			return xerrors.Errorf(elecIDBase64, electionOpen.Election)
		}

		// split channel to [lao id, election id]
		noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")

		IDs := strings.Split(noRoot, "/")
		if len(IDs) != 2 {
			return xerrors.Errorf(elecIDFormat, c.channelID)
		}

		laoID := IDs[0]
		electionID := IDs[1]

		// verify if lao id is the same as the channel
		if electionOpen.Lao != laoID {
			return xerrors.Errorf(laoIDCompare, laoID, electionOpen.Lao)
		}

		// verify if election id is the same as the channel
		if electionOpen.Election != electionID {
			return xerrors.Errorf(elecIDCompare, electionID, electionOpen.Election)
		}

		// verify opened at is positive
		if electionOpen.OpenedAt < 0 {
			return xerrors.Errorf("election open created at is %d, should be minimum 0",
				electionOpen.OpenedAt)
		}

		// verify if the election was already started or terminated
		if c.started || c.terminated {
			return xerrors.Errorf("election was already started or terminated")
		}

	*/
	return nil
}
