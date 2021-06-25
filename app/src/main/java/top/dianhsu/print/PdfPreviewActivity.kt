package top.dianhsu.print

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.pdfview.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class PdfPreviewActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var tbPdf: Toolbar
    private lateinit var file: File
    private var fileName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)
        initView()
        initPdf()
    }

    private fun initPdf() {
        if (!intent.getBooleanExtra("isPreview", false)) {
            getContent.launch("application/pdf")
        } else {
            Log.e("PreviewFileName", intent.getStringExtra("fileName").toString())
            val fileName = intent.getStringExtra("fileName")
            fileName?.let {
                showPdf(File(application.cacheDir,fileName))
            }
        }
    }

    private fun showPdf(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                file = generatePdfFromUri(uri)
            }
            withContext(Dispatchers.Main) {
                pdfView.fromFile(file).show()
            }
        }
    }

    private fun showPdf(file:File){
        pdfView.fromFile(file).show()
    }

    private fun initView() {
        pdfView = findViewById(R.id.pdf_view)
        tbPdf = findViewById(R.id.tb_pdf)
        tbPdf.setNavigationOnClickListener {
            this.finish()
        }
    }

    private fun generatePdfFromUri(uri: Uri): File {
        val outputDir = applicationContext.cacheDir
        val outputFile: File =
            File.createTempFile("${UUID.randomUUID()}", ".pdf", outputDir)
        val ins = contentResolver.openInputStream(uri)
        val buffer = ByteArray(1024)
        ins.use { input ->
            outputFile.outputStream().use { fileOut ->
                while (true) {
                    val length = input?.read(buffer)
                    if (length != null) {
                        if (length <= 0) {
                            break
                        }
                        fileOut.write(buffer, 0, length)
                    }
                }
                fileOut.flush()
            }
        }

        fileName=outputFile.name

        setResult(111, Intent().apply {
            putExtra("fileName",fileName)
            data=uri
        })

        return outputFile
    }


    private var getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                showPdf(uri)
            }
        }
}