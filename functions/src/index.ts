/**
 * Cloud Functions for Festify Event Management App
 * TypeScript implementation using Firebase Functions v2 API
 */

import {setGlobalOptions} from "firebase-functions/v2";
import {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
  Change,
  FirestoreEvent,
  QueryDocumentSnapshot,
} from "firebase-functions/v2/firestore";
import {onCall, HttpsError, CallableRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

// Initialize Firebase Admin
admin.initializeApp();

// Get Firestore instance
const db = admin.firestore();

// Set global options for cost control
setGlobalOptions({maxInstances: 10});

// ============================================================================
// FIRESTORE TRIGGERS
// ============================================================================

/**
 * When a user updates their name, update it in all their messages and events
 * Trigger: users/{userId} document update
 */
export const onUserNameUpdate = onDocumentUpdated(
  "users/{userId}",
  async (event: FirestoreEvent<Change<QueryDocumentSnapshot> | undefined>) => {
    if (!event.data) return null;

    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();
    const userId = event.params.userId;

    // Check if name actually changed
    if (beforeData.name === afterData.name) {
      return null;
    }

    const newName = afterData.name as string;

    try {
      const batch = db.batch();
      let updateCount = 0;

      // Update all messages sent by this user (using collectionGroup)
      const messagesQuery = await db
        .collectionGroup("messages")
        .where("senderId", "==", userId)
        .limit(500)
        .get();

      messagesQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {senderName: newName});
        updateCount++;
      });

      // Update all events hosted by this user
      const eventsQuery = await db
        .collection("events")
        .where("hostId", "==", userId)
        .get();

      eventsQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {hostName: newName});
        updateCount++;
      });

      // Update all event attendees
      const attendeesQuery = await db
        .collectionGroup("attendees")
        .where("userId", "==", userId)
        .limit(500)
        .get();

      attendeesQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {userName: newName});
        updateCount++;
      });

      // Commit all updates (max 500 per batch)
      if (updateCount > 0) {
        await batch.commit();
      }

      logger.info(`Updated name for user ${userId} to ${newName}`);
      return null;
    } catch (error) {
      logger.error("Error updating user name:", error);
      return null;
    }
  }
);

/**
 * When a user RSVPs to an event, update attendee count
 * Trigger: events/{eventId}/attendees/{userId} document create
 */
export const onEventRsvp = onDocumentCreated(
  "events/{eventId}/attendees/{userId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    if (!event.data) return null;

    const eventId = event.params.eventId;
    const userId = event.params.userId;
    const attendeeData = event.data.data();

    try {
      // Get event details
      const eventRef = db.collection("events").doc(eventId);
      const eventDoc = await eventRef.get();

      if (!eventDoc.exists) {
        return null;
      }

      const eventData = eventDoc.data();

      // Only increment for "attending" status
      if (attendeeData.status === "attending") {
        await eventRef.update({
          attendees: admin.firestore.FieldValue.increment(1),
        });

        // Log notification (implement FCM here if needed)
        if (eventData?.hostId !== userId) {
          logger.info(
            `User ${userId} RSVP'd to event ${eventId} ` +
            `hosted by ${eventData?.hostId}`
          );
        }
      }

      return null;
    } catch (error) {
      logger.error("Error on RSVP:", error);
      return null;
    }
  }
);

/**
 * When an attendee is removed, decrement attendee count
 * Trigger: events/{eventId}/attendees/{userId} document delete
 */
export const onEventRsvpCancel = onDocumentDeleted(
  "events/{eventId}/attendees/{userId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    if (!event.data) return null;

    const eventId = event.params.eventId;
    const attendeeData = event.data.data();

    try {
      // Only decrement if they were attending
      if (attendeeData.status === "attending") {
        await db
          .collection("events")
          .doc(eventId)
          .update({
            attendees: admin.firestore.FieldValue.increment(-1),
          });
      }

      logger.info(`Decremented attendee count for event ${eventId}`);
      return null;
    } catch (error) {
      logger.error("Error on RSVP cancel:", error);
      return null;
    }
  }
);

/**
 * When a new message is sent, update chat preview
 * Trigger: events/{eventId}/messages/{messageId} document create
 */
