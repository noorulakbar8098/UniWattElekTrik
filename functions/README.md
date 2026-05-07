# UniWatt Push Notifications — Setup notes (Android)

End-to-end FCM integration. Tokens are written by the Android client to
Firestore; pushes are fanned out by these Cloud Functions.

## Token storage

```
admins/{uid}: {
  fcmTokens:   ["abc...", ...],
  fcmTokenMeta: { "abc...": { platform, updatedAt } }
}
users/{uid}:  same shape
```

## Triggers

| Trigger                          | Recipient                  |
| -------------------------------- | -------------------------- |
| `tasks/{id}` create              | the assigned user          |
| `tasks/{id}` status change       | the owning admin           |
| `leave_requests/{id}` create     | the owning admin           |
| `leave_requests/{id}` status     | the requesting user        |

## Deploy

```bash
# from project root
cd functions && npm install && cd ..

firebase login          # if not already
firebase use <project>  # creates .firebaserc

firebase deploy --only functions
```

## Test the device end first

1. Run the app on a real Android 13+ device, log in.
2. Open Firestore → `admins/<your-uid>` (or `users/<uid>`) and confirm
   `fcmTokens` contains a string.
3. Firebase Console → Cloud Messaging → "Send test message" → paste the token.
   The phone should buzz immediately.

Once that works, deploy the functions and try a real flow (create a task,
submit a leave request).

## Logout cleanup

`PushTokenRegistrar.unregisterCurrentDevice()` is invoked from
`MainActivity` when the auth session flows to `null`. It removes the token
from Firestore *and* deletes it from FCM so a different user signing in on
the same device gets a brand-new token.

