
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
 * This is the main state machine for request workflow.
 */
export const handleRequestActions = onDocumentCreated(
  "requests/{requestId}/actions/{actionId}",
  async (event) => {
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
    
    logger.info(`⚙️ [START] Processing action '${actionType}' for request ${requestId} by user ${createdBy}`);
    
    try {
      await db.runTransaction(async (transaction) => {
        const requestDoc = await transaction.get(requestRef);
        if (!requestDoc.exists) {
          throw new Error("Request document not found.");
        }
        
        // Pre-fetch chat doc if needed for cancel action
        let chatDoc = null;
        if (actionType === "cancel_request" || actionType === "mark_complete" || 
            actionType === "mark_not_complete" || actionType === "reject_completion") {
          const chatRef = db.collection("chats").doc(requestId);
          chatDoc = await transaction.get(chatRef);
        }
        
        const requestData = requestDoc.data();
        const requesterId = requestData.userId;

        // ============================================
        // ACTION: MAKE OFFER
        // ============================================
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
          
          logger.info(`✅ Created offer from ${helperName} (${createdBy})`);
        }
        
        // ============================================
        // ACTION: ACCEPT OFFER
        // ============================================
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
          
          // Update request status
          transaction.update(requestRef, {
            status: "in_progress",
            responderId: helperId,
            responderName: responderData.name,
            participants: [requesterId, helperId],
          });
          
          // Create chat document
          const chatRef = db.collection("chats").doc(requestId);
          const systemMessageText = `${requesterData.name} accepted the offer! You can now chat.`;
          
          transaction.set(chatRef, {
            participants: [requesterId, helperId],
            participantInfo: {
              [requesterId]: { 
                name: requesterData.name, 
                photoUrl: requesterData.photoUrl || "" 
              },
              [helperId]: { 
                name: responderData.name, 
                photoUrl: responderData.photoUrl || "" 
              }
            },
            createdAt: FieldValue.serverTimestamp(),
            lastMessage: systemMessageText,
            lastMessageTimestamp: FieldValue.serverTimestamp(),
            // Initialize chat with request details
            requestId: requestId,
            requestStatus: "in_progress",
            helperId: helperId,
            requesterId: requesterId,
            deletedBy: [], // Initialize empty array for soft delete tracking
            unreadCount: {
              [requesterId]: 0,
              [helperId]: 0
            }
          });
          
          // Add initial system message
          const initialMessageRef = chatRef.collection("messages").doc();
          transaction.set(initialMessageRef, {
            senderId: "system",
            text: systemMessageText,
            timestamp: FieldValue.serverTimestamp(),
            type: "SYSTEM",
            systemType: "offer_accepted",
          });
          
          logger.info(`✅ Accepted offer from ${responderData.name}. Chat created.`);
        }
        
        // ============================================
        // ACTION: CANCEL REQUEST
        // ============================================
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
          
          logger.info(`✅ Request cancelled by requester`);
        }
        
        // ============================================
        // ACTION: MARK COMPLETE
        // ============================================
        else if (actionType === "mark_complete") {
          const responderId = requestData.responderId;
          if (requestData.status !== 'in_progress') {
            throw new Error(`Cannot mark complete. Current status: ${requestData.status}.`);
          }
          if (createdBy !== responderId) {
            throw new Error(`User ${createdBy} is not the assigned helper.`);
          }
          
          if (chatDoc && chatDoc.exists) {
            const systemMessageRef = chatDoc.ref.collection("messages").doc();
            transaction.set(systemMessageRef, {
              senderId: "system",
              text: `${requestData.responderName} (the helper) has marked this job as complete. Please confirm to finalize the request.`,
              timestamp: FieldValue.serverTimestamp(),
              type: "SYSTEM",
              systemType: "job_completed",
            });
            
            transaction.update(chatDoc.ref, {
              lastMessage: "Job marked as complete",
              lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
          }
          
          transaction.update(requestRef, { status: "pending_completion" });
          logger.info(`✅ Job marked complete by helper`);
        }
        
        // ============================================
        // ACTION: MARK NOT COMPLETE
        // ============================================
        else if (actionType === "mark_not_complete") {
          if (requestData.status !== 'in_progress') {
            throw new Error("Cannot mark not complete on a request that is not in progress.");
          }
          if (createdBy !== requestData.responderId) {
            throw new Error("Only the assigned helper can mark a job as not complete.");
          }
          
          if (chatDoc && chatDoc.exists) {
            const systemMessageRef = chatDoc.ref.collection("messages").doc();
            transaction.set(systemMessageRef, {
              senderId: "system",
              text: `${requestData.responderName} (the helper) has marked this job as not complete. Please discuss any issues.`,
              timestamp: FieldValue.serverTimestamp(),
              type: "SYSTEM",
              systemType: "job_not_completed",
            });
            
            transaction.update(chatDoc.ref, {
              lastMessage: "Job marked as not complete",
              lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
          }
          
          // No status change on request, it remains 'in_progress'
          logger.info(`✅ Job marked as not complete by helper`);
        }
        
        // ============================================
        // ACTION: CONFIRM COMPLETE
        // ============================================
        else if (actionType === "confirm_complete") {
          const responderId = requestData.responderId;
          if (requestData.status !== 'pending_completion') {
            throw new Error(`Cannot confirm completion. Current status: ${requestData.status}.`);
          }
          if (createdBy !== requesterId) {
            throw new Error(`User ${createdBy} is not authorized to confirm completion.`);
          }
          if (!requesterId || !responderId) {
            throw new Error("Missing user IDs on request document.");
          }
          
          // Increment helper's completed count
          const responderProfileRef = db.collection("users").doc(responderId);
          transaction.update(responderProfileRef, { 
            helpsCompleted: FieldValue.increment(1) 
          });
          
          if (chatDoc && chatDoc.exists) {
            const finalMessageRef = chatDoc.ref.collection("messages").doc();
            transaction.set(finalMessageRef, {
              senderId: "system",
              text: "Job confirmed and completed! This chat is now archived.",
              timestamp: FieldValue.serverTimestamp(),
              type: "SYSTEM",
              systemType: "job_confirmed",
            });
            
            transaction.update(chatDoc.ref, {
              lastMessage: "Job completed",
              lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
          }
          
          transaction.update(requestRef, { status: "completed" });
          logger.info(`✅ Job confirmed complete by requester`);
        }
        
        // ============================================
        // ACTION: REJECT COMPLETION
        // ============================================
        else if (actionType === "reject_completion") {
          if (requestData.status !== 'pending_completion') {
            throw new Error("Cannot reject completion on a request that is not pending completion.");
          }
          if (createdBy !== requesterId) {
            throw new Error("Only the requester can reject completion.");
          }
          
          if (chatDoc && chatDoc.exists) {
            const systemMessageRef = chatDoc.ref.collection("messages").doc();
            transaction.set(systemMessageRef, {
              senderId: "system",
              text: `${requestData.userName} (the requester) has not confirmed completion. The job is now back in progress.`,
              timestamp: FieldValue.serverTimestamp(),
              type: "SYSTEM",
              systemType: "completion_rejected",
            });
            
            transaction.update(chatDoc.ref, {
              lastMessage: "Completion rejected",
              lastMessageTimestamp: FieldValue.serverTimestamp(),
            });
          }
          
          // Revert status back to in_progress
          transaction.update(requestRef, { status: "in_progress" });
          logger.info(`✅ Completion rejected by requester`);
        }
        
        // Mark action as processed
        transaction.update(actionRef, {
          status: "processed",
          processedAt: FieldValue.serverTimestamp()
        });
      });
      
      logger.info(`✅ [SUCCESS] Transaction committed for action '${actionType}'.`);
      
      // Clean up offers after accepting one
      if (actionType === 'accept_offer') {
        logger.info(`Cleaning up offers for request ${requestId}.`);
        const offersSnapshot = await requestRef.collection("offers").get();
        const batch = db.batch();
        offersSnapshot.docs.forEach(doc => {
          batch.delete(doc.ref);
        });
        await batch.commit();
        logger.info(`✅ Deleted ${offersSnapshot.size} offers.`);
      }
      
    } catch (error) {
      logger.error(`❌ [ERROR] Transaction failed for action '${actionType}':`, error);
      await actionRef.update({ 
        status: "error", 
        errorMessage: error.message,
        errorTimestamp: FieldValue.serverTimestamp()
      });
    }
  }
);

/**
 * 🔥 CRITICAL FUNCTION: Syncs request status to chat document
 * This keeps the chat UI in sync with the actual request status
 */
export const syncRequestStatusToChat = onDocumentWritten(
  "requests/{requestId}", 
  async (event) => {
    const change = event.data;
    if (!change || !change.before || !change.after) {
      logger.info("[SYNC] No change data found for request update.");
      return;
    }

    const beforeData = change.before.data();
    const afterData = change.after.data();
    const beforeStatus = beforeData?.status;
    const afterStatus = afterData?.status;
    const requestId = event.params.requestId;

    // Only run if the status has actually changed
    if (beforeStatus === afterStatus) {
      return;
    }

    logger.info(`[SYNC] Request ${requestId} status changed from '${beforeStatus}' to '${afterStatus}'.`);
    
    const chatRef = db.collection("chats").doc(requestId);
    
    try {
      const chatDoc = await chatRef.get();
      
      // Check if a chat document actually exists for this request
      if (!chatDoc.exists) {
        logger.info(`[SYNC] No chat document found for request ${requestId}. Skipping sync.`);
        return;
      }
      
      // Update the chat's requestStatus field
      await chatRef.update({ 
        requestStatus: afterStatus,
        lastUpdated: FieldValue.serverTimestamp()
      });
      
      logger.info(`[SYNC] ✅ Successfully synced status to chat document ${requestId}.`);
      
      // Optionally add a system message for major status changes
      if (afterStatus === 'completed') {
        await chatRef.collection('messages').add({
          senderId: 'system',
          text: 'This job has been completed. Thank you!',
          timestamp: FieldValue.serverTimestamp(),
          type: 'SYSTEM',
          systemType: 'status_change'
        });
      }
      
    } catch (error) {
      logger.error(`[SYNC] ❌ Error updating chat document ${requestId}:`, error);
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
      logger.info(`[OFFER COUNT] Updated to ${offerCount} for request ${requestId}`);
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
      logger.info(`[STATS] Incremented requestsPosted for user ${userId}`);
    } catch (error) {
      logger.error(`❌ Failed to increment requestsPosted for user ${userId}:`, error);
    }
  }
);

/**
 * Cleans up all associated data when a request is deleted.
 */
export const cleanupRequestData = onDocumentDeleted(
  "requests/{requestId}",
  async (event) => {
    const requestId = event.params.requestId;
    const deletedData = event.data.data();
    const userId = deletedData.userId;
    
    logger.info(`🔥 [CLEANUP] Request ${requestId} by user ${userId} was deleted.`);
    
    // Decrement user's request count
    if (userId) {
      const userRef = db.collection("users").doc(userId);
      try {
        await userRef.update({ requestsPosted: FieldValue.increment(-1) });
        logger.info(`✅ [CLEANUP] Decremented requestsPosted count for user ${userId}.`);
      } catch (error) {
        logger.error(`❌ [ERROR] Failed to decrement requestsPosted for user ${userId}:`, error);
      }
    }
    
    // Clean up chat and messages
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

/**
 * 🔥 CRITICAL FUNCTION: Complete user data cleanup on account deletion
 */
export const cleanupUserData = onDocumentDeleted(
  "users/{userId}",
  async (event) => {
    const userId = event.params.userId;
    logger.info(`🔥 [USER DELETED] Starting cleanup for user ${userId}.`);

    try {
      // 1. Delete user from Firebase Authentication
      try {
        await auth.deleteUser(userId);
        logger.info(`[CLEANUP] Deleted user from Firebase Auth: ${userId}`);
      } catch (authError) {
        if (authError.code === 'auth/user-not-found') {
          logger.warn(`[CLEANUP] User ${userId} not found in Auth (already deleted).`);
        } else {
          throw authError;
        }
      }

      // 2. Delete profile image from Cloud Storage
      const bucket = storage.bucket();
      const filePath = `profile_images/${userId}.jpg`;
      const file = bucket.file(filePath);

      try {
        await file.delete();
        logger.info(`[CLEANUP] Deleted profile image ${filePath} from Storage.`);
      } catch (storageError) {
        if (storageError.code === 404) {
          logger.warn(`[CLEANUP] No profile image found at ${filePath}. Skipping.`);
        } else {
          logger.error(`[CLEANUP] Error deleting profile image ${filePath}:`, storageError);
        }
      }

      // 3. Delete all help requests created by the user
      const batch = db.batch();
      const requestsQuery = db.collection("requests").where("userId", "==", userId);
      const requestsSnapshot = await requestsQuery.get();
      
      if (!requestsSnapshot.empty) {
        requestsSnapshot.forEach((doc) => {
          logger.info(`[CLEANUP] Deleting request ${doc.id} created by user ${userId}.`);
          batch.delete(doc.ref);
        });
      }

      // 4. Delete all offers made by the user
      const offersQuery = db.collectionGroup("offers").where("helperId", "==", userId);
      const offersSnapshot = await offersQuery.get();
      
      if (!offersSnapshot.empty) {
        offersSnapshot.forEach((doc) => {
          logger.info(`[CLEANUP] Deleting offer ${doc.id} made by user ${userId}.`);
          batch.delete(doc.ref);
        });
      }
      
      // 5. Delete all reviews written by the user
      const reviewsQuery = db.collection("reviews").where("reviewerId", "==", userId);
      const reviewsSnapshot = await reviewsQuery.get();
      
      if (!reviewsSnapshot.empty) {
        reviewsSnapshot.forEach((doc) => {
          logger.info(`[CLEANUP] Deleting review ${doc.id} by user ${userId}.`);
          batch.delete(doc.ref);
        });
      }
      
      // Commit all deletions
      await batch.commit();
      logger.info(`✅ [CLEANUP] Firestore data cleanup complete for user ${userId}.`);

    } catch (error) {
      logger.error(`❌ [ERROR] Failed during cleanup for user ${userId}:`, error);
    }
  }
);

/**
 * Handles hard deletion of chats when both users have deleted it
 */
export const handleChatDeletion = onDocumentWritten(
  "chats/{chatId}",
  async (event) => {
    const afterData = event.data?.after.data();
    const chatId = event.params.chatId;

    // Exit if document was just created or doesn't have necessary fields
    if (!afterData || !afterData.participants || !afterData.deletedBy) {
      return;
    }

    const participants = afterData.participants;
    const deletedBy = afterData.deletedBy;

    // Check if all participants have deleted the chat
    if (deletedBy.length >= participants.length) {
      logger.info(`[HARD DELETE] All users have deleted chat ${chatId}. Cleaning up.`);
      
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
 * Sends push notification when a new message is received
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

      // Get the recipient's FCM token
      const userRef = db.collection("users").doc(recipientId);
      const userDoc = await userRef.get();
      
      if (!userDoc.exists || !userDoc.data().fcmToken) {
        logger.warn(`[FCM] Recipient ${recipientId} does not have an FCM token.`);
        return;
      }
      
      const recipientToken = userDoc.data().fcmToken;
      const senderName = chatData.participantInfo[senderId]?.name || "Someone";
      
      // Construct the notification payload
      const payload = {
        notification: {
          title: `New message from ${senderName}`,
          body: messageData.text.length > 100 
            ? messageData.text.substring(0, 97) + "..." 
            : messageData.text,
        },
        token: recipientToken,
        data: {
          chatId: chatId,
          senderName: senderName,
          screen: "chat"
        }
      };
      
      // Send the notification
      await getMessaging().send(payload);
      logger.info(`[FCM] ✅ Successfully sent notification to user ${recipientId}.`);

    } catch (error) {
      logger.error(`[FCM] ❌ Error sending new message notification for chat ${chatId}:`, error);
    }
  }
);

/**
 * Triggers review process when request is marked as completed
 */
export const onRequestCompleted = onDocumentWritten(
  "requests/{requestId}", 
  async (event) => {
    const change = event.data;
    if (!change || !change.before || !change.after) {
      logger.info("[Review] No change data found.");
      return;
    }

    const beforeStatus = change.before.data()?.status;
    const afterStatus = change.after.data()?.status;
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
            revieweeId: helperId,
            isHelperReviewing: "false"
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
            revieweeId: requesterId,
            isHelperReviewing: "true"
          },
        });
      }
      
      logger.info(`[Review] ✅ Sent review notifications to both users.`);
    }
  }
);

