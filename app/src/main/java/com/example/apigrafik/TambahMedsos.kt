package com.example.apigrafik

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apigrafik.databinding.ActivityTambahMedsosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TambahMedsos : AppCompatActivity() {
    private lateinit var binding: ActivityTambahMedsosBinding
    private val database = Firebase.database.reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi binding
        binding = ActivityTambahMedsosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Setup save button
        binding.saveButton.setOnClickListener {
            saveInstagramData()
        }
    }

    private fun saveInstagramData() {
        val instagramUsername = binding.editBio.text.toString().trim()

        // Validasi input
        if (instagramUsername.isEmpty()) {
            binding.editBio.error = "Mohon masukkan username Instagram"
            return
        }

        // Get user ID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Create data object
        val socialMediaData = SocialMediaData(
            username = instagramUsername,
            platform = "Instagram",
            timestamp = System.currentTimeMillis()
        )

        // Save to Firebase
        database.child("social_media")
            .child(userId)
            .child("instagram")
            .setValue(socialMediaData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data Instagram berhasil disimpan", Toast.LENGTH_SHORT).show()
                // Redirect to Profile Activity
                val intent = Intent(this, Profile::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

// Data class untuk data social media
data class SocialMediaData(
    val username: String = "",
    val platform: String = "",
    val timestamp: Long = 0
)
