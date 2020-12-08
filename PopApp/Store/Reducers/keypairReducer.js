const initialState = { pubKey: [], secKey: [] };

function keypairReducer(state = initialState, action) {
  if (action.type === 'SET_KEYPAIR') {
    let nextState = {
      ...state,
      pubKey: action.value.pubKey,
      secKey: action.value.secKey
    };
    return nextState || state;

  } else {
    return state;
  }
}

export default keypairReducer;
