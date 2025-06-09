package com.example.apigrafik

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.apigrafik.Fragment.ChartFragment
import com.example.apigrafik.Fragment.HomeFragment
import com.example.apigrafik.Fragment.SearchFragment
import com.example.apigrafik.databinding.ActivityHomeBinding

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment saat aplikasi diluncurkan
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), false)  // Tidak menambahkan HomeFragment ke back stack
        }

        binding.iconProfil.setOnClickListener {
            val profileintent = Intent(this, Profile::class.java)
            startActivity(profileintent)
        }

        // Navigasi BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment(), false)  // Tidak menambahkan HomeFragment ke back stack
                    true
                }
                R.id.search -> {
                    replaceFragment(SearchFragment(), true)  // Menambahkan SearchFragment ke back stack
                    true
                }
                R.id.chart -> {
                    replaceFragment(ChartFragment(), true)  // Menambahkan ChartFragment ke back stack
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi untuk mengganti fragment
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        // Membuat transaksi fragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Mengganti fragment di kontainer dengan fragment baru
        fragmentTransaction.replace(R.id.fragment_container, fragment)

        // Menambahkan transaksi ke back stack jika diperlukan
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }

        // Menyelesaikan transaksi fragment
        fragmentTransaction.commit()
    }
}

