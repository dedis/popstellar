# Witnessing

Should provide the witness functionality. Currently (01-07-2022) only supports
witness messages that can be turned on using the `enabled` feature toggle.
Supports "passive" witnessing where `message#witness` messages are automatically
sent and "active" witnessing where the message first has to be manually
confirmed by the user. The file `network/messages/WitnessRegistry.ts` contains
the configuration which messages require what type of witnessing.

## Dependencies

- Basic LAO functionality provided by the `lao` feature
- Notification functionality to show prompts to witnesses in case they need to
  manually confirm whether they want to witness a given message

See `interface/Configuration.ts` for more details.
