package com.example.apigrafik.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.apigrafik.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var listener: (String) -> Unit

    fun setOnCategorySelectedListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_list, container, false)

        view.findViewById<LinearLayout>(R.id.topGainers).setOnClickListener {
            listener.invoke("Top Gainers")
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.topLosers).setOnClickListener {
            listener.invoke("Top Losers")
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.Favorites).setOnClickListener {
            listener.invoke("Favorites")
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.allStocks).setOnClickListener {
            listener.invoke("All Stocks")
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.USstock).setOnClickListener {
            listener.invoke("Saham US")
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.IDNstock).setOnClickListener {
            listener.invoke("Saham Indo")
            dismiss()
        }
        return view
    }
}