/**
 * Updates user's trust badges when a new review is created
 */
export const onReviewCreated = onDocumentCreated(
  "reviews/{reviewId}", 
  async (event) => {
    const reviewSnap = event.data;
    if (!reviewSnap) {
      logger.warn("[Review] Review document data is missing.");
      return;
    }

    const reviewData = reviewSnap.data();
    const { revieweeId, badges, requestId, reviewerId } = reviewData;

    logger.info(`[Review] New review created for user ${revieweeId} by ${reviewerId}.`);

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

    logger.info(`[Review] ✅ Successfully updated badges for user ${revieweeId}.`);
  }
);

/**
 * Increments unread message count when a new message is received
 */
export const updateUnreadCount = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const messageData = event.data.data();
    const chatId = event.params.chatId;
    const senderId = messageData.senderId;

    // Don't increment for system messages
    if (senderId === "system") {
      return;
    }

    try {
      const chatRef = db.collection("chats").doc(chatId);
      const chatDoc = await chatRef.get();

      if (!chatDoc.exists) {
        logger.error(`[UNREAD] Chat ${chatId} not found.`);
        return;
      }

      const chatData = chatDoc.data();
      const participants = chatData.participants;

      // Increment unread count for all participants except the sender
      const updates = {};
      participants.forEach(participantId => {
        if (participantId !== senderId) {
          updates[`unreadCount.${participantId}`] = FieldValue.increment(1);
        }
      });

      if (Object.keys(updates).length > 0) {
        await chatRef.update(updates);
        logger.info(`[UNREAD] Incremented unread count for chat ${chatId}`);
      }

    } catch (error) {
      logger.error(`[UNREAD] ❌ Error updating unread count for chat ${chatId}:`, error);
    }
  }
);
