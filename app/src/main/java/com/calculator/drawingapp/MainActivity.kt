package com.calculator.drawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private var mDrawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null



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
}