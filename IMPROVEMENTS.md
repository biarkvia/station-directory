# Further improvements

## Show when each list was last refreshed

A station operator can reopen yesterday's device list offline. The app shows that
the list is saved, but gives no indication of its age. Store a last-successful-refresh
timestamp for the directory and for each station's device list, in the same Room
transaction as the rows. Show that time beside the saved-data message. An empty
response should update the timestamp, and a failed request should preserve it.
The device's `lastSeen` would still describe its last contact with the backend.

## Test the first database migration with an offline device

The version 1 Room schema is checked in. Adding refresh timestamps would be the
first schema change. Seed a version 1 database with a populated station, a station
with a saved empty list and a station whose devices have never been opened. After
migration, verify that all three still open correctly offline. Existing rows
should display an unknown refresh time until the next successful request.
