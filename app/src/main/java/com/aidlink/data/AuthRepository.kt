package com.aidlink.data

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots

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
            // Use .add() to let Firestore auto-generate a unique ID for the request
            db.collection("requests").add(requestData).await()
            Log.d(TAG, "Request saved successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving request", e)
            false
        }
    }

    fun getRequests(): Flow<List<HelpRequest>> {
        return db.collection("requests")
            // Order by most recent posts first
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots() // This returns a Flow that updates in real-time
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    // Convert each document into a HelpRequest object
                    HelpRequest(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        location = "Near...", // You'll update this later
                        type = if (doc.getString("compensation") == "Volunteer") RequestType.VOLUNTEER else RequestType.FEE
                    )
                }
            }
    }
}