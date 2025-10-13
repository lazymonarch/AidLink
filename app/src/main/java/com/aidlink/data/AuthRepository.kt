
package com.aidlink.data

import android.app.Activity
import android.util.Log
import com.aidlink.model.Chat
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
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
            // ✅ FIXED: Using the modern KTX toObject()
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
                // ✅ FIXED: Using the modern KTX toObject()
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
        val listener = db.collection("requests")
            .whereEqualTo("status", "open")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // ✅ FIXED: Using the modern KTX toObject()
                val requests = snapshots?.map { it.toObject<HelpRequest>().copy(id = it.id) }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getRequestById(requestId: String): HelpRequest? {
        return try {
            val document = db.collection("requests").document(requestId).get().await()
            // ✅ FIXED: Using the modern KTX toObject()
            document.toObject<HelpRequest>()?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching request by ID", e)
            null
        }
    }

    fun getMyActivityRequests(userId: String): Flow<List<HelpRequest>> = callbackFlow {
        val listener = db.collection("requests")
            // This query gets all documents where the user is either the requester OR the responder.
            .whereIn(
                "status",
                listOf("pending", "in_progress", "completed", "pending_completion")
            )
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allRequests = snapshots?.mapNotNull { it.toObject<HelpRequest>().copy(id = it.id) } ?: emptyList()

                // The filtering logic is now simpler and more robust
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
                // ✅ FIXED: Using the modern KTX toObject()
                val messages = snapshots?.mapNotNull { it.toObject<Message>() }
                trySend(messages ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, message: Message): Boolean {
        return try {
            val chatRef = db.collection("chats").document(chatId)
            val messageRef = chatRef.collection("messages").document()
            db.runBatch { batch ->
                batch.set(messageRef, message)
                batch.update(chatRef, mapOf(
                    "lastMessage" to message.text,
                    "lastMessageTimestamp" to message.timestamp
                ))
            }.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending message", e)
            false
        }
    }

    // ✅ FIXED: Removed the duplicate function
    suspend fun enqueueRequestAction(requestId: String, actionType: String, extraData: Map<String, Any> = emptyMap()): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
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
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to enqueue action '$actionType' for request $requestId", e)
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }
}