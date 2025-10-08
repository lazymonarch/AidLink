package com.aidlink.data

import android.app.Activity
import android.util.Log
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.aidlink.model.UserProfile

class AuthRepository {

    private val TAG = "AuthRepository"
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

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
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
        Log.d(TAG, "Attempting to sign in with credential...")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Sign-in task was SUCCESSFUL.")
                    continuation.resume(task.result?.user)
                } else {
                    Log.e(TAG, "Sign-in task FAILED", task.exception)
                    continuation.resume(null)
                }
            }
    }

    suspend fun checkIfProfileExists(uid: String): Boolean {
        return try {
            db.collection("users").document(uid).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createUserProfile(uid: String, profile: Map<String, Any>): Boolean {
        return try {
            db.collection("users").document(uid).set(profile).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveRequest(requestData: Map<String, Any>): Boolean {
        return try {
            db.collection("requests").add(requestData).await()
            Log.d(TAG, "Request saved successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving request", e)
            false
        }
    }

    private fun mapDocumentToHelpRequest(doc: com.google.firebase.firestore.DocumentSnapshot): HelpRequest {
        return HelpRequest(
            id = doc.id,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            category = doc.getString("category") ?: "",
            location = "Near...",
            type = if (doc.getString("compensation") == "Volunteer") RequestType.VOLUNTEER else RequestType.FEE,
            status = doc.getString("status") ?: "open"
        )
    }

    fun getRequests(currentUserId: String): Flow<List<HelpRequest>> {
        return db.collection("requests")
            // 1. First, order by the field you are filtering on
            .orderBy("userId")
            // 2. Then, apply the 'not-equal' filter
            .whereNotEqualTo("userId", currentUserId)
            // 3. Now, you can add your primary sorting
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    mapDocumentToHelpRequest(doc)
                }
            }
    }

    fun getMyRequests(userId: String): Flow<List<HelpRequest>> {
        return db.collection("requests")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> mapDocumentToHelpRequest(doc) } // <-- Consistency Fix
            }
    }

    fun getMyResponses(userId: String): Flow<List<HelpRequest>> {
        return db.collection("requests")
            .whereEqualTo("responderId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> mapDocumentToHelpRequest(doc) } // <-- Consistency Fix
            }
    }

    fun getUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java)
            }
    }

    fun logout() {
        auth.signOut()
    }
}