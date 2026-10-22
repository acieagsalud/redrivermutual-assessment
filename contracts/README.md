# Event contracts

`claim-submitted.v1.json` is the agreed shape of the `ClaimSubmitted` event (version 1).

Each service owns its own Java type for the event and has a test against this file:

- `claims-api` checks that what it **writes** matches the fixture field for field.
- `notification-service` checks that it can **read** the fixture.

If either side changes the shape, its build fails before anything is deployed. Adding an optional
field is non-breaking (the consumer ignores unknown fields). Removing or renaming one means a new
version: `claim-submitted.v2.json` and `version: 2`.
