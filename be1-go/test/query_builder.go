package test

import (
	"encoding/base64"
	"strconv"
	"strings"
)

func cleanJSON(s string) string {
	s = strings.ReplaceAll(s, "\n", "")
	s = strings.ReplaceAll(s, "\t", "")
	s = strings.ReplaceAll(s, " ", "")
	return s
}

func encode(s string) string {
	return base64.StdEncoding.EncodeToString([]byte(s))
}

func createQuery(jsonrpc, method, params string, id int) string {
	return cleanJSON(`{
		"jsonrpc": "` + jsonrpc + `",
		"method": "` + method + `",
		"params": ` + params + `,
		"id": ` + strconv.Itoa(id) + `
		}`)
}

func createParams(channel, message string) string {
	return cleanJSON(`{
		"channel": "` + channel + `",
		"message": ` + message + `
		}`)
}

func createMessage(data, organizerPK, signature, messageID string) string {
	return cleanJSON(`{
		"data": "` + encode(data) + `",
		"sender": "` + organizerPK + `",
		"signature": "` + encode(signature) + `",
		"message_id": "` + encode(messageID) + `",
		"witness_signatures": []
		}`)
}

func createData(object, action, data string) string {
	return cleanJSON(`{
		"object": "` + object + `",
		"action": "` + action + `",
		` + data + `
		}`)
}

func createLaoData(laoID, name, creation, organizerPK string) string {

	data := `
		"id": "` + encode(laoID) + `",
		"name": "` + name + `",
		"creation": ` + creation + `,
		"organizer": "` + organizerPK + `",
		"witnesses": []
		`
	return createData("lao", "create", data)
}

func createSetUpElectionData(id, laoID, name, version, createdAt, startTime, endTime, questions string) string {

	data := `
		"id": "` + encode(laoID) + `",
		"lao": "` + laoID + `",
		"name": "` + name + `",
		"version": "` + version + `",
		"created_at": ` + createdAt + `,
		"start_time": ` + startTime + `,
		"end_time": ` + endTime + `,
		"questions": [` + questions + `]
		`
	return createData("election", "setup", data)
}

func createQuestion(id, questionName, votingMethod, ballotOptions string, writeIn bool) string {

	question := `{
		"id": "` + encode(id) + `", 
		"question": "` + questionName + `",
		"voting_method": "` + votingMethod + `",
		"ballot_options": [` + ballotOptions + `],
		"write_in": ` + strconv.FormatBool(writeIn) + `
		}`
	return question
}
