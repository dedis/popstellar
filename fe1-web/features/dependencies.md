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
