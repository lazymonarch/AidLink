
package com.aidlink.data

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.aidlink.model.Chat
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import com.aidlink.model.Offer
import com.aidlink.model.UserProfile
import com.aidlink.utils.Geometries
import com.github.davidmoten.geo.GeoHash
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
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
    fun getCurrentUserId(): String? = auth.currentUser?.uid

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
                    doc.toObject(Offer::class.java).copy(id = doc.id)
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

    suspend fun markJobAsNotComplete(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "mark_not_complete")
    }

    suspend fun rejectJobCompletion(requestId: String): Boolean {
        return enqueueRequestAction(requestId, "reject_completion")
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
            db.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
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
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String? {
        return try {
            Log.d(tag, "New image provided. Uploading to Firebase Storage...")
            val photoRef = storage.reference.child("profile_images/$uid.jpg")
            photoRef.putFile(imageUri).await()
            val photoUrl = photoRef.downloadUrl.await().toString()
            Log.i(tag, "Image uploaded successfully. URL: $photoUrl")
            photoUrl
        } catch (e: Exception) {
            Log.e(tag, "Error uploading profile image for user: $uid", e)
            null
        }
    }
    suspend fun updateUserProfile(uid: String, userProfile: UserProfile): Boolean {
        return try {
            db.collection("users").document(uid).set(userProfile).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating user profile", e)
            false
        }
    }

    suspend fun updateUserLocation(uid: String, location: GeoPoint): Boolean {
        return try {
            db.collection("users").document(uid).update("location", location).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating user location", e)
            false
        }
    }

    suspend fun updateUserProfile(
        uid: String,
        name: String,
        bio: String,
        skills: List<String>,
        area: String,
        imageUri: Uri?
    ): Boolean {
        Log.d(tag, "Attempting to update profile for user: $uid")
        return try {
            var photoUrl = ""
            if (imageUri != null) {
                Log.d(tag, "New image provided. Uploading to Firebase Storage...")
                val photoRef = storage.reference.child("profile_images/$uid.jpg")
                photoRef.putFile(imageUri).await()
                photoUrl = photoRef.downloadUrl.await().toString()
                Log.i(tag, "Image uploaded successfully. URL: $photoUrl")
            }
            val userProfileRef = db.collection("users").document(uid)
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "bio" to bio,
                "skills" to skills,
                "area" to area
            )
            if (photoUrl.isNotEmpty()) {
                updates["photoUrl"] = photoUrl
            }

            userProfileRef.update(updates).await()
            Log.i(tag, "Successfully updated user profile in Firestore for user: $uid")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating user profile for user: $uid", e)
            false
        }
    }

    suspend fun updateProfilePhotoUrl(uid: String, photoUrl: String): Boolean {
        return try {
            db.collection("users").document(uid).update("photoUrl", photoUrl).await()
            Log.i(tag, "Successfully updated photo URL for user $uid")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating photo URL", e)
            false
        }
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

    fun getNearbyHelpRequests(
        center: GeoPoint,
        radiusInKm: Double
    ): Flow<List<HelpRequest>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(tag, "getNearbyHelpRequests: currentUserId=$currentUserId")

        val centerGeohash = GeoHash.encodeHash(center.latitude, center.longitude, 5)
        val geohashQueries = GeoHash.neighbours(centerGeohash).toMutableList()
        geohashQueries.add(centerGeohash)

        Log.d(tag, "getNearbyHelpRequests: center=${center.latitude},${center.longitude}, radius=${radiusInKm}km")
        Log.d(tag, "getNearbyHelpRequests: centerGeohash=$centerGeohash")
        Log.d(tag, "getNearbyHelpRequests: geohashQueries (${geohashQueries.size}) = $geohashQueries")

        val geohashQuery = db.collection("requests")
            .whereEqualTo("status", "open")
            .whereIn("geohashCoarse", geohashQueries)

        val geohashListener = geohashQuery.addSnapshotListener { geohashSnapshots, error ->
            if (error != null) {
                Log.e(tag, "Error listening to geohash requests", error)
                close(error)
                return@addSnapshotListener
            }

            val allFoundRequests = mutableMapOf<String, HelpRequest>()
            geohashSnapshots?.forEach { doc ->
                val request = doc.toObject(HelpRequest::class.java).copy(id = doc.id)
                allFoundRequests[doc.id] = request
            }

            Log.d(tag, "getNearbyHelpRequests: Found ${allFoundRequests.size} total requests from geohash query")

            val requestsInBox = allFoundRequests.values.filter { it.userId != currentUserId }
            Log.d(tag, "getNearbyHelpRequests: ${requestsInBox.size} requests after filtering current user")

            val requestsInRadius = requestsInBox.filter { request ->
                val requestLocation = Geometries.point(request.latitude, request.longitude)
                val centerLocation = Geometries.point(center.latitude, center.longitude)
                val distance = centerLocation.distance(requestLocation)
                distance <= radiusInKm
            }

            Log.d(tag, "getNearbyHelpRequests: Final result: ${requestsInRadius.size} requests within ${radiusInKm}km")
            trySend(requestsInRadius)
        }

        awaitClose { geohashListener.remove() }
    }


    suspend fun getRequestById(requestId: String): HelpRequest? {
        return try {
            val document = db.collection("requests").document(requestId).get().await()
            document.toObject(HelpRequest::class.java)?.copy(id = document.id)
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
                val allRequests = snapshots?.mapNotNull {
                    it.toObject(HelpRequest::class.java).copy(id = it.id)
                } ?: emptyList()

                val myRequests = allRequests.filter { it.userId == userId || it.responderId == userId }
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }

                trySend(myRequests)
            }
        awaitClose { listener.remove() }
    }

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val userId = getCurrentUser()?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chatsQuery = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)

        val chatsListener = chatsQuery.addSnapshotListener { chatsSnapshot, chatsError ->
            if (chatsError != null) {
                close(chatsError)
                return@addSnapshotListener
            }
            
            val chats = chatsSnapshot?.mapNotNull { doc ->
                try {
                    val chat = doc.toObject(Chat::class.java).copy(id = doc.id)
                    
                    val otherUserId = chat.participants.firstOrNull { it != userId } ?: return@mapNotNull null
                    val otherUserInfo = chat.participantInfo[otherUserId]
                    
                    @Suppress("UNCHECKED_CAST")
                    val unreadCountMap = doc.get("unreadCount") as? Map<String, Long>
                    val myUnreadCount = unreadCountMap?.get(userId)?.toInt() ?: 0
                    
                    val requestStatus = doc.getString("requestStatus") ?: chat.requestStatus

                    chat.copy(
                        otherUserName = otherUserInfo?.get("name") as? String ?: "Unknown User",
                        otherUserPhotoUrl = otherUserInfo?.get("photoUrl") as? String ?: "",
                        unreadCount = myUnreadCount,
                        requestStatus = requestStatus
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing chat: ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            
            val visibleChats = chats.filter { !it.deletedBy.contains(userId) }
            
            trySend(visibleChats)
        }

        awaitClose { chatsListener.remove() }
    }


    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshots?.mapNotNull {
                    it.toObject(Message::class.java)
                }
                trySend(messages ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getChatStream(chatId: String): Flow<Chat?> = callbackFlow {
        val userId = getCurrentUserId() ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }

        val chatRef = db.collection("chats").document(chatId)
        val listener = chatRef.addSnapshotListener { doc, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (doc == null || !doc.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            try {
                val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                if (chat == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val otherUserId = chat.participants.firstOrNull { it != userId }
                val otherUserInfo = if (otherUserId != null) {
                    chat.participantInfo[otherUserId]
                } else null
                
                @Suppress("UNCHECKED_CAST")
                val unreadCountMap = doc.get("unreadCount") as? Map<String, Long>
                val myUnreadCount = unreadCountMap?.get(userId)?.toInt() ?: 0

                val requestStatus = doc.getString("requestStatus") ?: "in_progress"

                val enrichedChat = chat.copy(
                    otherUserName = otherUserInfo?.get("name") as? String ?: "Unknown User",
                    otherUserPhotoUrl = otherUserInfo?.get("photoUrl") as? String ?: "",
                    unreadCount = myUnreadCount,
                    requestStatus = requestStatus
                )
                trySend(enrichedChat)

            } catch (e: Exception) {
                Log.e(tag, "Error parsing chat: ${doc.id}", e)
                close(e)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun hideChatForCurrentUser(chatId: String): Boolean {
        val uid = getCurrentUser()?.uid ?: return false
        Log.d(tag, "Attempting to hide chat $chatId for user $uid")
        return try {
            db.collection("chats").document(chatId).update(
                "deletedBy", FieldValue.arrayUnion(uid)
            ).await()
            Log.i(tag, "Successfully hid chat $chatId for user $uid")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error hiding chat: $chatId", e)
            false
        }
    }

    suspend fun hideChatsForCurrentUser(chatIds: List<String>): Boolean {
        val uid = getCurrentUser()?.uid ?: return false
        Log.d(tag, "Attempting to hide ${chatIds.size} chats for user $uid")
        return try {
            val batch = db.batch()
            chatIds.forEach { chatId ->
                val chatRef = db.collection("chats").document(chatId)
                batch.update(chatRef, "deletedBy", FieldValue.arrayUnion(uid))
            }
            batch.commit().await()
            Log.i(tag, "Successfully hid ${chatIds.size} chats for user $uid")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error hiding multiple chats", e)
            false
        }
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
                "timestamp" to FieldValue.serverTimestamp(),
                "isSent" to true
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

    fun observeTypingStatus(chatId: String, otherUserId: String): Flow<Boolean> = flow {
        // Temporarily disabled to prevent crashes
        emit(false)
    }

    suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {
        // Temporarily disabled
        return
    }

    suspend fun markChatAsRead(chatId: String) {
        val userId = getCurrentUser()?.uid ?: return
        try {
            db.collection("chats").document(chatId).update("unreadCount.$userId", 0).await()
        } catch (e: Exception) {
            Log.e(tag, "Error marking chat as read", e)
        }
    }

    fun resendMessage(chatId: String, messageId: String) {
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Boolean {
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting message", e)
            false
        }
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
    }

    fun sendImageMessage(chatId: String, imageUri: String) {
    }

    fun sendLocationMessage(chatId: String, lat: Double, lng: Double) {
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

    suspend fun deleteUserAccount(): Boolean {
        val uid = getCurrentUser()?.uid
        if (uid == null) {
            Log.e(tag, "deleteUserAccount failed: User is not logged in.")
            return false
        }

        Log.d(tag, "Attempting to delete user profile document for user: $uid to trigger backend cleanup.")
        return try {
            db.collection("users").document(uid).delete().await()
            Log.i(tag, "Successfully deleted user profile document for $uid. Backend will now proceed.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting user profile document for user: $uid", e)
            false
        }
    }

    suspend fun updateRequest(requestId: String, updatedRequest: HelpRequest): Boolean {
        Log.d(tag, "Attempting to update request: $requestId")
        return try {
            val requestRef = db.collection("requests").document(requestId)
            val updates = mapOf(
                "title" to updatedRequest.title,
                "description" to updatedRequest.description,
                "category" to updatedRequest.category,
                "type" to updatedRequest.type
            )
            requestRef.update(updates).await()
            Log.i(tag, "Successfully updated request: $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating request: $requestId", e)
            false
        }
    }

    suspend fun enqueueRequestAction(
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