export const onNewMessage = onDocumentCreated(
  "events/{eventId}/messages/{messageId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    if (!event.data) return null;

    const eventId = event.params.eventId;
    const messageData = event.data.data();

    try {
      // Get event details
      const eventDoc = await db.collection("events").doc(eventId).get();

      if (!eventDoc.exists) {
        return null;
      }

      const eventData = eventDoc.data();

      // Update chat preview
      await db
        .collection("chats")
        .doc(eventId)
        .set(
          {
            eventId: eventId,
            eventName: eventData?.title || "Unknown Event",
            lastMessage: (messageData.text || "").substring(0, 100),
            lastMessageTime:
              messageData.timestamp ||
              admin.firestore.FieldValue.serverTimestamp(),
            lastMessageSender: messageData.senderName || "Unknown",
          },
          {merge: true}
        );

      logger.info(`Updated chat preview for event ${eventId}`);
      return null;
    } catch (error) {
      logger.error("Error updating chat preview:", error);
      return null;
    }
  }
);

/**
 * When a new event is created, initialize its chat
 * Trigger: events/{eventId} document create
 */
export const onEventCreate = onDocumentCreated(
  "events/{eventId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    if (!event.data) return null;

    const eventId = event.params.eventId;
    const eventData = event.data.data();

    try {
      // Create chat document
      await db
        .collection("chats")
        .doc(eventId)
        .set({
          eventId: eventId,
          eventName: eventData.title || "Unknown Event",
          lastMessage: "Chat started",
          lastMessageTime: admin.firestore.FieldValue.serverTimestamp(),
          lastMessageSender: "System",
          participantCount: 0,
          participantIds: [],
        });

      // Create welcome message
      await db
        .collection("events")
        .doc(eventId)
        .collection("messages")
        .add({
          senderId: "system",
          senderName: "System",
          text: `Welcome to ${eventData.title || "this event"}! 🎉`,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          type: "system",
        });

      // Increment host's events hosted count
      if (eventData.hostId) {
        await db
          .collection("users")
          .doc(eventData.hostId)
          .update({
            eventsHosted: admin.firestore.FieldValue.increment(1),
          });
      }

      logger.info(`Initialized chat for new event ${eventId}`);
      return null;
    } catch (error) {
      logger.error("Error on event create:", error);
      return null;
    }
  }
);

/**
 * When an event is deleted, clean up related data
 * Trigger: events/{eventId} document delete
 */
export const onEventDelete = onDocumentDeleted(
  "events/{eventId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    if (!event.data) return null;

    const eventId = event.params.eventId;
    const eventData = event.data.data();

    try {
      const batch = db.batch();

      // Delete chat document
      batch.delete(db.collection("chats").doc(eventId));

      // Delete all messages (limited to 500 due to batch size)
      const messagesQuery = await db
        .collection("events")
        .doc(eventId)
        .collection("messages")
        .limit(500)
        .get();

      messagesQuery.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      // Delete all attendees
      const attendeesQuery = await db
        .collection("events")
        .doc(eventId)
        .collection("attendees")
        .limit(500)
        .get();

      attendeesQuery.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      // Decrement host's events hosted count
      if (eventData.hostId) {
        batch.update(db.collection("users").doc(eventData.hostId), {
          eventsHosted: admin.firestore.FieldValue.increment(-1),
        });
      }

      await batch.commit();

      logger.info(`Cleaned up deleted event ${eventId}`);
      return null;
    } catch (error) {
      logger.error("Error on event delete:", error);
      return null;
    }
  }
);

// ============================================================================
// SCHEDULED FUNCTIONS
// ============================================================================

/**
 * Scheduled function to mark past events as "past"
 * Runs every day at midnight UTC
 */
