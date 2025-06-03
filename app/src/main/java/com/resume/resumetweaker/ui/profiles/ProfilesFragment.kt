package com.resume.resumetweaker.ui.profiles

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.resume.resumetweaker.R
import androidx.core.content.edit

class ProfilesFragment : Fragment() {

    private lateinit var profilesContainer: LinearLayout

     override fun onCreateView(
         inflater: LayoutInflater, container: ViewGroup?,
         savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profiles, container, false)
        profilesContainer = view.findViewById(R.id.profilesContainer)

        loadProfiles()
        return view
    }

    private fun loadProfiles() {
        val prefs = requireContext().getSharedPreferences("ResumeData", Context.MODE_PRIVATE)
        val allProfiles = prefs.all

        profilesContainer.removeAllViews()

        for ((key, value) in allProfiles) {
            val profileView = layoutInflater.inflate(R.layout.fragment_profiles_items, profilesContainer, false)
            val profileName = profileView.findViewById<TextView>(R.id.profileName)
            val btnDelete = profileView.findViewById<Button>(R.id.btnDelete)

            profileName.text = key

            btnDelete.setOnClickListener {
                prefs.edit { remove(key) }
                loadProfiles()
            }

            profilesContainer.addView(profileView)
        }
    }
}
