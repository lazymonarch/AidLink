import { onDocumentCreated, onDocumentWritten, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { logger } from "firebase-functions";
import { getStorage } from "firebase-admin/storage";

initializeApp();
const db = getFirestore();
const auth = getAuth();
const storage = getStorage();

/**
 * Handles all actions submitted to a request's 'actions' subcollection.
 */
export const handleRequestActions = onDocumentCreated(
  "requests/{requestId}/actions/{actionId}",
  async (event) => {
    // This function is correct and remains unchanged.
    const actionSnap = event.data;
    if (!actionSnap) {
      logger.warn("Action document data is missing.");
      return;
    }
    const actionData = actionSnap.data();
    const requestId = event.params.requestId;
    const actionType = actionData.type;
    const createdBy = actionData.createdBy;
    const requestRef = db.collection("requests").doc(requestId);
    const actionRef = requestRef.collection("actions").doc(event.params.actionId);
    logger.info(`⚙️ [START] Processing action '${actionType}' for request ${requestId}`);
    try {
      await db.runTransaction(async (transaction) => {
        const requestDoc = await transaction.get(requestRef);
        if (!requestDoc.exists) {
          throw new Error("Request document not found.");
        }
        let chatDoc = null;
        if (actionType === "cancel_request") {
            const chatRef = db.collection("chats").doc(requestId);
            chatDoc = await transaction.get(chatRef);
        }
        const requestData = requestDoc.data();
        const requesterId = requestData.userId;

        if (actionType === "make_offer") {
            if (requestData.status !== 'open') {
                throw new Error(`Cannot make offer on a request that is not open. Status: ${requestData.status}.`);
            }
            if (createdBy === requesterId) {
                throw new Error("User cannot make an offer on their own request.");
            }
            const helperProfileRef = db.collection("users").doc(createdBy);
            const helperProfileDoc = await transaction.get(helperProfileRef);
            if (!helperProfileDoc.exists) {
                throw new Error(`Helper profile ${createdBy} not found.`);
            }
            const helperName = helperProfileDoc.data().name;
            const offerRef = requestRef.collection("offers").doc(createdBy);
            transaction.set(offerRef, {
                helperId: createdBy,
                helperName: helperName,
                status: "pending",
                createdAt: FieldValue.serverTimestamp(),
            });
        }
        else if (actionType === "accept_offer") {
            if (requestData.status !== 'open') {
                throw new Error(`Cannot accept an offer on this request. Status: ${requestData.status}.`);
            }
            if (createdBy !== requesterId) {
                throw new Error(`User ${createdBy} is not authorized to accept offers for this request.`);
            }
            const { helperId } = actionData;
            if (!helperId) {
                throw new Error("Missing 'helperId' in action data for 'accept_offer'.");
            }
            const [requesterProfileDoc, responderProfileDoc] = await Promise.all([
                transaction.get(db.collection("users").doc(requesterId)),
                transaction.get(db.collection("users").doc(helperId))
            ]);
            if (!requesterProfileDoc.exists || !responderProfileDoc.exists) {
                throw new Error("Requester or chosen responder profile not found.");
            }
            const requesterData = requesterProfileDoc.data();
            const responderData = responderProfileDoc.data();
            transaction.update(requestRef, {
                status: "in_progress",
                responderId: helperId,
                responderName: responderData.name, 
                participants: [requesterId, helperId],
            });
            const chatRef = db.collection("chats").doc(requestId);
            const systemMessageText = `${requesterData.name} accepted the offer! You can now chat.`;
            transaction.set(chatRef, {
                participants: [requesterId, helperId],
                participantInfo: {
                    [requesterId]: { name: requesterData.name, photoUrl: requesterData.photoUrl || "" },
                    [helperId]: { name: responderData.name, photoUrl: responderData.photoUrl || "" }
                },
                createdAt: FieldValue.serverTimestamp(),
                lastMessage: systemMessageText,
                lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
            const initialMessageRef = chatRef.collection("messages").doc();
            transaction.set(initialMessageRef, {
                senderId: "system",
                text: systemMessageText,
                timestamp: FieldValue.serverTimestamp(),
                type: "SYSTEM",
                systemType: "offer_accepted",
            });
        }
        else if (actionType === "cancel_request") {
            if (requestData.status !== 'in_progress') {
                throw new Error("Request is not in progress.");
            }
            if (createdBy !== requesterId) {
                throw new Error("Only the requester can cancel.");
            }
            transaction.update(requestRef, {
                status: "open",
                responderId: null,
                responderName: null,
                participants: FieldValue.delete(), 
            });
            if (chatDoc && chatDoc.exists) {
                const cancelMessage = "The requester has canceled this job. The request is now open for other helpers.";
                transaction.update(chatDoc.ref, {
                    lastMessage: cancelMessage,
                    lastMessageTimestamp: FieldValue.serverTimestamp(),
                });
                const systemMessageRef = chatDoc.ref.collection("messages").doc();
                transaction.set(systemMessageRef, {
                    senderId: "system",
                    text: cancelMessage,
                    timestamp: FieldValue.serverTimestamp(),
                    type: "SYSTEM",
                    systemType: "request_cancelled",
                });
            }
        } 
        else if (actionType === "mark_complete") {
            const responderId = requestData.responderId;
            if (requestData.status !== 'in_progress') { throw new Error(`Cannot mark complete. Current status: ${requestData.status}.`); }
            if (createdBy !== responderId) { throw new Error(`User ${createdBy} is not the assigned helper.`); }
            const chatRef = db.collection("chats").doc(requestId);
            const systemMessageRef = chatRef.collection("messages").doc();
            transaction.set(systemMessageRef, {
                senderId: "system",
                text: `${requestData.responderName} (the helper) has marked this job as complete. Please confirm to finalize the request.`,
                timestamp: FieldValue.serverTimestamp(),
                type: "SYSTEM",
                systemType: "job_completed",
            });
            transaction.update(requestRef, { status: "pending_completion" });
        }
        else if (actionType === "confirm_complete") {
            const responderId = requestData.responderId;
            if (requestData.status !== 'pending_completion') { throw new Error(`Cannot confirm completion. Current status: ${requestData.status}.`); }
            if (createdBy !== requesterId) { throw new Error(`User ${createdBy} is not authorized to confirm completion.`); }
            if (!requesterId || !responderId) { throw new Error("Missing user IDs on request document."); }
            const responderProfileRef = db.collection("users").doc(responderId);
            transaction.update(responderProfileRef, { helpsCompleted: FieldValue.increment(1) });
            const chatRef = db.collection("chats").doc(requestId);
            const finalMessageRef = chatRef.collection("messages").doc();
            transaction.set(finalMessageRef, {
                senderId: "system",
                text: "Job confirmed and completed! This chat is now archived.",
                timestamp: FieldValue.serverTimestamp(),
                type: "SYSTEM",
                systemType: "job_confirmed",
            });
            transaction.update(requestRef, { status: "completed" });
        }
        transaction.update(actionRef, {
            status: "processed",
            processedAt: FieldValue.serverTimestamp()
        });
      });
      logger.info(`✅ [SUCCESS] Transaction committed for action '${actionType}'.`);
      if (actionType === 'accept_offer') {
          logger.info(`Cleaning up offers for request ${requestId}.`);
          const offersSnapshot = await requestRef.collection("offers").get();
          const batch = db.batch();
          offersSnapshot.docs.forEach(doc => {
              batch.delete(doc.ref);
          });
          await batch.commit();
      }
    } catch (error) {
      logger.error(`❌ [ERROR] Transaction failed for action '${actionType}':`, error);
      await actionRef.update({ status: "error", errorMessage: error.message });
    }
  }
);

/**
 * Keeps a real-time count of offers on a request document.
 */
export const updateOfferCount = onDocumentWritten(
    "requests/{requestId}/offers/{offerId}",
    async (event) => {
        const requestId = event.params.requestId;
        const requestRef = db.collection("requests").doc(requestId);
        try {
            const offersSnapshot = await requestRef.collection("offers").get();
            const offerCount = offersSnapshot.size;
            await requestRef.update({ offerCount: offerCount });
        } catch (error) {
            logger.error(`❌ Error updating offer count for request ${requestId}:`, error);
        }
    }
);

/**
 * Increments a user's `requestsPosted` count whenever they create a new request.
 */
export const incrementRequestsPosted = onDocumentCreated(
    "requests/{requestId}",
    async (event) => {
        const requestSnap = event.data;
        if (!requestSnap) return;
        const requestData = requestSnap.data();
        const userId = requestData.userId;
        if (!userId) return;
        const userRef = db.collection("users").doc(userId);
        try {
            await userRef.update({ requestsPosted: FieldValue.increment(1) });
        } catch (error) {
            logger.error(`❌ Failed to increment requestsPosted for user ${userId}:`, error);
        }
    }
);

/**
 * Cleans up all associated data and decrements the user's post count when a request is deleted.
 */
export const cleanupRequestData = onDocumentDeleted(
  "requests/{requestId}",
  async (event) => {
    const requestId = event.params.requestId;
    const deletedData = event.data.data();
    const userId = deletedData.userId;
    logger.info(`🔥 [CLEANUP] Request ${requestId} by user ${userId} was deleted.`);
    if (userId) {
        const userRef = db.collection("users").doc(userId);
        try {
            await userRef.update({ requestsPosted: FieldValue.increment(-1) });
            logger.info(`✅ [CLEANUP] Decremented requestsPosted count for user ${userId}.`);
        } catch (error) {
            logger.error(`❌ [ERROR] Failed to decrement requestsPosted for user ${userId}:`, error);
        }
    }
    const chatRef = db.collection("chats").doc(requestId);
    try {
      const messagesSnapshot = await chatRef.collection("messages").get();
      if (!messagesSnapshot.empty) {
        const batch = db.batch();
        messagesSnapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
        });
        await batch.commit();
        logger.info(`[CLEANUP] Deleted ${messagesSnapshot.size} messages for chat ${requestId}.`);
      }
      const chatDoc = await chatRef.get();
      if (chatDoc.exists) {
        await chatRef.delete();
        logger.info(`[CLEANUP] Deleted chat document ${requestId}.`);
      }
    } catch (error) {
      logger.error(`❌ [ERROR] Failed to clean up chat data for request ${requestId}:`, error);
    }
  }
);

// ✅ --- ADD THIS ENTIRE NEW FUNCTION ---
/**
 * Triggers when a user document is deleted from Firestore.
 * This function performs a complete cleanup of all data associated with that user.
 */
export const cleanupUserData = onDocumentDeleted(
    "users/{userId}",
    async (event) => {
        const userId = event.params.userId;
        logger.info(`🔥 [USER DELETED] Starting cleanup for user ${userId}.`);

        const batch = db.batch();

        try {
            // 1. Delete the user from Firebase Authentication
            await auth.deleteUser(userId);
            logger.info(`[CLEANUP] Deleted user from Firebase Auth: ${userId}`);

            // 1.5. Delete all user-related files from Cloud Storage
            const bucket = storage.bucket(); 
            const filePath = `profile_images/${userId}.jpg`;
            const file = bucket.file(filePath);

            try {
                await file.delete();
                logger.info(`[CLEANUP] Deleted profile image ${filePath} from Storage.`);
            } catch (storageError) {
                if (storageError.code === 404) {
                    logger.warn(`[CLEANUP] No profile image found to delete at ${filePath}. Skipping.`);
                } else {
                    logger.error(`[CLEANUP] Error deleting profile image ${filePath}:`, storageError);
                }
            }

            // 2. Find and delete all help requests created by the user
            const requestsQuery = db.collection("requests").where("userId", "==", userId);
            const requestsSnapshot = await requestsQuery.get();
            if (!requestsSnapshot.empty) {
                requestsSnapshot.forEach((doc) => {
                    logger.info(`[CLEANUP] Deleting request ${doc.id} created by user ${userId}.`);
                    batch.delete(doc.ref);
                });
                // Note: Deleting these requests will automatically trigger the 'cleanupRequestData'
                // function for each one, which will handle deleting chats and messages.
            }

            // 3. Find all offers the user has made and delete them
            // This is a collection group query to find offers across all requests.
            const offersQuery = db.collectionGroup("offers").where("helperId", "==", userId);
            const offersSnapshot = await offersQuery.get();
            if (!offersSnapshot.empty) {
                offersSnapshot.forEach((doc) => {
                    logger.info(`[CLEANUP] Deleting offer ${doc.id} made by user ${userId}.`);
                    batch.delete(doc.ref);
                });
            }
            
            // Commit all the batched deletions to Firestore
            await batch.commit();
            logger.info(`✅ [CLEANUP] Firestore data cleanup complete for user ${userId}.`);

        } catch (error) {
            logger.error(`❌ [ERROR] Failed during cleanup for user ${userId}:`, error);
        }
    }
);

/**
 * Checks chat documents on update. If both participants have "deleted" the chat,
 * it performs a hard delete on the chat and its 'messages' subcollection.
 */
export const handleChatDeletion = onDocumentWritten(
    "chats/{chatId}",
    async (event) => {
        const afterData = event.data?.after.data();
        const chatId = event.params.chatId;

        // Exit if the document was just created or doesn't have the necessary fields
        if (!afterData || !afterData.participants || !afterData.deletedBy) {
            return;
        }

        const participants = afterData.participants;
        const deletedBy = afterData.deletedBy;

        // Check if the number of users who deleted the chat is equal to or greater than the number of participants
        if (deletedBy.length >= participants.length) {
            logger.info(`[HARD DELETE] Both users have deleted chat ${chatId}. Cleaning up.`);
            
            const chatRef = db.collection("chats").doc(chatId);
            const messagesRef = chatRef.collection("messages");

            try {
                // Delete the 'messages' subcollection first
                const messagesSnapshot = await messagesRef.get();
                if (!messagesSnapshot.empty) {
                    const batch = db.batch();
                    messagesSnapshot.docs.forEach(doc => {
                        batch.delete(doc.ref);
                    });
                    await batch.commit();
                    logger.info(`[HARD DELETE] Deleted ${messagesSnapshot.size} messages for chat ${chatId}.`);
                }

                // Finally, delete the parent chat document
                await chatRef.delete();
                logger.info(`✅ [HARD DELETE] Successfully deleted chat document ${chatId}.`);

            } catch (error) {
                logger.error(`❌ [ERROR] Failed during hard delete for chat ${chatId}:`, error);
            }
        }
    }
);

/**
 * Sends a push notification to a user when they receive a new message.
 */
export const sendNewMessageNotification = onDocumentCreated(
    "chats/{chatId}/messages/{messageId}",
    async (event) => {
        const messageData = event.data.data();
        const chatId = event.params.chatId;

        // Exit if the message is a system message
        if (messageData.senderId === "system") {
            logger.info(`[FCM] System message detected in chat ${chatId}. No notification sent.`);
            return;
        }

        try {
            // Get the chat document to find the participants
            const chatRef = db.collection("chats").doc(chatId);
            const chatDoc = await chatRef.get();
            if (!chatDoc.exists) {
                logger.error(`[FCM] Chat document ${chatId} not found.`);
                return;
            }
            const chatData = chatDoc.data();
            const senderId = messageData.senderId;

            // Determine the recipient's ID
            const recipientId = chatData.participants.find(id => id !== senderId);
            if (!recipientId) {
                logger.warn(`[FCM] Could not find a recipient in chat ${chatId}.`);
                return;
            }

            // Get the recipient's user profile to find their FCM token
            const userRef = db.collection("users").doc(recipientId);
            const userDoc = await userRef.get();
            if (!userDoc.exists || !userDoc.data().fcmToken) {
                logger.warn(`[FCM] Recipient ${recipientId} does not have an FCM token.`);
                return;
            }
            const recipientToken = userDoc.data().fcmToken;

            // Get the sender's name from the chat document
            const senderName = chatData.participantInfo[senderId]?.name || "Someone";
            
            // Construct the notification payload
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: messageData.text,
                },
                token: recipientToken,
                data: { // Optional: send data to handle clicks in the app
                    chatId: chatId,
                    senderName: senderName
                }
            };
            
            // Send the notification
            await getMessaging().send(payload);
            logger.info(`[FCM] Successfully sent notification to user ${recipientId}.`);

        } catch (error) {
            logger.error(`[FCM] ❌ Error sending new message notification for chat ${chatId}:`, error);
        }
    }
);

