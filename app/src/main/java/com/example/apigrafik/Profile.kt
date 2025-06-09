package com.example.apigrafik

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apigrafik.databinding.ProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class Profile : AppCompatActivity() {
    private lateinit var binding: ProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileBinding.inflate(layoutInflater)  // Inisialisasi binding
        setContentView(binding.root)  // Gunakan binding.root setelah inisialisasi

        binding.backHome.setOnClickListener{
            finish()
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Logout dari Firebase

            val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
            googleSignInClient.signOut().addOnCompleteListener {

                Toast.makeText(this, "Anda berhasil logout", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
        }
    }
}