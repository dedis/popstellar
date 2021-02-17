import eventsData from '../../res/EventData';

const initialState = { events: eventsData };

/**
 * Reducer to manage the events of the current LAO of the user
 *
 * Three differents actions:
 *  ADD_EVENT: add the event given in action.value in the events list, must not be in the store
 *  UPDATE_EVENT: update the event given in action.value in the events list
 *  REMOVE_EVENT: remove the event with ID given in action.value
 *  CLEAR_EVENTS: delete all the store event
 *  UPDATE_EVENTS: move the event to the correct categories
*/

function removeEventAnEvent(event, id) {
  if (event.id === id) {
    if (event.children === undefined) {
      return [];
    }
    return event.children;
  }
  if (event.children !== undefined) {
    return { ...event, children: event.children.flatMap((c) => removeEventAnEvent(c, id)) };
  }
  return [event];
}

function removeEvent(state, id) {
  const s = state.map((period) => ({
    title: period.title,
    data: period.data.flatMap((event) => removeEventAnEvent(event, id)),
  }));
  return s;
}

function checkFutureNestedEvent(data, event, n) {
  if (event.end && event.start < data[n].start && event.end > data[n].start) {
    let j = n + 1;
    while (j < data.length && event.start < data[j].start && event.end > data[j].start) {
      j += 1;
    }
    const newchildren = data.splice(n, j - n);
    const parentEvent = { ...event, children: newchildren };
    data.splice(n, 0, parentEvent);
    return data;
  }
  return data;
}

function addEventAnEvent(data, event) {
  if (data.length > 0) {
    for (let i = 0; i < data.length; i += 1) {
      if (data[i].end && data[i].start < event.start && data[i].end > event.start) {
        if (data[i].children) {
          const parentEvent = { ...data[i], children: addEventAnEvent(data[i].children, event) };
          data.splice(i, 1, parentEvent);
          return data;
        }
        const parentEvent = { ...data[i], children: [event] };
        data.splice(i, 1, parentEvent);
      } if (data[i].end && data[i].end > event.start) {
        return checkFutureNestedEvent(data, event, i);
      } if (data[i].start > event.start) {
        return checkFutureNestedEvent(data, event, i);
      }
    }
    data.push(event);
    return data;
  }
  return [event];
}

function addEvent(state, event) {
  const timeStamp = Math.floor(Date.now() / 1000);
  let title;
  if (event.end) {
    if (event.end < timeStamp) {
      title = 'Past';
    } else if (event.start <= timeStamp && event.end > timeStamp) {
      title = 'Present';
    } else {
      title = 'Future';
    }
  } else if (event.start < timeStamp) {
    title = 'Past';
  } else if (event.start === timeStamp) {
    title = 'Present';
  } else {
    title = 'Future';
  }
  const s = state.map((period) => {
    if (period.title === title) {
      return {
        title: period.title,
        data: addEventAnEvent(period.data, event),
      };
    }
    return period;
  });
  return s;
}

function updateEvent(state) {
  const timeStamp = Math.floor(Date.now() / 1000);
  const past = state.filter((x) => x.title === 'Past').data;
  let present = state.filter((x) => x.title === 'Present').data;
  let i = 0;
  while (i < present.length
    && ((present[i].end && present[i].end < timeStamp)
    || (!present[i].end && present[i].start < timeStamp))) {
    i += 1;
  }

  const past2 = present.slice(0, i);
  past.push(past2);
  present = present.slice(i);

  let future = state.filter((x) => x.title === 'Future').data;
  i = 0;
  while (i < future.length
    && ((future[i].end && future[i].end < timeStamp)
    || (!future[i].end && future[i].start < timeStamp))) {
    i += 1;
  }

  const present2 = future.slice(0, i);
  present.push(present2);
  future = future.slice(i);
  const s = state.map((period) => {
    switch (period.title) {
      case 'Past':
        return {
          title: period.title,
          data: past,
        };
      case 'Present':
        return {
          title: period.title,
          data: present,
        };
      case 'Future':
        return {
          title: period.title,
          data: future,
        };
      default:
        return period;
    }
  });
  return s;
}

function currentEventsReducer(state = initialState, action) {
  let nextState;
  switch (action.type) {
    case 'ADD_EVENT':
      nextState = {
        ...state,
        events: [...addEvent(state.events, action.value)],
      };
      return nextState || state;
    case 'UPDATE_EVENT':
      nextState = {
        ...state,
        events: [...addEvent(removeEvent(state.events, action.value), action.value)],
      };
      return nextState || state;
    case 'REMOVE_EVENT':
      nextState = {
        ...state,
        events: [...removeEvent(state.events, action.value)],
      };
      return nextState || state;
    case 'CLEAR_EVENTS':
      nextState = {
        ...state,
        events: [
          {
            title: 'Past',
            data: [],
          },
          {
            title: 'Present',
            data: [],
          },
          {
            title: 'Future',
            data: [],
          },
        ],
      };
      return nextState || state;
    case 'UPDATE_EVENTS':
      nextState = {
        ...state,
        events: [...updateEvent(state.events)],
      };
      return nextState || state;
    default:
      return state;
  }
}

export default currentEventsReducer;
