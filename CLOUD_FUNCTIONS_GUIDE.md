# 🔥 Quick Start: Firebase Cloud Functions for Festify

## ⚡ What Are Cloud Functions?

Cloud Functions are serverless functions that run in response to events:
- **Firestore triggers**: Auto-run when data changes
- **HTTP functions**: Call from your Android app
- **Scheduled functions**: Run on a schedule (cron jobs)

## 🎯 Why Use Them in Festify?

✅ **Denormalization**: Auto-update user names across all their messages
✅ **Integrity**: Keep attendee counts accurate automatically
✅ **Cleanup**: Delete related data when events are removed
✅ **Notifications**: Send push notifications on events
✅ **Search**: Server-side search (faster & more secure)
✅ **Analytics**: Generate statistics and insights
✅ **Scheduled tasks**: Mark past events, send reminders

---

## 🚀 Quick Deploy (5 minutes)

### Step 1: Install Dependencies
```bash
cd functions
npm install
```

### Step 2: Login to Firebase
```bash
firebase login
```

### Step 3: Select Your Project
```bash
# List projects
firebase projects:list

# Use your project
firebase use festify-xxxxx  # Replace with your project ID
```

### Step 4: Deploy!
```bash
firebase deploy --only functions
```

That's it! Your functions are live. 🎉

---

## 📱 Using Functions in Android App

### Example 1: Search Events (from Repository)

```kotlin
// In your ViewModel
class HomeScreenViewModel @Inject constructor(
    private val functionsRepository: FunctionsRepository
) : ViewModel() {

    fun searchEvents(query: String) {
        viewModelScope.launch {
            functionsRepository.searchEvents(query, limit = 20).fold(
                onSuccess = { events ->
                    _events.value = events
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
        }
    }
}
```

### Example 2: Get Event Statistics

```kotlin
// Get detailed event stats
viewModelScope.launch {
    functionsRepository.getEventStats(eventId).fold(
        onSuccess = { stats ->
            println("Total attendees: ${stats.totalAttendees}")
            println("Messages sent: ${stats.totalMessages}")
        },
        onFailure = { error ->
            println("Error: ${error.message}")
        }
    )
}
```

### Example 3: Direct Function Call

```kotlin
// Inject FirebaseFunctions
@Inject lateinit var functions: FirebaseFunctions

// Call function
suspend fun callCustomFunction() {
    try {
        val data = hashMapOf(
            "param1" to "value1",
            "param2" to 123
        )

        val result = functions
            .getHttpsCallable("myFunctionName")
            .call(data)
            .await()

        // Use result
        val response = result.data
    } catch (e: Exception) {
        // Handle error
    }
}
```

---

## 🧪 Local Testing with Emulator

### 1. Start Emulator
```bash
cd functions
npm run serve
```

### 2. Connect Android App to Emulator

In your `FestifyApplication.kt`:

```kotlin
class FestifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Use emulator in debug builds
        if (BuildConfig.DEBUG) {
            FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        }
    }
}
```

### 3. Test Functions

Run your app - it will now use local functions instead of production!

---

## 📊 What Functions Are Included?

### 🔄 Auto-Triggered (No code needed!)

These run automatically when events happen:

| Function | Trigger | Purpose |
|----------|---------|---------|
| `onUserNameUpdate` | User profile update | Updates name everywhere |
| `onEventCreate` | New event created | Initializes chat, welcome message |
| `onEventDelete` | Event deleted | Cleans up messages, attendees |
| `onEventRsvp` | User RSVPs | Updates count, notifies host |
| `onEventRsvpCancel` | User cancels | Updates count |
| `onNewMessage` | Message sent | Updates chat preview |
| `markPastEvents` | Daily at midnight | Marks old events as past |

### 📞 Callable (Call from Android)

| Function | Purpose | Parameters |
|----------|---------|------------|
| `searchEvents` | Server-side search | `query`, `limit` |
| `getEventStats` | Detailed statistics | `eventId` |

---

## 🎨 Adding Your Own Functions

### Example: Send Birthday Reminder

1. **Add to `functions/src/index.ts`**:

```typescript
export const sendBirthdayReminder = functions.pubsub
  .schedule("0 9 * * *") // Every day at 9 AM
  .timeZone("America/New_York")
  .onRun(async (context) => {
    const today = new Date();

    // Find users with birthday today
    const usersSnapshot = await db.collection("users")
      .where("birthday", "==", today.toDateString())
      .get();

    // Send them a message
    for (const doc of usersSnapshot.docs) {
      await sendPushNotification(
        doc.id,
        "Happy Birthday! 🎉",
        "Celebrate with events today!"
      );
    }

    return null;
  });
```

2. **Deploy**:
```bash
firebase deploy --only functions:sendBirthdayReminder
```

3. **Done!** Function runs automatically every day.

---

## 🔍 Monitoring & Debugging

### View Logs
```bash
# All logs
firebase functions:log

# Specific function
firebase functions:log --only onEventCreate

# Live logs
firebase functions:log --follow
```

### Firebase Console
See execution stats, errors, and performance:
- Go to: [Firebase Console → Functions](https://console.firebase.google.com)
- View execution count, errors, duration

---

## 💰 Pricing (Free Tier)

✅ **2,000,000** invocations/month
✅ **400,000** GB-seconds/month
✅ **200,000** CPU-seconds/month

**For Festify**: Likely FREE forever unless you have millions of users!

---

## 🚨 Troubleshooting

### "Firebase project not found"
```bash
firebase login --reauth
firebase use --add
```

### "Permission denied"
Make sure you're an owner/editor of the Firebase project.

### "Function timeout"
Increase timeout in function definition:
```typescript
export const myFunction = functions
  .runWith({timeoutSeconds: 300}) // 5 minutes
  .firestore.document(...)
```

### "Module not found"
```bash
cd functions
rm -rf node_modules package-lock.json
npm install
```

---

## 📚 Next Steps

1. ✅ Deploy functions: `firebase deploy --only functions`
2. ✅ Test locally with emulator
3. ✅ Add `FunctionsRepository` to your ViewModels
4. ✅ Monitor logs and performance
5. ✅ Add custom functions as needed!

---

## 🎉 You're a Cloud Functions Expert!

Key takeaways:
- Functions run automatically on triggers
- Can be called from Android like an API
- Perfect for data consistency and automation
- Free for most apps!

**Need help?** Check `functions/README.md` for detailed docs.

**Ready to deploy?** Run:
```bash
cd functions && npm install && firebase deploy --only functions
```

🚀 Happy coding!
