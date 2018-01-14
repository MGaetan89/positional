package io.trewartha.positional.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import io.trewartha.positional.R
import kotlinx.android.synthetic.main.sign_in_activity.*

class SignInActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_SIGN_IN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_activity)

        googleSignInButton.setOnClickListener { onGoogleSignInClick() }
        whyTextView.setOnClickListener { onWhyClick() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> onSignInResult(resultCode, data)
        }
    }

    private fun onSignInResult(resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            val response = IdpResponse.fromResultIntent(data)
            val errorText = when (response?.errorCode) {
                ErrorCodes.NO_NETWORK -> R.string.sign_in_failed_no_network
                ErrorCodes.UNKNOWN_ERROR -> R.string.sign_in_cancelled
                null -> R.string.sign_in_cancelled
                else -> R.string.sign_in_failed_unknown
            }
            Snackbar.make(signInLayout, errorText, Snackbar.LENGTH_LONG).show()
            googleSignInButton.isEnabled = true
        }
    }

    private fun onGoogleSignInClick() {
        googleSignInButton.isEnabled = false
        val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                ))
                .build()
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun onWhyClick() {
        AlertDialog.Builder(this)
                .setTitle(R.string.sign_in_why_title)
                .setMessage(R.string.sign_in_why_message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }
}