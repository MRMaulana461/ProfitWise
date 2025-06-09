package com.example.apigrafik

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.tabs.TabLayout

class FixedWidthTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = tabCount
        if (childCount > 0) {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val tabWidth = screenWidth / childCount

            // Set the minimum width for each tab
            for (i in 0 until childCount) {
                val tabView = getTabAt(i)?.view
                tabView?.minimumWidth = tabWidth
            }

            // Adjust the tab indicator width to match the width of the selected tab
            val selectedTab = getTabAt(selectedTabPosition)
            val indicator = getChildAt(0).findViewById<View>(resources.getIdentifier("tab_indicator", "id", context.packageName))

            if (indicator != null && selectedTab != null) {
                indicator.layoutParams.width = tabWidth
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
