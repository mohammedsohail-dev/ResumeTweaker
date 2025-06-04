package com.resume.resumetweaker.ui.jobdescription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.resume.resumetweaker.R
import com.itextpdf.html2pdf.HtmlConverter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit



class JobDescriptionFragment : Fragment() {

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val PROVIDER_AUTHORITY = "com.resume.resumetweaker.fileprovider"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_jobdescription, container,false)
        populateDropdown(rootView)
        setButton(rootView)
        return rootView
    }

    private fun populateDropdown (rootView: View) {
        val sharedprefs = requireContext().getSharedPreferences("ResumeData", Context.MODE_PRIVATE)
        val allprofiles = sharedprefs.all.keys.toList()
        val spinner = rootView.findViewById<Spinner>(R.id.profileSpinner)
        if (allprofiles.isEmpty()) {
            // Disable spinner and show empty state
            spinner.isEnabled = false
            spinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                listOf("No profiles available") // Show a message
            )
        } else {
            // Enable spinner and populate with profiles
            spinner.isEnabled = true
            spinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                allprofiles
            )
        }
    }

    private fun getTextCompanyAndRoleName(rootView:View) : String {
        return  rootView.findViewById<EditText>(R.id.editTextCompanyAndRoleName).text.toString()
    }

    private fun getJobDescription(rootView:View) : String {
        return  rootView.findViewById<EditText>(R.id.editTextJobDescription).text.toString()
    }

    private fun getSelectedProfile(rootView: View): String {
        val spinner = rootView.findViewById<Spinner>(R.id.profileSpinner)
        return spinner.selectedItem.toString()
    }

    private fun setButton(rootView: View) {
        rootView.findViewById<Button>(R.id.btnGenerate).setOnClickListener{
            val companyAndRole = getTextCompanyAndRoleName(rootView)
            val jobDescription = getJobDescription(rootView)
            val selectedProfile = getSelectedProfile(rootView)

            if(validateInputs(companyAndRole, jobDescription, selectedProfile)){
                generateResumeAndCoverLetter(companyAndRole, jobDescription, selectedProfile,rootView)
            } else {
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs (companyAndRole: String, jobDescription: String, selectedProfile:String) : Boolean {
        if(companyAndRole.isBlank()){
            Toast.makeText(requireContext(), "Please enter Company and Role Details", Toast.LENGTH_SHORT).show()
            return false;
        } else if(jobDescription.isBlank()){
            Toast.makeText(requireContext(), "Please enter Job Description Or Instruction", Toast.LENGTH_SHORT).show()
            return false;
        } else if(selectedProfile == "No profiles available") {
            Toast.makeText(requireContext(), "Please create a profile first", Toast.LENGTH_SHORT).show()
            return false;
        } else {
            return true;
        }
    }

    private fun generateResumeAndCoverLetter(
        companyAndRole: String,
        jobDescription: String,
        selectedProfile: String,
        rootView: View
    ) {
        val profile: String = getProfile(selectedProfile).toString()

        updateProgress(10, "Starting..", rootView)
        val prompt = preparePrompt(companyAndRole, jobDescription, profile)

        // Run in background thread
        Executors.newSingleThreadExecutor().execute {
            try {
                updateProgress(20, "Contacting OpenAI...", rootView)
                val response = sendPromptToOpenAI(prompt)

                activity?.runOnUiThread {
                    if (response == null) {
                        Toast.makeText(requireContext(), "Failed to connect to OpenAI", Toast.LENGTH_SHORT).show()
                        updateProgress(0, "Failed", rootView)
                        return@runOnUiThread
                    }

                    try {
                        Log.d("OpenAI_RawResponse", response) // Debug: Log the raw response

                        val jsonResponse = JSONObject(response)

                        // Check for API errors first
                        if (jsonResponse.has("error")) {
                            val error = jsonResponse.getJSONObject("error")
                            val errorMsg = error.getString("message")
                            Toast.makeText(requireContext(), "OpenAI Error: $errorMsg", Toast.LENGTH_LONG).show()
                            return@runOnUiThread
                        }

                        // Check if "choices" exists
                        if (!jsonResponse.has("choices")) {
                            Toast.makeText(requireContext(), "Invalid OpenAI response", Toast.LENGTH_SHORT).show()
                            Log.e("OpenAIError", "Response: $response")
                            return@runOnUiThread
                        }

                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() == 0) {
                            Toast.makeText(requireContext(), "No response from OpenAI", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        val content = choices
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        // Split into resume & cover letter
                        updateProgress(60, "Splitting Resume and Cover Letter", rootView)
                        val parts = content.split("=== RESUME ===", "=== COVER LETTER ===")
                        if (parts.size >= 3) {
                            val resumeContent = parts[1].trim()
                            val coverLetterContent = parts[2].trim()

                            Log.d("ResumeContent", resumeContent)
                            Log.d("CoverLetterContent", coverLetterContent)
                            updateProgress(70, "Saving Resume As Pdf", rootView)
                            saveResumeAsPdf(companyAndRole,resumeContent)
                            updateProgress(90, "Saving Cover Letter As Pdf", rootView)
                            saveCoverLetterAsPdf(companyAndRole,coverLetterContent)

                            updateProgress(100, "Done!", rootView)

                        } else {
                            Toast.makeText(requireContext(), "Unexpected response format", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show()
                        Log.e("OpenAIError", "Parsing failed", e)
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("OpenAIError", "API call failed", e)
                }
            }
        }
    }

    private fun getProfile(selectedProfile: String): String? {
        val sharedprefs = requireContext().getSharedPreferences("ResumeData", Context.MODE_PRIVATE)
        return sharedprefs.getString(selectedProfile, null)
    }


    private fun saveResumeAsPdf(companyAndRole: String, htmlContent: String) {
        try {
            val fileName = "Resume_${companyAndRole.replace(" ", "_")}_${System.currentTimeMillis()}"
            val pdfFile = saveHtmlAsPdf(htmlContent, fileName)

            activity?.runOnUiThread {
                if (pdfFile != null) {
                    Toast.makeText(
                        requireContext(),
                        "Resume saved to ${pdfFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    sharePdfFile(pdfFile)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save resume",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Failed to save resume", e)
        }
    }

    private fun saveCoverLetterAsPdf(companyAndRole: String, htmlContent: String) {
        try {
            val fileName = "CoverLetter_${companyAndRole.replace(" ", "_")}_${System.currentTimeMillis()}"
            val pdfFile = saveHtmlAsPdf(htmlContent, fileName)

            activity?.runOnUiThread {
                if (pdfFile != null) {
                    Toast.makeText(
                        requireContext(),
                        "Cover letter saved to ${pdfFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    sharePdfFile(pdfFile)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save cover letter",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Failed to save cover letter", e)
        }
    }

    private fun saveHtmlAsPdf(htmlContent: String, fileName: String): File? {
        return try {
            // Create downloads directory if it doesn't exist
            val downloadsDir = File(requireContext().getExternalFilesDir(null), "ResumeTweaker")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Create PDF file
            val pdfFile = File(downloadsDir, "$fileName.pdf")

            // Convert HTML to PDF
            HtmlConverter.convertToPdf(htmlContent, FileOutputStream(pdfFile))

            pdfFile
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Failed to save PDF", e)
            null
        }
    }

    private fun sharePdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                PROVIDER_AUTHORITY,
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
        } catch (e: Exception) {
            Log.e("SHARE_ERROR", "Failed to share PDF", e)
        }
    }

    private fun preparePrompt(companyAndRole: String, jobDescription: String, profile: String): String {
        return """
Create a tailored resume and cover letter for the specified job opportunity. 
The response must be formatted exactly as shown below.

Ensure that the **entire resume and cover letter are written in ONE language**.  
Use the language of the job description (JD).  
If both English and another language appear in the JD, prioritize the **non-English** language.  
**Ignore the resume/profile language if different, and translate all content into the JD language.**

=== RESUME ===
<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: 'Times New Roman', serif;
            margin: 40px;
        }
        h1 {
            font-size: 24px;
            font-weight: bold;
        }
        .contact {
            margin-bottom: 20px;
        }
        .contact p {
            margin: 0;
        }
        .section-title {
            font-weight: bold;
            text-align: center;
            margin: 30px 0 10px;
            font-size: 18px;
            text-decoration: underline;
        }
        ul {
            margin-top: 5px;
        }
        li {
            margin-bottom: 8px;
        }
        .sub-section {
            margin-top: 20px;
            font-size: 16px;
            line-height: 1.0;
        }
        .sub-section p {
            margin-bottom:0px;
        }
        .label {
            font-weight: bold;
        }
        .exp-title {
            font-weight: bold;
        }
        .exp-date {
            float: right;
            font-style: italic;
        }
        .job-entry {
            margin-top: 15px;
        }
        hr {
            border: none;
            border-top: 1px solid #000;
            margin: 30px 0;
        }
    </style>
</head>
<body>

    <h1>[Candidate Full Name]</h1>
    <div class="contact">
        <p>[Phone Number]</p>
        <p><a href="mailto:[Email Address]">[Email Address]</a></p>
        <p><a href="[portfolio-link]">Portfolio</a></p>
        <p><a href="[LinkedIn-link]">LinkedIn</a></p>
    </div>

    <hr />

    <div class="section-title">[Translate "Field of Expertise" to match JD language]</div>
    <ul>
        <li>[Relevant capabilities — match JD tone and content. Translate if necessary.]</li>
        <li>[Add more only if relevant. Don’t invent.]</li>
    </ul>

    <div class="sub-section">
        <p><span class="label">[Translate "Programming Languages:"]</span> [List programming languages]</p>
        <p><span class="label">[Translate "Operating Systems:"]</span> [List OS]</p>
        <p><span class="label">[Translate "Software Tools:"]</span> [List IDEs and tools]</p>
        <p><span class="label">[Translate "Databases:"]</span> [List DBs]</p>
        <p><span class="label">[Translate "Cloud Computing:"]</span> [List cloud services]</p>
        <p><span class="label">[Translate "Project Management:"]</span> [List project management methodologies]</p>
        <p><span class="label">[Translate "Languages:"]</span> [Spoken/written languages - Translate the languages and the fluency according to JD]</p>
    </div>

    <div class="section-title">[Translate "Professional Experience"]</div>

    <div class="job-entry">
        <p class="exp-title">[Job Title — translate to JD language] <span class="exp-date">[Start Date] – [End Date]</span></p>
        <p>[Company Name], [Location]</p>
        <ul>
            <li>[Responsibility 1 — reword to align with JD and language]</li>
            <li>[Responsibility 2]</li>
            <li>[Responsibility 3]</li>
        </ul>
    </div>

    <div class="job-entry">
        <p class="exp-title">[Job Title — translate to JD language] <span class="exp-date">[Start Date] – [End Date]</span></p>
        <p>[Company Name], [Location]</p>
        <ul>
            <li>[Responsibility 1]</li>
            <li>[Responsibility 2]</li>
            <li>[Responsibility 3]</li>
        </ul>
    </div>

    <!-- Add more job entries as needed -->

    <hr>

    <div class="section-title">[Translate "Education"]</div>

    <div class="sub-section">
        <p><strong>[Degree — translate to JD language]</strong> <span class="exp-date">[Year Range]</span><br>
        [University Name — translate if needed]</p>
        <!-- Add additional degrees if applicable -->
    </div>

    <div class="section-title">[Translate "Certifications"]</div>

    <div class="sub-section">
        <ul>
            <li>
                <strong>[Certification Name — translate to JD language]</strong> – [Year]<br>
                [Label "Certificat" — translate to JD language]: [Credential — translate to JD language]<br>
                [Issuer]
            </li>
            <li>
                <strong>[Certification Name — translate to JD language]</strong> – [Year]<br>
                [Label "Certificat" — translate to JD language]: [Credential — translate to JD language]<br>
                [Issuer]
            </li>
            <!-- Add more if needed -->
        </ul>
    </div>

</body>
</html>

=== COVER LETTER ===
<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: 'Times New Roman', serif;
            margin: 40px;
        }
        .header {
            text-align: right;
            margin-bottom: 30px;
        }
        .content {
            line-height: 1.6;
        }
        .signature {
            margin-top: 50px;
            font-style: italic;
        }
    </style>
</head>
<body>

    <div class="header">
        [Candidate Name]<br>
        [Email Address]<br>
        [Phone Number]<br>
    </div>

    <div class="content">

        <p>[Translate greeting, e.g., “Dear Hiring Manager”]</p>

        <p>[Intro: Reference the position, express interest — translated and rewritten if needed]</p>

        <p>[Body: Emphasize 2–3 strengths that match the JD. Use keywords from the JD and <strong>bold</strong> them.]</p>

        <p>[Closing: Thank the recruiter and express eagerness to interview.]</p>

        <div class="signature">
            [Translated formal closing phrase],<br>
            [Candidate Name]
        </div>
    </div>
</body>
</html>

Job Application Details:
- Company and Role: $companyAndRole
- Job Description: $jobDescription

Candidate Profile:
$profile

Instructions:
1. Use the provided HTML templates.
2. Replace all placeholders inside [] with appropriate content from the candidate profile and job description.
3. Always use the **language of the job description**. If the resume or profile is in a different language, IGNORE that and translate all content into the JD language.
4. **Translate job titles, section headings, degrees, university names, certification names, and even labels like "Certificat" into the JD language.**
5. Bold keywords from the job description for ATS optimization.
6. Include relevant skills from the JD even if not in the profile, but justify them with transferable experience.
7. Maintain full consistency in tone, language, and format across resume and cover letter.
8. Do not put extra stuff like backticks or anything, the template is strict
9. If the end date is null it means its present date. but for certifications null means no expiry date.
10. Never ever omit education or experience.
11.take key words from JD and try to use them as much as possible, also bold them this will make the resume pass through ATS check.
12. Try to Use Action word + Task + Result and try to use numbers as much as possible but do not forge numbers.

""".trimIndent()
    }

    private fun sendPromptToOpenAI(prompt: String): String? {
        val OpenAIToken = getOpenAIToken()
        val client = OkHttpClient.Builder()
            .readTimeout(4, TimeUnit.MINUTES)
            .build()

        // Using JSONObject for 100% valid JSON (no manual escaping needed)
        val jsonBody = JSONObject().apply {
            put("model", "gpt-4") // or "gpt-3.5-turbo" if you don't have GPT-4 access
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt) // JSONObject handles escaping automatically
                })
            })
        }

        val requestBody = jsonBody.toString()
        Log.d("OpenAI_Request", requestBody) // Debug: Verify the JSON is correct

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $OpenAIToken")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("OpenAI_Response", responseBody ?: "Empty response") // Debug
            responseBody
        } catch (e: Exception) {
            Log.e("OpenAI_Error", "API call failed", e)
            null
        }
    }
    private fun getOpenAIToken():String {
        val openaiprefs = requireContext().getSharedPreferences("openai_prefs", Context.MODE_PRIVATE)
        val token =  openaiprefs.getString("openai_token", "") ?: ""
        return token
    }

    private fun updateProgress(percent: Int, message: String, rootView: View) {
        activity?.runOnUiThread {
            val progressDialogContainer = rootView.findViewById<View>(R.id.progressDialogContainer)
            val progressBar = rootView.findViewById<ProgressBar>(R.id.progressBar)
            val progressText = rootView.findViewById<TextView>(R.id.progressText)

            if (percent in 1..99) {
                progressDialogContainer.visibility = View.VISIBLE
                progressBar.progress = percent
                progressText.text = "$percent% - $message"
            } else {
                progressDialogContainer.visibility = View.GONE
            }

            if (percent >= 100) {
                Handler(Looper.getMainLooper()).postDelayed({
                    progressDialogContainer.visibility = View.GONE
                }, 1000)
            }
        }
    }
}

