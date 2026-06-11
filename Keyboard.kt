package com.greyb.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class GreyBIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.onKeyListener = { code -> handleKey(code) }
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun handleKey(code: Int) {
        val ic = currentInputConnection ?: return
        when (code) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }
            32 -> ic.commitText(" ", 1)
            else -> ic.commitText(code.toChar().toString(), 1)
        }
    }
}

class KeyboardView(context: Context) : View(context) {

    var onKeyListener: ((Int) -> Unit)? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = 40f
    }

    private val bgColor = Color.argb(128, 26, 26, 26)
    private val keyNormal = Color.argb(128, 42, 42, 42)
    private val keyPressed = Color.argb(128, 180, 180, 170)
    private val textColor = Color.argb(128, 220, 220, 215)

    private val keys = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123",","," ",".","⏎")
    )

    private val codes = mapOf(
        "⇧" to -1, "⌫" to -5, "?123" to -2, "⏎" to -4, " " to 32
    )

    private var pressedKey: Pair<Int, Int>? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgColor)

        val totalWidth = width.toFloat()
        val keyHeight = 96f
        val margin = 2f
        val startY = 0f

        for ((rowIdx, row) in keys.withIndex()) {
            val numKeys = row.size
            val keyWidth = (totalWidth / numKeys) - (margin * 2)
            val y = startY + rowIdx * keyHeight

            for ((colIdx, label) in row.withIndex()) {
                val x = colIdx * (keyWidth + margin * 2) + margin
                val isPressed = pressedKey?.let { it.first == rowIdx && it.second == colIdx } == true

                paint.color = if (isPressed) keyPressed else keyNormal
                canvas.drawRoundRect(x, y, x + keyWidth, y + keyHeight - margin, 8f, 8f, paint)

                paint.color = textColor
                val displayLabel = if (label.length == 1 && label[0].isLowerCase()) label else label
                canvas.drawText(displayLabel, x + keyWidth / 2, y + keyHeight / 2 + 12f, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val totalWidth = width.toFloat()
        val keyHeight = 96f

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val y = event.y.toInt()
                val rowIdx = (y / keyHeight).toInt()
                if (rowIdx in keys.indices) {
                    val row = keys[rowIdx]
                    val numKeys = row.size
                    val keyWidth = totalWidth / numKeys
                    val colIdx = (event.x / keyWidth).toInt()
                    if (colIdx in row.indices) {
                        pressedKey = Pair(rowIdx, colIdx)
                    } else {
                        pressedKey = null
                    }
                } else {
                    pressedKey = null
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                pressedKey?.let { (row, col) ->
                    val label = keys[row][col]
                    val code = codes[label] ?: label[0].code
                    onKeyListener?.invoke(code)
                }
                pressedKey = null
                invalidate()
            }
        }
        return true
    }
}
