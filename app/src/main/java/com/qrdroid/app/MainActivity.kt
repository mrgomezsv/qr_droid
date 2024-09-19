package com.qrdroid.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.EnumMap
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.google.zxing.ResultPoint

class MainActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.decodeContinuous(barcodeCallback)
        barcodeView.setTorchListener(this)

        val generateQRButton: Button = findViewById(R.id.generateQRButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val saveQRButton: Button = findViewById(R.id.saveQRButton)
        val madeByText: TextView = findViewById(R.id.madeByText)
        val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageView)

        generateQRButton.setOnClickListener { generateQRCode() }
        clearButton.setOnClickListener { clearFields() }
        saveQRButton.setOnClickListener { saveQRCode() }

        // Inicialmente, ocultar el botón Clear y el botón Save QR
        clearButton.visibility = View.GONE
        saveQRButton.visibility = View.GONE
        madeByText.visibility = View.GONE // Ocultar madeByText inicialmente

        link()
    }

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            // Maneja el resultado del escaneo aquí, si es necesario
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // Maneja los puntos de resultado posibles aquí, si es necesario
        }
    }

    private fun generateQRCode() {
        // Obtiene el valor del TextInputEditText
        val inputValue = findViewById<TextInputEditText>(R.id.textInputEditText).text.toString()

        if (inputValue.isEmpty()) {
            // Muestra un mensaje Toast si el campo de texto está vacío
            Toast.makeText(this, "Por favor ingresa una url o lo que quieras Generar su QR", Toast.LENGTH_SHORT).show()
            return
        }

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
            val qrCodeImageView = findViewById<ImageView>(R.id.qrCodeImageView)
            qrCodeImageView.setImageBitmap(bmp)

            // Aplicar animación al ImageView
            val animation = AnimationUtils.loadAnimation(this, R.anim.qr_slide_up_bounce)
            qrCodeImageView.startAnimation(animation)

            // Oculta el botón generateQRButton
            findViewById<Button>(R.id.generateQRButton).visibility = View.GONE

            // Muestra el botón clearButton
            findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE

            // Muestra el botón saveQRButton
            findViewById<Button>(R.id.saveQRButton).visibility = View.VISIBLE

            // Muestra el TextView madeByText si saveQRButton es visible
            findViewById<TextView>(R.id.madeByText).visibility = View.VISIBLE

            // Oculta el teclado virtual
            hideKeyboard()

        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun clearFields() {
        // Limpia el campo de texto
        findViewById<TextInputEditText>(R.id.textInputEditText).text?.clear()

        // Limpia el ImageView
        findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(null)

        // Oculta el botón clearButton
        findViewById<Button>(R.id.clearButton).visibility = View.GONE

        // Oculta el botón saveQRButton
        findViewById<Button>(R.id.saveQRButton).visibility = View.GONE

        // Oculta el TextView madeByText
        findViewById<TextView>(R.id.madeByText).visibility = View.GONE

        // Muestra el botón generateQRButton
        findViewById<Button>(R.id.generateQRButton).visibility = View.VISIBLE
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

    private fun link() {
        val txtUrl: TextView = findViewById(R.id.madeByText)
        txtUrl.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://mrgomezsv.github.io/")
            )
            startActivity(intent)
        }
    }
}
