package com.resume.resumetweaker.ui.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.resume.resumetweaker.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultFragment : Fragment() {
    private lateinit var adapter: FileAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = FileAdapter(requireContext(), getFilesList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun getFilesList(): List<File> {
        val filesDir = File(requireContext().getExternalFilesDir(null), "ResumeTweaker")
        if (!filesDir.exists()) {
            filesDir.mkdirs()
            return emptyList()
        }
        return filesDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    inner class FileAdapter(private val context: Context, private val files: List<File>) :
        RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

        inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fileName: TextView = itemView.findViewById(R.id.fileName)
            val fileDate: TextView = itemView.findViewById(R.id.fileDate)
            val fileSize: TextView = itemView.findViewById(R.id.fileSize)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_result_item, parent, false)
            return FileViewHolder(view)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val file = files[position]
            holder.fileName.text = file.name
            holder.fileDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(file.lastModified()))
            holder.fileSize.text = "${file.length() / 1024} KB"

            holder.itemView.setOnClickListener {
                openPdfForPrinting(file)
            }
        }

        override fun getItemCount(): Int = files.size

        private fun openPdfForPrinting(file: File) {
            // Debug logging
            Log.d("PDF_DEBUG", "Attempting to open: ${file.absolutePath}")
            Log.d("PDF_DEBUG", "File exists: ${file.exists()}, readable: ${file.canRead()}")

            if (!file.exists() || !file.canRead()) {
                Toast.makeText(context, "File not found or inaccessible", Toast.LENGTH_LONG).show()
                return
            }

            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",  // Must match manifest
                    file
                ).also {
                    Log.d("PDF_DEBUG", "Generated URI: $it")
                }

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                // Create chooser
                val chooserIntent = Intent.createChooser(viewIntent, "Open PDF with...")

                // Verify there's an app to handle this
                if (viewIntent.resolveActivity(context.packageManager) != null) {
                    startActivity(chooserIntent)
                } else {
                    Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_LONG).show()
                    // Optionally open Play Store to suggest a viewer
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=com.adobe.reader")
                        })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Please install a PDF viewer", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: IllegalArgumentException) {
                Log.e("PDF_ERROR", "Invalid file path", e)
                Toast.makeText(context, "Invalid file path: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("PDF_ERROR", "Error opening PDF", e)
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}