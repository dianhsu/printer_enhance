package top.dianhsu.print

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import com.pdfview.PDFView
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import java.io.File
import java.util.*

class PdfPreviewActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private var pdfUri: Uri? = null
    private lateinit var tb_pdf:Toolbar




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)
        initView()
        initPdf()


    }

    private fun initPdf() {
        getContent.launch("application/pdf")
    }

    private fun initView() {
        pdfView = findViewById(R.id.pdf_view)
        tb_pdf=findViewById(R.id.tb_pdf)
        tb_pdf.setNavigationOnClickListener {
            this.finish()
        }
    }


    private var getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
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

            pdfUri = outputFile.toUri()
            pdfView.fromFile(outputFile).show()
            PDFBoxResourceLoader.init(baseContext)
            var doc = PDDocument.load(outputFile)
            tb_pdf.title = "${doc.numberOfPages}"
//            if (doc.numberOfPages % 2 != 0) {
//                doc.addPage(PDPage())
//            }
//            doc.save(outputFile)
            doc.close()
            doc = PDDocument.load(outputFile)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        setResult(111, Intent().apply {
            this.data = pdfUri
        })
    }
}