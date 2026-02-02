# Firebase Cloud Functions für Bonus Collection

Diese Dokumentation beschreibt die Firebase Cloud Functions, die für die tägliche Silent Push Notification zum Bonus-Sammeln benötigt werden.

## Setup

### Prerequisites
```bash
npm install -g firebase-tools
firebase login
firebase init functions
cd functions
npm install
```

### Dependencies in `functions/package.json`
```json
{
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^4.8.0"
  }
}
```

## Cloud Functions

### 1. Device Token Registration Endpoint

**File:** `functions/src/registerDeviceToken.ts`

```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

interface DeviceTokenRequest {
  token: string;
  platform: "iOS" | "Android";
}

export const registerDeviceToken = functions.https.onRequest(
  async (req, res) => {
    // Allow CORS
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }

    // Only POST allowed
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    // Check authentication token
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const token = authHeader.slice(7);

    try {
      // Verify Firebase token (from Kickbase auth)
      // Note: This requires the user's auth token from Kickbase
      // For now, we'll use it as userId directly (you may need to adjust)
      const userId = token; // In production, verify this token

      const body: DeviceTokenRequest = req.body;

      // Validate input
      if (!body.token || !body.platform) {
        res.status(400).json({ error: "Missing token or platform" });
        return;
      }

      // Check if token is valid format (roughly)
      if (body.token.length < 20) {
        res.status(400).json({ error: "Invalid token format" });
        return;
      }

      // Store device token in Firestore
      const deviceTokenRef = db.collection("deviceTokens").doc();

      await deviceTokenRef.set(
        {
          userId: userId,
          token: body.token,
          platform: body.platform,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          valid: true,
          lastActivity: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

      // Also maintain a user index for faster queries
      const userRef = db.collection("users").doc(userId);
      await userRef.set(
        {
          deviceTokens: admin.firestore.FieldValue.arrayUnion(
            deviceTokenRef.id
          ),
          lastTokenUpdate: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

      console.log(
        `✅ Device token registered for user ${userId}, platform ${body.platform}`
      );

      res.status(201).json({
        success: true,
        message: "Device token registered successfully",
      });
    } catch (error) {
      console.error("Error registering device token:", error);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);
```

### 2. Daily Bonus Push Function

**File:** `functions/src/sendDailyBonusPush.ts`

```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

export const sendDailyBonusPush = functions.pubsub
  .schedule("0 12 * * *") // 12:00 UTC every day
  .timeZone("UTC")
  .onRun(async (context) => {
    console.log("🎁 Starting daily bonus push at", new Date());

    try {
      // Get all unique users with device tokens
      const usersSnapshot = await db
        .collection("users")
        .where("deviceTokens", "!=", null)
        .get();

      let successCount = 0;
      let failureCount = 0;
      const badTokens: string[] = [];

      for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id;
        const userData = userDoc.data();
        const deviceTokenIds = userData.deviceTokens || [];

        // Check if bonus already sent today (using cache)
        const todayKey = new Date().toISOString().split("T")[0]; // YYYY-MM-DD
        const cacheKey = `bonus_push_${userId}_${todayKey}`;

        // For now, we'll skip this check - Firestore would be better
        // In production, use Firestore to track sent bonuses

        for (const tokenId of deviceTokenIds) {
          try {
            const tokenDoc = await db
              .collection("deviceTokens")
              .doc(tokenId)
              .get();

            if (!tokenDoc.exists) {
              console.warn(`Token doc not found: ${tokenId}`);
              continue;
            }

            const tokenData = tokenDoc.data();
            if (!tokenData?.valid) {
              console.log(`Token marked invalid: ${tokenId}`);
              continue;
            }

            const deviceToken = tokenData.token;

            // Build silent push payload
            const payload = {
              data: {
                bonus_id: `daily_${todayKey}`,
                collection_deadline: new Date(
                  Date.now() + 24 * 60 * 60 * 1000
                ).toISOString(),
              },
              apns: {
                headers: {
                  "apns-priority": "10",
                  "apns-push-type": "background",
                },
                payload: {
                  aps: {
                    "content-available": 1,
                  },
                },
              },
              webpush: {
                headers: {
                  TTL: "3600",
                },
              },
            };

            // Send via FCM (which uses APNs for iOS)
            const messageId = await messaging.send({
              token: deviceToken,
              data: payload.data,
              apns: payload.apns as admin.messaging.ApnsConfig,
            });

            console.log(
              `✅ Push sent to ${userId} with messageId: ${messageId}`
            );
            successCount++;

            // Update last activity
            await tokenDoc.ref.update({
              lastActivity: admin.firestore.FieldValue.serverTimestamp(),
            });
          } catch (error: any) {
            failureCount++;

            // Check if token is invalid
            if (
              error.code === "messaging/invalid-registration-token" ||
              error.code === "messaging/registration-token-not-registered"
            ) {
              console.warn(`Bad token detected: ${tokenId}`);
              badTokens.push(tokenId);

              // Mark as invalid
              try {
                await db
                  .collection("deviceTokens")
                  .doc(tokenId)
                  .update({
                    valid: false,
                    invalidatedAt: admin.firestore.FieldValue.serverTimestamp(),
                  });
              } catch (updateError) {
                console.error("Error marking token invalid:", updateError);
              }
            } else {
              console.error(`Error sending to ${tokenId}:`, error);
            }
          }
        }
      }

      // Log results
      const results = {
        timestamp: new Date().toISOString(),
        totalUsers: usersSnapshot.size,
        successCount,
        failureCount,
        badTokensCount: badTokens.length,
      };

      console.log("📊 Daily bonus push summary:", results);

      // Store results in Firestore for monitoring
      await db.collection("bonusPushLogs").add(results);

      return results;
    } catch (error) {
      console.error("❌ Error in sendDailyBonusPush:", error);
      throw error;
    }
  });
```

