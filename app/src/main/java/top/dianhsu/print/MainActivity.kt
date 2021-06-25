package top.dianhsu.print

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import de.gmuth.ipp.cups.CupsClient
import java.net.URI


class MainActivity : AppCompatActivity() {
    private lateinit var printerSpinner: Spinner
    private lateinit var ivPrinter:ImageView
    private lateinit var ivPageSelect:ImageView
    private lateinit var ivDoubleSide:ImageView
    private lateinit var ivFileAdd:ImageView
    private lateinit var ivFilePreview:ImageView
    private lateinit var selectPdfBtn: ConstraintLayout
    private lateinit var previewPdfBtn:ConstraintLayout
    private lateinit var printPdfBtn: Button
    private lateinit var printerSpinnerAdapter: ArrayAdapter<String>
    private lateinit var printerData: ArrayList<String>
    private lateinit var printerUri: ArrayList<String>
    private var fileName:String?=null
    private var pdfUri: Uri? = null
    private lateinit var pagesEditText: EditText
    private var _handler: Handler?=null
    private val mHandler get() = _handler!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beta)
        initData()
        initView()
        initListener()
        initPrinter()
        initHandler()
    }

    private fun initData() {
        printerData = ArrayList()
        printerUri = ArrayList()
    }

    private fun initHandler() {
        _handler=object : Handler(mainLooper) {
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

    private fun initPrinter() {
        printerDiscovery()
    }

    private fun initView() {
        printerSpinner = findViewById(R.id.spinner)
        pagesEditText = findViewById(R.id.pages_edit_text)
        printerSpinnerAdapter =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, printerData)
        printerSpinner.adapter = printerSpinnerAdapter
        ivDoubleSide=findViewById(R.id.iv_double_sided)
        ivFileAdd=findViewById(R.id.iv_file_add)
        ivFilePreview=findViewById(R.id.iv_file_preview)
        ivPageSelect=findViewById(R.id.iv_pages)
        ivPrinter=findViewById(R.id.iv_printer)
        selectPdfBtn=findViewById(R.id.select_pdf_btn)
        previewPdfBtn=findViewById(R.id.preview_pdf_btn)
        printPdfBtn=findViewById(R.id.print_pdf_btn)
    }

    private fun initListener() {

        selectPdfBtn.setOnClickListener {
            content.launch(Intent(this, PdfPreviewActivity::class.java).apply {
                putExtra("isPreview",false)
            })
        }

        printPdfBtn.setOnClickListener {
            //printDocument()
        }

        previewPdfBtn.setOnClickListener {
            val intent=Intent(this,PdfPreviewActivity::class.java)
            intent.putExtra("isPreview",true)
            intent.data = pdfUri
            intent.putExtra("fileName",fileName)
            Log.e("MainActivity",intent.data.toString())
            startActivity(intent)
        }
    }


    private val content = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == 111){
            pdfUri = it.data?.data
            fileName=it.data?.getStringExtra("fileName")
            Log.e("MainActivityReturn",fileName.toString())
        }

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


    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }
}


