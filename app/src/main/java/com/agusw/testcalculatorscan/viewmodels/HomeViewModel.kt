package com.agusw.testcalculatorscan.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.agusw.testcalculatorscan.App
import com.agusw.testcalculatorscan.db.TestDB
import com.agusw.testcalculatorscan.db.entities.Result
import com.google.mlkit.vision.text.Text
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val db: TestDB,
    private val app: App,
    moshi: Moshi,
) : ViewModel() {

    val listResult: MutableLiveData<List<Result>> = MutableLiveData()
    val message: MutableLiveData<String> = MutableLiveData()
    private val jsonAdapter: JsonAdapter<List<Result>> by lazy {
        moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                Result::class.java
            )
        )
    }
    private var storage = "Database"

    init {
        viewModelScope.launch {
            listResult.postValue(db.result().get())
        }
    }

    suspend fun changeStorage(storage: String) {
        if (storage == this.storage) return

        this.storage = storage
        if (this.storage == "Database") {
            db.result().insert(listResult.value ?: emptyList())
            writeFile("")
        } else {
            writeFile(jsonAdapter.toJson(listResult.value))
            db.result().nuke()
        }
    }

    private fun writeFile(text: String) {
        val encryptedFile = EncryptedFile.Builder(
            app.baseContext,
            File(app.filesDir, "data.txt").also { if (it.exists()) it.delete() },
            MasterKey(app.baseContext, "test"),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().apply {
            write(text.toByteArray(StandardCharsets.UTF_8))
            flush()
            close()
        }
    }

    private fun failProcessInput(): Boolean {
        message.postValue("Failed to process")
        return false
    }

    private val regexNumber by lazy { "\\-?\\d+".toRegex() }
    private val regexSymbol by lazy { "[x\\+\\-\\:\\*\\/]".toRegex() }
    suspend fun processInput(input: Text): Boolean {
        Timber.d("text: ${input.text}")

        // find first number
        val matchNumber = regexNumber.find(input.text) ?: return failProcessInput()
        val firstNumber = matchNumber.value.toInt()

        // find second number
        val nextMatchNumber = matchNumber.next() ?: return failProcessInput()
        val secondNumber = nextMatchNumber.value.toInt()

        // find symbol
        val matchSymbol =
            regexSymbol.find(input.text, matchNumber.value.length) ?: return failProcessInput()
        val symbol = matchSymbol.value

        val result = Result(
            input = "$firstNumber $symbol $secondNumber",
            output = processOperation(firstNumber, secondNumber, symbol).toString()
        )
        if (storage == "Database") {
            db.result().insert(result)
            listResult.postValue(db.result().get())
        } else {
            val newList = (listResult.value ?: emptyList()).toMutableList().apply {
                add(result)
            }
            writeFile(jsonAdapter.toJson(newList))
            listResult.postValue(newList)
        }

        return true
    }

    private fun processOperation(firstNum: Int, secondNum: Int, symbol: String): Int {
        return when (symbol) {
            "+" -> firstNum + secondNum
            "-" -> firstNum - secondNum
            "*", "x", "X" -> firstNum * secondNum
            "/", ":" -> firstNum / secondNum
            else -> 0
        }
    }
}