### 3. Cleanup Bad Tokens Function

**File:** `functions/src/cleanupBadTokens.ts`

```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const cleanupBadTokens = functions.pubsub
  .schedule("0 1 * * *") // 1:00 AM UTC daily
  .timeZone("UTC")
  .onRun(async (context) => {
    console.log("🧹 Starting cleanup of invalid tokens");

    try {
      // Find all invalid tokens
      const invalidTokensSnapshot = await db
        .collection("deviceTokens")
        .where("valid", "==", false)
        .get();

      let deletedCount = 0;

      for (const tokenDoc of invalidTokensSnapshot.docs) {
        const tokenData = tokenDoc.data();

        // Delete if invalidated more than 7 days ago
        const invalidatedAt = tokenData.invalidatedAt?.toDate();
        const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);

        if (invalidatedAt && invalidatedAt < sevenDaysAgo) {
          // Remove from user's deviceTokens array
          if (tokenData.userId) {
            await db
              .collection("users")
              .doc(tokenData.userId)
              .update({
                deviceTokens: admin.firestore.FieldValue.arrayRemove(
                  tokenDoc.id
                ),
              });
          }

          // Delete the token document
          await tokenDoc.ref.delete();
          deletedCount++;
        }
      }

      console.log(`✅ Cleanup complete: ${deletedCount} tokens deleted`);
      return { deletedCount };
    } catch (error) {
      console.error("❌ Error in cleanupBadTokens:", error);
      throw error;
    }
  });
```

## Firestore Schema

### Collections

#### `deviceTokens` Collection
```
/deviceTokens/{tokenId}
  - userId: string (Kickbase user ID)
  - token: string (iOS device token)
  - platform: "iOS" | "Android"
  - timestamp: Timestamp (when registered)
  - valid: boolean (true/false - false if APNs reported bad)
  - lastActivity: Timestamp (last push sent)
  - invalidatedAt: Timestamp (when marked invalid)
```

#### `users` Collection (Index)
```
/users/{userId}
  - deviceTokens: string[] (array of tokenIds)
  - lastTokenUpdate: Timestamp
```

#### `bonusPushLogs` Collection (Monitoring)
```
/bonusPushLogs/{logId}
  - timestamp: Timestamp
  - totalUsers: number
  - successCount: number
  - failureCount: number
  - badTokensCount: number
```

## Deployment

```bash
# Deploy all functions
firebase deploy --only functions

# Deploy specific function
firebase deploy --only functions:sendDailyBonusPush

# View logs
firebase functions:log
```

## Testing

### Test Device Token Registration
```bash
curl -X POST https://your-project.cloudfunctions.net/registerDeviceToken \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -d '{
    "token": "abc123def456...",
    "platform": "iOS"
  }'
```

### Test Manual Push (Firebase Console)
1. Go to Firebase Console → Cloud Messaging
2. Select your device token
3. Send test message

### Test via Cloud Functions Emulator
```bash
firebase emulators:start
# In another terminal:
curl -X POST http://localhost:5001/your-project/us-central1/registerDeviceToken \
  -H "Content-Type: application/json" \
  -d '{"token": "test123", "platform": "iOS"}'
```

## Monitoring & Alerts

### Setup Alerts in Firebase Console

1. **Quota exceeded**: Get notified if function execution quota is exceeded
2. **Function errors**: Enable error reporting
3. **Custom metrics**: Use Firestore monitoring for push success rate

### Sample Alert Rule
- Alert when daily success rate < 95%
- Alert when bad tokens > 10% of total

## Notes

- Device token registration uses the Kickbase auth token as userId
- You may need to adjust authentication based on your Kickbase implementation
- Silent push priority is set to `apns-priority: 10` for immediate delivery
- The schedule is UTC-based; adjust timezone if needed for your user base
