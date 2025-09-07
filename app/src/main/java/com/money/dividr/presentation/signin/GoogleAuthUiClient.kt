package com.money.dividr.presentation.signin

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
 
import com.money.dividr.R
import com.money.dividr.model.SignInResult
import com.money.dividr.domain.model.UserData
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient,
    private val firestore: FirebaseFirestore  
) {
    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val firebaseUser = auth.signInWithCredential(googleCredentials).await().user

            if (firebaseUser != null) {
                try {
                    val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                    val existingDoc = userDocRef.get().await()

                    val userDataUpdates = mutableMapOf<String, Any?>(
                        "userId" to firebaseUser.uid,  
                        "username" to firebaseUser.displayName,
                        "profilePictureUrl" to firebaseUser.photoUrl?.toString(),
                        "email" to firebaseUser.email
                         
                    )

                    if (existingDoc.exists()) {
                         
                        userDocRef.update(userDataUpdates).await()
                    } else {
                         
                        userDataUpdates["groupIds"] = emptyList<String>()  
                        userDocRef.set(userDataUpdates).await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                     
                     
                }
            }
            
             
             
            val resultUserData = firebaseUser?.run {
                UserData(
                    userId = uid,
                    username = displayName,
                    profilePictureUrl = photoUrl?.toString(),
                    email = email
                     
                     
                )
            }

            SignInResult(
                data = resultUserData,  
                error = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(null, e.message)
        }
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

     
     
     
    fun getSignedInUser(): UserData? = auth.currentUser?.run {
         UserData(
            userId = uid,
            username = displayName,
            profilePictureUrl = photoUrl?.toString(),
            email = email
             
        )
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
