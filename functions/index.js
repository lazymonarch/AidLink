import { onDocumentCreated, onDocumentWritten, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { logger } from "firebase-functions";

initializeApp();
const db = getFirestore();
const auth = getAuth();

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
            const requesterName = requesterProfileDoc.data().name;
            const responderName = responderProfileDoc.data().name;
            transaction.update(requestRef, {
                status: "in_progress",
                responderId: helperId,
                responderName: responderName,
                participants: [requesterId, helperId],
            });
            const chatRef = db.collection("chats").doc(requestId);
            transaction.set(chatRef, {
                participants: [requesterId, helperId],
                participantInfo: {
                    [requesterId]: { name: requesterName },
                    [helperId]: { name: responderName }
                },
                createdAt: FieldValue.serverTimestamp(),
                lastMessage: `${requesterName} accepted the offer! You can now chat.`,
                lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
            const initialMessageRef = chatRef.collection("messages").doc();
            transaction.set(initialMessageRef, {
                senderId: "system",
                text: `${requesterName} accepted the offer! You can now chat.`,
                timestamp: FieldValue.serverTimestamp(),
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