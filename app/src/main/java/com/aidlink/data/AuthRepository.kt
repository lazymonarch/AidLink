package com.aidlink.data

import android.app.Activity
import android.util.Log
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.model.UserProfile
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository {

    private val tag = "AuthRepository"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCurrentUser() = auth.currentUser

    suspend fun sendOtp(
        phone: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed(e)
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onCodeSent(verificationId)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun getCredential(verificationId: String, otp: String): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, otp)
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential) = suspendCoroutine { continuation ->
        Log.d(tag, "Attempting to sign in with credential...")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "Sign-in task was SUCCESSFUL.")
                    continuation.resume(task.result?.user)
                } else {
                    Log.e(tag, "Sign-in task FAILED", task.exception)
                    continuation.resume(null)
                }
            }
    }

    suspend fun checkIfProfileExists(uid: String): Boolean {
        return try {
            db.collection("users").document(uid).get().await().exists()
        } catch (e: Exception) {
            Log.e(tag, "Error checking if profile exists", e)
            false
        }
    }

    suspend fun createUserProfile(uid: String, profile: Map<String, Any>): Boolean {
        return try {
            db.collection("users").document(uid).set(profile).await()
            true // Correctly return true on success
        } catch (e: Exception) {
            Log.e(tag, "Error creating user profile", e)
            false
        }
    }

    suspend fun saveRequest(requestData: Map<String, Any>): Boolean {
        return try {
            db.collection("requests").add(requestData).await()
            Log.d(tag, "Request saved successfully.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving request", e)
            false
        }
    }

    suspend fun addResponderToRequest(requestId: String, responderId: String, responderName: String): Boolean {
        return try {
            db.collection("requests").document(requestId)
                .update(mapOf(
                    "status" to "pending",
                    "responderId" to responderId,
                    "responderName" to responderName
                )).await()
            Log.d(tag, "Successfully added responder to request $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error adding responder to request", e)
            false
        }
    }

    suspend fun getUserProfileOnce(userId: String): UserProfile? {
        return try {
            db.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e(tag, "Error getting user profile once", e)
            null
        }
    }

    suspend fun acceptOffer(requestId: String, requesterId: String, helperId: String): Boolean {
        return try {
            val requestDocRef = db.collection("requests").document(requestId)
            val chatDocRef = db.collection("chats").document(requestId)
            val chatData = mapOf(
                "participants" to listOf(requesterId, helperId),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            db.runBatch { batch ->
                batch.update(requestDocRef, "status", "in_progress")
                batch.set(chatDocRef, chatData)
            }.await()
            Log.d(tag, "Offer accepted and chat created for request: $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error accepting offer", e)
            false
        }
    }

    suspend fun declineOffer(requestId: String): Boolean {
        return try {
            db.collection("requests").document(requestId)
                .update(mapOf(
                    "status" to "open",
                    "responderId" to FieldValue.delete(),
                    "responderName" to FieldValue.delete()
                )).await()
            Log.d(tag, "Successfully declined offer for request $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error declining offer", e)
            false
        }
    }

    suspend fun deleteRequest(requestId: String): Boolean {
        return try {
            db.collection("requests").document(requestId).delete().await()
            Log.d(tag, "Successfully deleted request: $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting request", e)
            false
        }
    }

    suspend fun cancelRequest(requestId: String): Boolean {
        return try {
            // This is now identical to declineOffer
            db.collection("requests").document(requestId)
                .update(
                    mapOf(
                        "status" to "open",
                        "responderId" to FieldValue.delete(),
                        "responderName" to FieldValue.delete()
                    )
                ).await()
            Log.d(tag, "Successfully cancelled request (reverted to open): $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error cancelling request", e)
            false
        }
    }

    suspend fun editRequest(requestId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            db.collection("requests").document(requestId).update(updatedData).await()
            Log.d(tag, "Successfully edited request: $requestId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error editing request", e)
            false
        }
    }

    private fun mapDocumentToHelpRequest(doc: com.google.firebase.firestore.DocumentSnapshot): HelpRequest {
        return HelpRequest(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            category = doc.getString("category") ?: "",
            location = "Near...",
            type = if (doc.getString("compensation") == "Volunteer") RequestType.VOLUNTEER else RequestType.FEE,
            status = doc.getString("status") ?: "open",
            createdAt = doc.getTimestamp("createdAt"),
            responderId = doc.getString("responderId"),
            responderName = doc.getString("responderName")
        )
    }

    fun getRequests(): Flow<List<HelpRequest>> {
        return db.collection("requests")
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { doc -> mapDocumentToHelpRequest(doc) } }
    }

    fun getMyRequests(userId: String): Flow<List<HelpRequest>> {
        return db.collection("requests")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { doc -> mapDocumentToHelpRequest(doc) } }
    }

    fun getMyResponses(userId: String): Flow<List<HelpRequest>> {
        return db.collection("requests")
            .whereEqualTo("responderId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { doc -> mapDocumentToHelpRequest(doc) } }
    }

    fun getUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
    }

    fun logout() {
        auth.signOut()
    }
}