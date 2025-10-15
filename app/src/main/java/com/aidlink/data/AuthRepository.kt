package com.aidlink.data

import android.app.Activity
import android.util.Log
import com.aidlink.model.Chat
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import com.aidlink.model.Offer
import com.aidlink.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseUser

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    private val tag = "AuthRepository"

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun getCurrentUser() = auth.currentUser

    suspend fun makeOffer(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "make_offer")
    }

    suspend fun acceptOffer(requestId: String, helperId: String): Boolean {
        val data = mapOf("helperId" to helperId)
        return enqueueRequestAction(requestId, "accept_offer", data)
    }

    fun getOffers(requestId: String): Flow<List<Offer>> = callbackFlow {
        val listener = db.collection("requests").document(requestId).collection("offers")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val offers = snapshots?.map { doc ->
                    doc.toObject<Offer>().copy(id = doc.id)
                } ?: emptyList()
                trySend(offers)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markJobAsComplete(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "mark_complete")
    }

    suspend fun cancelRequest(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "cancel_request")
    }

    suspend fun confirmCompletion(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "confirm_complete")
    }

    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Boolean {
        return try {
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "signInWithPhoneAuthCredential failed", e)
            false
        }
    }

    suspend fun isUserProfileExists(): Boolean {
        val user = getCurrentUser() ?: return false
        return try {
            val document = db.collection("users").document(user.uid).get().await()
            document.exists()
        } catch (e: Exception) {
            Log.e(tag, "isUserProfileExists failed", e)
            false
        }
    }

    suspend fun createUserProfile(userProfile: UserProfile): Boolean {
        val user = getCurrentUser() ?: return false
        return try {
            db.collection("users").document(user.uid).set(userProfile).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "createUserProfile failed", e)
            false
        }
    }

    suspend fun getUserProfileOnce(uid: String): UserProfile? {
        return try {
            db.collection("users").document(uid).get().await().toObject<UserProfile>()
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user profile", e)
            null
        }
    }

    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<UserProfile>())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createRequest(request: HelpRequest): Boolean {
        return try {
            db.collection("requests").add(request).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error creating request", e)
            false
        }
    }

    fun getOpenHelpRequests(): Flow<List<HelpRequest>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("requests")
            .whereEqualTo("status", "open")
            .whereNotEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Log.e(tag, "Firestore index missing. Please create it in the Firebase console.", error)
                    }
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshots?.map { it.toObject<HelpRequest>().copy(id = it.id) }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getRequestById(requestId: String): HelpRequest? {
        return try {
            val document = db.collection("requests").document(requestId).get().await()
            document.toObject<HelpRequest>()?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching request by ID", e)
            null
        }
    }

    fun getMyActivityRequests(userId: String): Flow<List<HelpRequest>> = callbackFlow {
        val listener = db.collection("requests")
            .whereIn(
                "status",
                listOf("open", "pending", "in_progress", "completed", "pending_completion")
            )
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allRequests = snapshots?.mapNotNull { it.toObject<HelpRequest>().copy(id = it.id) } ?: emptyList()

                val myRequests = allRequests.filter { it.userId == userId || it.responderId == userId }
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }

                trySend(myRequests)
            }
        awaitClose { listener.remove() }
    }


    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val userId = getCurrentUser()?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshots?.map { it.toObject<Chat>().copy(id = it.id) }
                trySend(chats ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshots?.mapNotNull { it.toObject<Message>() }
                trySend(messages ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, text: String): Boolean {
        val senderId = getCurrentUser()?.uid
        if (senderId == null) {
            Log.e(tag, "sendMessage failed: User is not logged in.")
            return false
        }

        Log.d(tag, "Attempting to send message to chatId: $chatId")
        return try {
            val chatRef = db.collection("chats").document(chatId)
            val messageRef = chatRef.collection("messages").document()

            val messageData = mapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(messageRef, messageData)
                batch.update(chatRef, mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                ))
            }.await()

            Log.i(tag, "Successfully sent message to chatId: $chatId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending message to chatId: $chatId. Check Firestore rules.", e)
            false
        }
    }

    suspend fun deleteRequest(requestId: String): Boolean {
        Log.d(tag, "Attempting to delete request: $requestId")
        return try {
            db.collection("requests").document(requestId).delete().await()
            Log.i(tag, "Successfully sent delete command for request: $requestId. Cleanup will be handled by the backend.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting request: $requestId. Check Firestore rules.", e)
            false
        }
    }

    private suspend fun enqueueRequestAction(
        requestId: String,
        actionType: String,
        extraData: Map<String, Any> = emptyMap()
    ): Boolean {
        Log.d(tag, "Attempting to enqueue action '$actionType' for request $requestId")
        return try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Log.e(tag, "Failed to enqueue action: User is not logged in.")
                return false
            }

            val action = mutableMapOf<String, Any>(
                "type" to actionType,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            action.putAll(extraData)

            db.collection("requests")
                .document(requestId)
                .collection("actions")
                .add(action)
                .await()
            Log.i(tag, "Successfully enqueued action '$actionType' for request $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to enqueue action '$actionType' for request $requestId", e)
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }
}