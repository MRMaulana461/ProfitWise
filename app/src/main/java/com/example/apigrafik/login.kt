package com.example.apigrafik

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.apigrafik.databinding.LoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class Login: AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        // Cek apakah sudah login
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish() // Finish login activity agar tidak bisa kembali
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignIn = findViewById<LinearLayout>(R.id.btn_google)
        googleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.confirmLoginButton.setOnClickListener {
            val email = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, Home::class.java)
                        startActivity(intent)
                        finish() // Finish login activity setelah berhasil login
                    } else {
                        Toast.makeText(this, "Email / Password Salah", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Ups, Tidak boleh ada yang kosong", Toast.LENGTH_SHORT).show()
            }
        }

        binding.regisLink.setOnClickListener {
            val loginIntent = Intent(this, Register::class.java)
            startActivity(loginIntent)
            finish()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResult(task)
        }
    }

    private fun handleResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            Toast.makeText(this, "Ups, Pendaftaran Gagal, Coba Lagi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(this, Home::class.java)
                startActivity(intent)
                finish() // Finish login activity setelah berhasil login
            } else {
                Toast.makeText(this, "Email / Password Salah", Toast.LENGTH_SHORT).show()
            }
        }
    }
}