package com.qrdroid.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.ResultPoint
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.EnumMap

class MainActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.decodeContinuous(barcodeCallback)
        barcodeView.setTorchListener(this)

        findViewById<Button>(R.id.generateQRButton).setOnClickListener { generateQRCode() }
        findViewById<Button>(R.id.saveQRButton).setOnClickListener { saveQRCode() }

    }

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {

        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

        }
    }

    private fun generateQRCode() {
        // Obtiene el valor del TextInputEditText
        val inputValue = findViewById<TextInputEditText>(R.id.textInputEditText).text.toString()

        try {
            // Genera el código QR utilizando ZXing
            val bitMatrix = encodeAsBitmap(inputValue)

            // Convierte el BitMatrix en un Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }

            // Muestra el código QR en el ImageView
            findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(bmp)

            // Oculta el teclado virtual
            hideKeyboard()

        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    // Método para ocultar el teclado virtual
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    @Throws(WriterException::class)
    private fun encodeAsBitmap(contents: String): BitMatrix {
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val result = MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, 400, 400, hints)
        return result
    }


    private fun saveQRCode() {
        val qrCodeImageView = findViewById<ImageView>(R.id.qrCodeImageView)
        val bitmap = (qrCodeImageView.drawable as BitmapDrawable).bitmap

        // Guardar en la galería de fotos
        val savedImageURL = saveToGallery(bitmap, "QR_Code")

        // Guardar en almacenamiento externo de la aplicación
        saveToExternalStorage(bitmap, "QR_Code.jpg")

        // Muestra un mensaje indicando la ubicación de la imagen guardada
        Toast.makeText(this, "Imagen guardada en: $savedImageURL", Toast.LENGTH_SHORT).show()
    }

    private fun saveToGallery(bitmap: Bitmap, displayName: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        resolver.openOutputStream(uri!!).use { outputStream ->
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }

        return uri.toString()
    }

    private fun saveToExternalStorage(bitmap: Bitmap, fileName: String) {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val qrCodeFile = File(storageDirectory, fileName)

        try {
            val stream = FileOutputStream(qrCodeFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onTorchOn() {
        // Implementa la lógica para cuando se encienda el flash.
    }

    override fun onTorchOff() {
        // Implementa la lógica para cuando se apague el flash.
    }
}
