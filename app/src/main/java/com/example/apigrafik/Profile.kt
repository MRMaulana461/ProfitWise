package com.example.apigrafik

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apigrafik.databinding.ProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest

class Profile : AppCompatActivity() {
    private lateinit var binding: ProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val user = firebaseAuth.currentUser
        if (user != null) {
            binding.email.setText(user.email)
            binding.name.setText(user.displayName)
        } else {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Tampilkan ikon ceklis ketika mulai mengedit name
        binding.name.setOnFocusChangeListener { _, hasFocus ->
            binding.checkIcon.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

        // Handle klik pada ikon ceklis
        binding.checkIcon.setOnClickListener {
            val updatedName = binding.name.text.toString()

            // Perbarui data pengguna di Firebase
            val profileUpdates = userProfileChangeRequest {
                displayName = updatedName
            }

            user?.updateProfile(profileUpdates)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Nama berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal memperbarui nama", Toast.LENGTH_SHORT).show()
                    }
                }

            // Sembunyikan ikon ceklis
            binding.checkIcon.visibility = View.GONE
            binding.name.clearFocus()
        }

        binding.backHome.setOnClickListener {
            finish()
        }


        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

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