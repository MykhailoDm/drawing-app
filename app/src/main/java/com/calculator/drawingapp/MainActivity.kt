package com.calculator.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mDrawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageBackgroundView: ImageView = findViewById(R.id.iv_background)
            imageBackgroundView.setImageURI(result.data?.data)
        }
    }

    val requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            val permissionName = it.key
            val isGranted = it.value

            if (isGranted) {
                Toast.makeText(this, "Permission Granted now you can read the storage files.", Toast.LENGTH_LONG).show()

                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            } else {
                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    Toast.makeText(this, "You just denied the permission.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDrawingView = findViewById(R.id.drawing_view)
        mDrawingView?.setSizeForBrush(20F)

        val linerLayoutPaintColors: LinearLayout = findViewById(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linerLayoutPaintColors[1] as? ImageButton
        mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        val brushIb: ImageButton = findViewById(R.id.brush_setting_btn)
        brushIb.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            mDrawingView?.onClickUndo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val fileDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitmapFromView(fileDrawingView))
                }
            }
        }

        val galleryBtn: ImageButton = findViewById(R.id.gallery_btn)
        galleryBtn.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")
        chooseDialogBrushSizeListener(brushDialog, R.id.small_brush, 10F)
        chooseDialogBrushSizeListener(brushDialog, R.id.medium_brush, 20F)
        chooseDialogBrushSizeListener(brushDialog, R.id.large_brush, 30F)

        brushDialog.show()
    }

    private fun chooseDialogBrushSizeListener(dialog: Dialog, id: Int, size: Float) {
        val btn = dialog.findViewById<ImageView>(id)
        btn.setOnClickListener {
            mDrawingView?.setSizeForBrush(size)
            dialog.dismiss()
        }
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as? ImageButton
            imageButton?.tag?.let { colorTag ->
                mDrawingView?.setColor(colorTag.toString())
                imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
                mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
                mImageButtonCurrentPaint = view
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showRationaleDialog("Drawing App", "Drawing App needs to Access Your External Storage")
        } else {
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap !== null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")

                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bytes.toByteArray())
                    fileOutputStream.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String) {

        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}