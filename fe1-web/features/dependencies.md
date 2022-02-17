# Feature dependencies

Here is the list of dependency between features that will need to be resolved in
the future.

## Social media

- AddChirp : `store/reducers/__tests__/MessageReducer.test.ts`,
`model/network/method/message/data/__tests__/MessageRegistry.test.ts`,
`model/network/method/message/__tests__/Message.test.ts` (only tests)
- ChirpHandler : `ingestion/handlers/index.ts`
- SocialHandler : `ingestion/handlers/index.ts`
- SocialNavigation : `navigation/bars/LaoNavigation.tsx`
- SocialReducer : `store/reducers/RootReducer.ts`

## Meeting

- EventMeeting : `features/events/components/Event.tsx`
- Meeting : `features/events/objects/LaoEventBuilder.ts`, `features/events/objects/__tests__/LaoEventBuilder.test.ts`
`features/events/reducer/__tests__/EventsReducer.test.ts`
- MeetingHandler : `ingestion/handlers/index.ts`
- CreateMeeting : `navigation/bars/organizer/OrganizerNavigation.tsx`
- Storage is done in EventsReducer/EventsStore and could be done separately in a MeetingsReducer

## E-voting (Election)

- EndElection : `model/network/method/message/__tests__/Message.test.ts`
- EventElection : `features/events/components/Event.tsx`
- Election : `features/events/objects/LaoEventBuilder.ts`, `features/events/objects/__tests__/LaoEventBuilder.test.ts`
- ElectionHandler : `ingestion/handlers/index.ts`
- CreateElection : `navigation/bars/organizer/OrganizerNavigation.tsx`
- Storage is done in EventsReducer/EventsStore and could be done separately in an ElectionsReducer

## Roll Call

- EventRollCall : `features/events/components/Event.tsx`
- RollCall : `features/events/objects/LaoEventBuilder.ts`, `features/events/objects/__tests__/LaoEventBuilder.test.ts`,
`model/objects/wallet/Token.ts`, `model/objects/wallet/Wallet.ts`, `features/events/reducer/EventsReducer.ts`
`features/events/reducer/__tests__/EventsReducer.test.ts`, `parts/wallet/WalletSyncedSeed.tsx`,
`features/social/navigation/SocialMediaNavigation.tsx`
- RollCallHandler : `ingestion/handlers/index.ts`
- CreateRollCall : `navigation/bars/organizer/OrganizerNavigation.tsx`
- OpenedRollCall : `navigation/bars/organizer/OrganizerNavigation.tsx`

## Events

- CreateEvent : `navigation/bars/organizer/OrganizerNavigation.tsx`
- LaoEvent : `model/objects/wallet/Wallet.ts`, `parts/wallet/WalletSyncedSeed.tsx`,
`features/evoting/objects/Election.ts`, `features/meeting/objects/Meeting.ts`, `features/rollCall/objects/RollCall.ts`
- EventHandlerUtils : `features/evoting/network/ElectionHandler.ts`, `features/rollCall/network/RollCallHandler.ts`,
`features/meeting/network/MeetingHandler.ts`, `features/evoting/components/EventElection.tsx`
- EventsReducer : `store/reducers/RootReducer.ts`, `features/lao/screens/AttendeeScreen.tsx`
`features/social/screens/SocialSearch.tsx`, `parts/wallet/WalletSyncedSeed.tsx`,
`features/rollCall/network/RollCallHandler.ts`, `features/meeting/network/MeetingHandler.ts`,
`features/evoting/network/ElectionHandler.ts`, `features/evoting/components/EventElection.tsx`,
`model/objects/wallet/Wallet.ts`, `features/social/navigation/SocialMediaNavigation.tsx`,
`features/lao/screens/WitnessScreen.tsx`
- EventStore : `model/objects/wallet/Token.ts`,

## Lao

- AttendeeScreen : `navigation/bars/LaoNavigation.tsx`
- OrganizerScreen : `navigation/bars/organizer/OrganizerNavigation.tsx`
- WitnessScreen : `navigation/bars/witness/WitnessNavigation.tsx`

## Wallet

- generateToken : `features/social/navigation/SocialMediaNavigation.tsx`
- Token : `model/network/method/message/Message.ts`
- Wallet objects : `features/rollCall/components/EventRollCall.tsx`, `features/rollCall/network/RollCallHandler.ts`,
`features/rollCall/screens/RollCallOpened.tsx`
- WalletNavigation : `navigation/bars/LaoNavigation.tsx`, `navigation/bars/MainNavigation.tsx`
- WalletReducer : `store/reducers/RootReducer.ts`, `store/__mocks__/Storage.ts`
