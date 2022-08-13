package com.schoolofthought.googleonetapsignin

import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    private lateinit var startIntentSenderForResult: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var oneTapClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getLoginIntentResult()

        oneTapClient = Identity.getSignInClient(this)
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(CLIENT_ID)
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        val tokenText = findViewById<TextView>(R.id.token)
        findViewById<MaterialButton>(R.id.signIn).setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(
                            result.pendingIntent
                        ).build()
                        tokenText.text = "Loggin in...."
                        startIntentSenderForResult.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Google Sign-in failed")
                        val dialog = AlertDialog.Builder(this)
                            .setTitle("Failed to login")
                            .setMessage("Please try again some times.")
                            .setPositiveButton("Okay") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                        dialog.show()
                    }
                }
                .addOnFailureListener { e -> Log.e(TAG, "Google Sign-in failed", e) }
        }
    }

    private fun getLoginIntentResult() {
        val tokenText = findViewById<TextView>(R.id.token)
        startIntentSenderForResult = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { data ->
            val googleCredential = oneTapClient.getSignInCredentialFromIntent(data.data)
            val idToken = googleCredential.googleIdToken
            tokenText.text = "Token: $idToken"
            when {
                idToken != null -> {
                    // Got an ID token from Google. Use it to authenticate
                    // with Firebase.
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    Log.d(TAG, "getLoginIntentResult: ${firebaseCredential.signInMethod}")
                }
                else -> {
                    // Shouldn't happen.
                    Log.d(TAG, "No ID token!")
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val CLIENT_ID = "220017953809-6ltlq3gccm990f1hglslk4ek9neodkf6.apps.googleusercontent.com"
        private const val CLIENT_SECRET = "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"
    }
}