/**
 * Triggers when a request's status is updated. If it's marked "completed",
 * it kicks off the review process for both the requester and the helper.
 */
export const onRequestCompleted = onDocumentWritten("requests/{requestId}", async (event) => {
    const change = event.data;
    if (!change || !change.before || !change.after) {
        logger.info("[Review] No change data found.");
        return;
    }

    const beforeStatus = change.before.data().status;
    const afterStatus = change.after.data().status;
    const requestData = change.after.data();

    // Trigger only when status changes TO "completed"
    if (beforeStatus !== "completed" && afterStatus === "completed") {
        const requestId = event.params.requestId;
        const requesterId = requestData.userId;
        const helperId = requestData.responderId;
        const requesterName = requestData.userName;
        const helperName = requestData.responderName;

        logger.info(`[Review] Request ${requestId} completed. Initiating review process.`);

        // 1. Update the request with the initial review status
        await db.collection("requests").doc(requestId).update({
            reviewStatus: {
                [requesterId]: "pending",
                [helperId]: "pending",
            },
        });

        // 2. Send push notifications to both users
        const requesterTokenDoc = await db.collection("users").doc(requesterId).get();
        const helperTokenDoc = await db.collection("users").doc(helperId).get();

        if (requesterTokenDoc.exists && requesterTokenDoc.data().fcmToken) {
            await getMessaging().send({
                token: requesterTokenDoc.data().fcmToken,
                notification: {
                    title: "How was your experience?",
                    body: `Leave feedback for ${helperName} to earn Trust Badges!`,
                },
                data: {
                    screen: "review",
                    requestId: requestId,
                    revieweeId: helperId, // The person they are reviewing
                },
            });
        }

        if (helperTokenDoc.exists && helperTokenDoc.data().fcmToken) {
            await getMessaging().send({
                token: helperTokenDoc.data().fcmToken,
                notification: {
                    title: "How was your experience?",
                    body: `Leave feedback for ${requesterName} to earn Trust Badges!`,
                },
                data: {
                    screen: "review",
                    requestId: requestId,
                    revieweeId: requesterId, // The person they are reviewing
                },
            });
        }
    }
});


/**
 * Triggers when a new review is created. It updates the recipient's
 * profile with the new trust badges.
 */
export const onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
    const reviewSnap = event.data;
    if (!reviewSnap) {
        logger.warn("[Review] Review document data is missing.");
        return;
    }

    const reviewData = reviewSnap.data();
    const { revieweeId, badges, requestId, reviewerId } = reviewData;

    logger.info(`[Review] New review created for user ${revieweeId}.`);

    // 1. Update the user's profile with the new badges
    const userRef = db.collection("users").doc(revieweeId);
    const transactionUpdates = {};
    badges.forEach((badge) => {
        transactionUpdates[`trustBadges.${badge}`] = FieldValue.increment(1);
    });

    await db.runTransaction(async (transaction) => {
        transaction.update(userRef, transactionUpdates);
    });


    // 2. Update the review status on the original request
    const requestRef = db.collection("requests").doc(requestId);
    await requestRef.update({
        [`reviewStatus.${reviewerId}`]: "completed",
    });

    logger.info(`[Review] Successfully updated badges for user ${revieweeId}.`);
});