export const markPastEvents = onSchedule("0 0 * * *", async () => {
  try {
    const now = admin.firestore.Timestamp.now();

    // Query upcoming events that have passed
    const pastEventsQuery = await db
      .collection("events")
      .where("status", "==", "upcoming")
      .where("startDateTime", "<", now)
      .get();

    const batch = db.batch();
    let count = 0;

    pastEventsQuery.docs.forEach((doc) => {
      batch.update(doc.ref, {
        status: "past",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      count++;
    });

    // Batch limit is 500
    if (count > 0 && count <= 500) {
      await batch.commit();
    }

    logger.info(`Marked ${count} events as past`);
  } catch (error) {
    logger.error("Error marking past events:", error);
  }
});

// ============================================================================
// HTTP CALLABLE FUNCTIONS
// ============================================================================

interface SearchEventsRequest {
  query: string;
  limit?: number;
}

interface EventStatsRequest {
  eventId: string;
}

interface EventData {
  id?: string;
  title?: string;
  description?: string;
  tags?: string[];
  [key: string]: any; // eslint-disable-line @typescript-eslint/no-explicit-any
}

/**
 * HTTP Callable function to search events
 * Call from Android: functions.httpsCallable("searchEvents").call(data)
 */
export const searchEvents = onCall(
  async (request: CallableRequest<SearchEventsRequest>) => {
    // Check authentication
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated");
    }

    const query = (request.data.query || "").toLowerCase();
    const limit = request.data.limit || 20;

    try {
      // Get public upcoming events
      const eventsQuery = await db
        .collection("events")
        .where("isPublic", "==", true)
        .where("status", "==", "upcoming")
        .limit(100) // Get more than needed for filtering
        .get();

      // Filter results
      const results: EventData[] = [];

      for (const doc of eventsQuery.docs) {
        const eventData = doc.data() as EventData;
        eventData.id = doc.id;

        // Search in title, description, or tags
        const title = (eventData.title || "").toLowerCase();
        const description = (eventData.description || "").toLowerCase();
        const tags = eventData.tags || [];

        if (
          title.includes(query) ||
          description.includes(query) ||
          tags.some((tag: string) => tag.toLowerCase().includes(query))
        ) {
          results.push(eventData);

          if (results.length >= limit) {
            break;
          }
        }
      }

      return {
        events: results,
        count: results.length,
      };
    } catch (error) {
      logger.error("Error searching events:", error);
      throw new HttpsError("internal", "Error searching events");
    }
  }
);

/**
 * HTTP Callable function to generate event statistics
 * Call from Android: functions.httpsCallable("getEventStats").call(data)
 */
export const getEventStats = onCall(
  async (request: CallableRequest<EventStatsRequest>) => {
    // Check authentication
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated");
    }

    const eventId = request.data.eventId;

    if (!eventId) {
      throw new HttpsError("invalid-argument", "Event ID is required");
    }

    try {
      // Get event
      const eventDoc = await db.collection("events").doc(eventId).get();

      if (!eventDoc.exists) {
        throw new HttpsError("not-found", "Event not found");
      }

      // Get attendees
      const attendeesSnapshot = await db
        .collection("events")
        .doc(eventId)
        .collection("attendees")
        .get();

      // Get messages
      const messagesSnapshot = await db
        .collection("events")
        .doc(eventId)
        .collection("messages")
        .get();

      // Calculate stats
      const attendees = attendeesSnapshot.docs.map((doc) => doc.data());
      const attending = attendees.filter(
        (a) => a.status === "attending"
      ).length;
      const maybe = attendees.filter((a) => a.status === "maybe").length;

      return {
        totalAttendees: attendees.length,
        attending: attending,
        maybe: maybe,
        totalMessages: messagesSnapshot.size,
        eventData: eventDoc.data(),
      };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      logger.error("Error getting event stats:", error);
      throw new HttpsError("internal", "Error getting event stats");
    }
  }
);

// ============================================================================
// HELPER FUNCTIONS (not exported, used internally)
// ============================================================================

/**
 * Helper function to send push notifications (FCM)
 * @param {string} userId - The user ID to send notification to
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {object} data - Additional data payload
 */
async function sendPushNotification(
  userId: string,
  title: string,
  body: string,
  data: {[key: string]: string} = {}
): Promise<void> {
  try {
    // Get user's FCM token
    const userDoc = await db.collection("users").doc(userId).get();

    if (!userDoc.exists) {
      logger.info(`User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      logger.info(`No FCM token for user ${userId}`);
      return;
    }

    // Send notification
    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: data,
      android: {
        priority: "high",
      },
    });

    logger.info(`Sent notification to user ${userId}`);
  } catch (error) {
    logger.error("Error sending push notification:", error);
  }
}

// Prevent unused function warning - can be called by other functions
export {sendPushNotification};
