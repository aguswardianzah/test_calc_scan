package com.agusw.testcalculatorscan.pages

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agusw.testcalculatorscan.BuildConfig
import com.agusw.testcalculatorscan.databinding.HomePageBinding
import com.agusw.testcalculatorscan.databinding.ResultItemBinding
import com.agusw.testcalculatorscan.db.entities.Result
import com.agusw.testcalculatorscan.utils.ImageHandler
import com.agusw.testcalculatorscan.viewmodels.HomeViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class HomePage : AppCompatActivity() {

    private val viewModel by viewModels<HomeViewModel>()
    private val layout by lazy { HomePageBinding.inflate(layoutInflater) }
    private val adapter by lazy { DataAdapter() }
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layout.root)

        layout.rvData.adapter = adapter
        layout.btnAdd.setOnClickListener { addItem() }

        viewModel.listResult.observe(this, Observer(adapter::submitList))
        viewModel.message.observe(this, Observer(this::toast))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.addSubMenu("Storage")?.apply {
            add("File")
            add("Database")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title.toString() == "Database" || item.title.toString() == "File")
            lifecycleScope.launch {
                viewModel.changeStorage(item.title.toString())
            }
        return super.onOptionsItemSelected(item)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { _ ->
            ImageHandler.processImageResult(baseContext, photoUri, true)?.let {
                textRecognize(InputImage.fromBitmap(it, 0))
            } ?: toast("Invalid source")
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            result?.let { textRecognize(InputImage.fromFilePath(baseContext, it)) }
                ?: toast("Invalid source")
        }

    private fun textRecognize(inputImage: InputImage) = recognizer.process(inputImage)
        .addOnSuccessListener {
            lifecycleScope.launch {
                loading()
                viewModel.processInput(it)
                dismissLoading()
            }
        }
        .addOnFailureListener(Timber::e)

    private fun openCamera() {
        try {
            val file = File.createTempFile("result", ".jpg", cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            photoUri = FileProvider.getUriForFile(
                baseContext,
                BuildConfig.APPLICATION_ID,
                file
            )

            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun addItem() {
        if (BuildConfig.INPUT_SOURCE.contains("CAMERA")) {
            if (checkPermission(Manifest.permission.CAMERA))
                openCamera()
            else
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            pickImageLauncher.launch(("image/*"))
        }
    }

    private fun checkPermission(permission: String) =
        ContextCompat.checkSelfPermission(
            baseContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1)
            if (checkPermission(Manifest.permission.CAMERA))
                openCamera()
            else
                toast("Can't start camera")
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private inner class DataAdapter : ListAdapter<Result, ViewHolder>(object :
        DiffUtil.ItemCallback<Result>() {
        override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean =
            oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(
        parent: ViewGroup,
        val binding: ResultItemBinding = ResultItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(result: Result) {
            binding.tvInput.text = "Input: ${result.input}"
            binding.tvOutput.text = "Output: ${result.output}"
        }
    }

    var loadingDialog: ProgressDialog? = null
    private fun loading(
        title: String = "Mohon tunggu",
        msg: String = "Sedang memproses ...",
        indeterminate: Boolean = true
    ) {
        dismissLoading()
        loadingDialog = ProgressDialog.show(this, title, msg, indeterminate)
    }

    private fun dismissLoading() {
        loadingDialog?.dismiss()
    }
}