package top.dianhsu.print

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.pdfview.PDFView
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import de.gmuth.ipp.cups.CupsClient
import java.io.File
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var printerSpinner: Spinner
    private lateinit var printerSpinnerAdapter: ArrayAdapter<String>
    private lateinit var printerData: ArrayList<String>
    private lateinit var printerUri: ArrayList<String>
    private var pdfUri: Uri? = null
    private lateinit var pdfView: PDFView
    private lateinit var pagesEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        printerSpinner = findViewById(R.id.spinner)
        printerData = ArrayList()
        printerUri = ArrayList()
        pdfView = findViewById(R.id.pdf_view)
        pagesEditText = findViewById(R.id.pages_edit_text)
        printerSpinnerAdapter =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, printerData)
        printerSpinner.adapter = printerSpinnerAdapter
        val selectPdfBtn: Button = findViewById(R.id.select_pdf_btn)
        val printPdfBtn: Button = findViewById(R.id.print_pdf_btn)
        selectPdfBtn.setOnClickListener {
            getContent.launch("application/pdf")
        }
        printPdfBtn.setOnClickListener {
            //printDocument()
        }
        printerDiscovery()
    }

    private fun printDocument() {
        Thread {
            val client = CupsClient(URI.create(printerUri[printerSpinner.selectedItemId.toInt()]))
            pdfUri?.let { uri ->
                contentResolver.openInputStream(uri)?.let { ins ->
                    client.getDefault().printJob(ins)
                }
            }

        }.start()

    }

    private fun printerDiscovery() {
        Thread {
            val conn = Printer()
            mHandler.sendEmptyMessage(0)
            val msg = Message()
            msg.obj = conn.findPrinters()
            mHandler.sendMessage(msg)
        }.start()
    }

    var getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
            pagesEditText.setText("${doc.numberOfPages}")
//            if (doc.numberOfPages % 2 != 0) {
//                doc.addPage(PDPage())
//            }
//            doc.save(outputFile)
            doc.close()
            doc = PDDocument.load(outputFile)
        }
    }

    var mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                0 -> {
                    //完成主界面更新,拿到数据
                    printerData.clear()
                    printerUri.clear()
                    val printer = msg.obj as ArrayList<*>?
                    printer?.forEach {
                        val nex = it as Printer.PrinterInfo
                        Log.d("Find Printer", "${nex.name}, ${nex.uri}")
                        printerData.add(nex.name.toString())
                        printerUri.add(nex.uri.toString())
                    }
                    printerSpinnerAdapter.notifyDataSetChanged()
                }
                else -> {
                }
            }
        }
    }
}


