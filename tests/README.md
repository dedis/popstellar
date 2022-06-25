# Functional Requirements
Last updated  : 25.6.2022

# Lao
## Create
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Reliable|   X    |   X    |
| Be2 |Reliable|   X    |   X    |
| Fe1 |Reliable|   X    |   X    |
| Fe2 |Reliable|   X    |   X    |

## Greeting
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|   X    |        |
| Be2 |Operational|   X    |        |
| Fe1 |Operational|   X    |        |
| Fe2 |Operational|   X    |        |

# Roll-Call

## Create
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Unstable|   X    |    X   |
| Be2 |Reliable|   X    |    X   |
| Fe1 |Operational|   X    |    X   |
| Fe2 |Operational|   X    |    X   |

## Open


|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Unstable|   X    |    X   |
| Be2 |Reliable|   X    |    X   |
| Fe1 |Operational|     X  |    X   |
| Fe2 |Operational|     X  |    X   |

## Close


|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Unstable|    X   |    X   |
| Be2 |Operational|    X   |    X   |
| Fe1 |Operational|     X  |    X   |
| Fe2 |Operational|     X  |    X   |

## ReOpen


|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|   X    |        |
| Be2 |Operational|   X    |        |
| Fe1 |Operational|   X    |        |
| Fe2 |Operational|   X    |        |


## ReClose


|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|    X   |        |
| Be2 |Operational|    X   |        |
| Fe1 |Operational|    X   |        |
| Fe2 |Operational|    X   |        |

# Election

## SetUp
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Reliable|   X    |    X   |
| Be2 |Unstable|   X    |    X   |
| Fe1 |Operational|    X   |        |
| Fe2 |Operational|    X   |        |
## Open
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Reliable|   X    |    X   |
| Be2 |Operational|        |        |
| Fe1 |Operational|   X    |        |
| Fe2 |Operational|   X    |        |

## Cast Vote open ballots
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|   X    |    X   |
| Be2 |Operational|    X   |     X  |
| Fe1 |Operational|   X    |        |
| Fe2 |Operational|   X    |        |

## Cast Vote encrypted
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|   X    |        |
| Be2 |Operational|   X    |        |
| Fe1 |Operational|   X    |        |
| Fe2 |Operational|   X    |        |

## End
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Unstable|    X   |    X   |
| Be2 |Operational|    X   |    X   |
| Fe1 |Operational|    X   |        |
| Fe2 |Operational|    X   |        |
## Result
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|    X   |    X   |
| Be2 |Operational|    X   |    X   |
| Fe1 |Operational|    X   |        |
| Fe2 |Operational|    X   |        |
# Digital Cash

## Issuance
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|    X   |    X   |
| Be2 |Operational|    X   |    X   |
| Fe1 |Operational|    X   |        |
| Fe2 |Unstable|   X    |        |
## Send/Receive
|     | Status | Manual | Karate |
|-----|--------|--------|--------|
| Be1 |Operational|   X    |        |
| Be2 |Unstable|   X    |        |
| Fe1 |Operational|   X    |        |
| Fe2 |Unstable|    X   |        |

## Offline sent
Not implemented for now

# Non treated this semester

## Chirp

### Add
### Delete
### Reaction Add
### Reaction Delete

## Consensus


# Legend
- Unimplemented : The feature is not implemented.
- No Data : No Data was collected on this specific feature.
- Not Working : The feature is not working properly and cannot be used.
- Unstable : The feature is not working properly or is prone to vulnerabilities but can still be used in the main scenarios.
- Operational : The feature can be used in all tested scenarios. It does not mean that it is
reliable. It only means that the current tests are passing and that one is able to use the
functionality properly.
- Reliable : The feature has a sufficient number of tests covering all of its aspects. We can
say confidently that it can be used properly in almost every situation.
```
