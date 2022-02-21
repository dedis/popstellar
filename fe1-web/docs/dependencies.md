### Warning: this is a working document, partially superseded by the introduction of dependency-cruiser.

If you don't know what this is about, it's probably safe for you to ignore it :-)

-----


# Feature dependencies

Here is the list of dependency between features that will need to be resolved in
the future. Files might have moved since, but their name should be identical

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
- Election : `features/events/objects/LaoEventBuilder.ts`, `features/events/objects/__tests__/LaoEventBuilder.test.ts`,
  `src/core/components/BarChartDisplay.tsx`
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
- LaoHandler : `ingestion/handlers/index.ts`
- LaoNavigation : `src/navigation/AppNavigation.tsx`
- LAOItem : `src/parts/Home.tsx`
- LaoReducer : `src/features/events/reducer/EventsReducer.ts`, `src/features/social/reducer/SocialReducer.ts`,
  `src/store/reducers/MessageReducer.ts`, `src/features/events/components/Event.tsx`,
  `src/features/evoting/network/ElectionHandler.ts`, `src/features/meeting/network/MeetingHandler.ts`,
  `src/features/rollCall/network/RollCallHandler.ts`, `src/features/rollCall/components/EventRollCall.tsx`,
  `src/features/rollCall/screens/RollCallOpened.tsx`, `src/features/social/navigation/SocialMediaNavigation.tsx`,
  `src/features/social/network/ChirpHandler.ts`, `src/features/social/network/ReactionHandler.ts`,
  `src/features/social/reducer/SocialReducer.ts`, `src/features/wallet/screens/WalletSyncedSeed.tsx`,
  `src/features/witness/screens/WitnessScreen.tsx`, `src/ingestion/Watcher.ts`, `src/parts/Home.tsx`,
  `src/parts/Launch.tsx`, `src/features/social/screens/SocialSearch.tsx`
- OpenedLaoStore : `src/features/evoting/network/messages/__tests__/SetupElection.test.ts`,
  `src/features/evoting/network/messages/SetupElection.ts`, `src/features/evoting/network/ElectionMessageApi.ts`,
  `src/features/evoting/screens/CreateElection.tsx`, `src/features/meeting/network/__tests__/MeetingMessageApi.test.ts`,
  `src/features/meeting/network/messages/__tests__/CreateMeeting.test.ts`,
  `src/features/meeting/network/messages/__tests__/StateMeeting.test.ts`,
  `src/features/meeting/network/messages/CreateMeeting.ts`, `src/features/meeting/network/MeetingMessageApi.t`,
  `src/features/rollCall/network/__tests__/RollCallMessageApi.ts`, `src/features/rollCall/network/messages`,
  `src/features/rollCall/network/RollCallMessageApi.ts`, `src/features/rollCall/screens/RollCallOpened.tsx`,
  `src/features/social/network/__tests__/SocialMessageApi.test.ts`, `src/features/social/network/SocialMessageApi.ts`,
  `src/ingestion/handlers/Utils.ts`, `src/ingestion/Handler.ts`, `src/parts/Launch.tsx`,
  `src/features/witness/network/__tests__/WitnessMessageApi.ts`, `src/model/network/__tests__/FromJsonRpcRequest.test.ts`
  `src/model/network/method/message/data/__tests__/MessageRegistry.test.ts`, `src/features/wallet/objects/Token.ts`
- Lao : `src/features/evoting/network/messages/SetupElection.ts`, `src/features/evoting/network/ElectionMessageApi.ts`,
  `src/features/evoting/screens/CreateElection.tsx`, `src/features/meeting/network/messages/CreateMeeting.ts`,
  `src/features/meeting/network/MeetingMessageApi.ts`, `src/features/rollCall/network/messages/__tests_`,
  `src/features/rollCall/network/RollCallMessageApi.ts`, `src/features/social/network/SocialMessageApi.ts`,
  `src/parts/Home.tsx`, `src/parts/Launch.tsx`, `src/model/network/__tests__/FromJsonRpcRequest.test.ts`,
  `src/model/network/method/message/data/__tests__/MessageRegistry.test.ts`
- CreateLao : `src/model/network/__tests__/FromJsonRpcRequest.test.ts`

## Wallet

- generateToken : `features/social/navigation/SocialMediaNavigation.tsx`
- Token : `model/network/method/message/Message.ts`
- Wallet objects : `features/rollCall/components/EventRollCall.tsx`, `features/rollCall/network/RollCallHandler.ts`,
  `features/rollCall/screens/RollCallOpened.tsx`
- WalletNavigation : `navigation/bars/LaoNavigation.tsx`, `navigation/bars/MainNavigation.tsx`
- WalletReducer : `store/reducers/RootReducer.ts`, `store/__mocks__/Storage.ts`

## Witness

- WitnessScanning : `src/features/lao/navigation/OrganizerNavigation.tsx`
- WitnessNavigation : `src/features/lao/navigation/LaoNavigation.tsx`
- WitnessHandler : `src/ingestion/handlers/index.ts`
- WitnessSignature : `src/model/network/method/message/Message.ts`, `src/store/reducers/MessageReducer.ts`,
  `src/features/lao/network/messages/StateLao.ts`, `src/features/meeting/network/messages/StateMeeting.ts`,
  `src/model/network/method/message/ExtendedMessage.ts`

## Connect

- ConnectNavigation : `src/navigation/bars/MainNavigation.tsx`
- ConnectToLao : `src/features/lao/components/LaoProperties.tsx`
