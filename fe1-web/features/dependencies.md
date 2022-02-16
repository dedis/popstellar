# Feature dependencies

Here is the list of dependency between features that will need to be resolved in
the future.

## Social media

- AddChirp : `store/reducers/__tests__/MessageReducer.test.ts`,
`model/network/method/message/data/__tests__/MessageRegistry.test.ts`,
`model/network/method/message/__tests__/Message.test.ts` (only tests)
- ChirpHandler : `ingestion/handlers/index.ts`
- SocialHandler : `ingestion/handlers/index.ts`
- SocialNavigation : `navigation/bars/LaoNavigation.tsx`,
- SocialReducer : `store/reducers/index.ts`, `store/reducers/RootReducer.ts`

## Meeting

- EventMeeting : `components/eventList/events/Event.tsx`
- Meeting : `model/objects/LaoEventBuilder.ts`, `model/objects/__tests__/LaoEventBuilder.test.ts`,
`store/reducers/__tests__/EventsReducer.test.ts`
- MeetingHandler : `ingestion/handlers/index.ts`
- Storage is done in EventsReducer/EventsStore and could be done separately in a MeetingsReducer
