# Meeting

Provides support for the lao event type `meeting` that was added as a
proof-of-concept. It supports the creation of meetings and has code that was
added for supporting the `meeting#state` message which updates certain
properties. The latter message was not tested in the spring semester 2022 as the
code includes checks that require working consensus functionality which was
turned off before the semester started.

## Dependencies

- Basic LAO functionality provided by the `lao` feature
- LAO Events support provided by the `events` feature

See `interface/Configuration.ts` for more details.
