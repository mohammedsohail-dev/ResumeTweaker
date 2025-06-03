package com.resume.resumetweaker.ui.openai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.resume.resumetweaker.R

class OpenAIFragment : Fragment() {

    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //set the view
        rootView = inflater.inflate(R.layout.fragment_openai, container, false)

        //fetch the variables
        val editText = rootView!!.findViewById<EditText>(R.id.edittext_token)
        val button = rootView!!.findViewById<Button>(R.id.button_submit_token)

        //stored shared preferences in the app
        val sharedpref = requireContext().getSharedPreferences("openai_prefs", Context.MODE_PRIVATE)

        val savedToken = sharedpref.getString("openai_token","")

        //set the token for edit its already been set
        editText.setText(savedToken)

        //set the token
        button.setOnClickListener {
            val token = editText.text.toString().trim()

            if(token.isNotEmpty()){
                sharedpref.edit { putString("openai_token", token) }
                Toast.makeText(requireContext(), "Token set successfully", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(requireContext(), "Please enter a valid token", Toast.LENGTH_SHORT).show()
            }
        }


        return rootView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }
}