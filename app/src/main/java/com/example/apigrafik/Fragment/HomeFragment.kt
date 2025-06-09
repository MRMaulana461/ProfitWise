package com.example.apigrafik.Fragment

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.apigrafik.R
import com.example.apigrafik.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // TabLayout dan ViewPager
        val tabLayout: TabLayout = binding.tabLayout
        val viewPager: ViewPager = binding.viewPagerHome
        val viewPagerAdapter = ViewPagerAdapter(childFragmentManager)

        // Tambahkan Fragment ke ViewPager
        viewPagerAdapter.addFragment(ListSahamFragment(), "List")
        viewPagerAdapter.addFragment(BeritaFragment(), "Berita")
        viewPager.adapter = viewPagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        // Sesuaikan lebar indikator setelah TabLayout diatur
        tabLayout.post {
            adjustTabIndicatorWidth(tabLayout)
        }
    }

    private fun adjustTabIndicatorWidth(tabLayout: TabLayout) {
        try {
            // Dapatkan field slidingTabIndicator menggunakan reflection
            val field = TabLayout::class.java.getDeclaredField("slidingTabIndicator")
            field.isAccessible = true
            val slidingTabIndicator = field.get(tabLayout) as ViewGroup

            // Dapatkan lebar layar
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels

            // Hitung lebar masing-masing tab
            val tabWidth = screenWidth / tabLayout.tabCount

            // Iterasi setiap tab
            for (i in 0 until slidingTabIndicator.childCount) {
                val tab = slidingTabIndicator.getChildAt(i)

                // Gunakan reflection untuk mendapatkan dan memodifikasi LayoutParams
                val tabViewField = tab.javaClass.getDeclaredField("view")
                tabViewField.isAccessible = true
                val tabView = tabViewField.get(tab) as android.view.View

                // Atur lebar indikator
                val layoutParams = tabView.layoutParams
                layoutParams.width = tabWidth
                tabView.layoutParams = layoutParams
            }

            // Minta layout ulang
            tabLayout.requestLayout()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ViewPager Adapter untuk mengelola fragment
    internal class ViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(
        fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        private val fragments: ArrayList<Fragment> = ArrayList()
        private val juduls: ArrayList<String> = ArrayList()

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        fun addFragment(fragment: Fragment, judul: String) {
            fragments.add(fragment)
            juduls.add(judul)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return juduls[position]
        }
    }
}
