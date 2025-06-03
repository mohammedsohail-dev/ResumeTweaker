package com.resume.resumetweaker.ui.resumeform

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import com.resume.resumetweaker.R
import com.resume.resumetweaker.databinding.FragmentResumeBuilderBinding
import org.json.JSONArray
import org.json.JSONObject

class ResumeFragment : Fragment() {

    private var _binding: FragmentResumeBuilderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResumeBuilderBinding.inflate(inflater, container, false)
        val root = binding.root

        setupDynamicFieldButtons()

        binding.btnSubmit.setOnClickListener {
                val json = collectResumeData()

                val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                builder.setTitle("Save Profile As")

                val input = EditText(requireContext())
                input.hint = "Enter profile name"
                builder.setView(input)

                builder.setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        saveResumeToSharedPreferences(name, json)
                    } else {
                        Toast.makeText(requireContext(), "No name provided, not saving", Toast.LENGTH_SHORT ).show()
                    }
                }

                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                builder.show()
            }


        return root
    }

    private fun setupDynamicFieldButtons() {
        setupAddRemoveField(binding.btnAddLanguage, binding.btnDeleteLanguage, R.layout.fragment_resume_builder_language_item, binding.languageContainer)
        setupAddRemoveField(binding.btnAddCertification, binding.btnDeleteCertification, R.layout.fragment_resume_builder_certification_item, binding.certificationContainer)
        setupAddRemoveField(binding.btnAddEducation, binding.btnDeleteEducation, R.layout.fragment_resume_builder_education_item, binding.educationContainer)
        setupAddRemoveField(binding.btnAddExperience, binding.btnDeleteExperience, R.layout.fragment_resume_builder_experience_item, binding.experienceContainer)
        setupAddRemoveField(binding.btnAddSkill, binding.btnDeleteSkill, R.layout.fragment_resume_builder_skill_item, binding.skillsContainer)
        setupAddRemoveField(binding.btnAddVolunteer, binding.btnDeleteVolunteer, R.layout.fragment_resume_builder_volunteer_item, binding.volunteerContainer)
    }

    private fun setupAddRemoveField(addButton: View, deleteButton: View, layoutId: Int, container: LinearLayout) {
        addButton.setOnClickListener {
            addField(layoutId, container)
        }
        deleteButton.setOnClickListener {
            removeField(container)
        }
    }

    private fun addField(layoutId: Int, container: LinearLayout) {
        val fieldView = layoutInflater.inflate(layoutId, container, false)
        container.addView(fieldView)
    }

    private fun removeField(container: LinearLayout) {
        val childCount = container.childCount
        if (childCount > 0) {
            container.removeViewAt(childCount - 1)
        }
    }

    private fun collectResumeData(): JSONObject {
        val resume = JSONObject()
        resume.put("name", binding.editTextName.text.toString())
        resume.put("phone", binding.editTextPhone.text.toString())
        resume.put("email", binding.editTextEmail.text.toString())
        resume.put("linkedin", binding.editTextLinkedIn.text.toString())
        resume.put("portfolio", binding.editTextPortfolio.text.toString())
        resume.put("summary", binding.editTextSummary.text.toString())

        resume.put("languages", collectFieldData(binding.languageContainer))
        resume.put("certifications", collectFieldData(binding.certificationContainer))
        resume.put("skills", collectFieldData(binding.skillsContainer))
        resume.put("education", collectFieldData(binding.educationContainer))
        resume.put("experience", collectFieldData(binding.experienceContainer))
        resume.put("volunteer", collectFieldData(binding.volunteerContainer))

        return resume
    }

    private fun collectFieldData(container: LinearLayout): JSONArray {
        val array = JSONArray()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val entry = JSONObject()
            val editTexts = getAllEditTexts(child)
            for (editText in editTexts) {
                val key = try {
                    resources.getResourceEntryName(editText.id)
                } catch (e: Exception) {
                    "field${editText.id}"
                }
                entry.put(key, editText.text.toString())
            }
            if (entry.length() > 0) {
                array.put(entry)
            }
        }
        return array
    }

    private fun getAllEditTexts(view: View): List<EditText> {
        val result = mutableListOf<EditText>()
        if (view is EditText) {
            result.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                result.addAll(getAllEditTexts(view.getChildAt(i)))
            }
        }
        return result
    }

    private fun saveResumeToSharedPreferences(profileName: String, json: JSONObject) {
        try {
            val prefs = requireContext().getSharedPreferences("ResumeData", Context.MODE_PRIVATE)
            prefs.edit {
                putString(profileName, json.toString())
            }
        } catch (e: Exception) {
            Log.e("SharedPrefsError", "Error saving resume to SharedPreferences: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

