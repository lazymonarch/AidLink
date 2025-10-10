package com.aidlink.utils

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// This extension function wraps the listener in a Flow
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val listenerRegistration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error) // Close the flow with an exception if an error occurs
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot) // Send the latest data snapshot to the flow
        }
    }
    // When the flow is cancelled (e.g., ViewModel is cleared), remove the listener
    awaitClose { listenerRegistration.remove() }
}