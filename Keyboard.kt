package com.greyb.keyboard

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.inputmethodservice.InputMethodService
import android.util.AttributeSet
import android.util.TypedValue
import android.util.Xml
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// ── Keyboard Data Models (Programmatic) ────────────────────────────────────

enum class KeyType {
    REGULAR, MODIFIER, STICKY, REPEATABLE, ACTION, SPACE
}

data class KeyDef(
    val code: Int,
    val label: String,
    val widthPercent: Float = 10f,
    val keyType: KeyType = KeyType.REGULAR,
    val edgeFlags: Int = 0
)

data class RowDef(val keys: List<KeyDef>)
data class LayoutDef(val rows: List<RowDef>)

// ── Predefined Layouts ─────────────────────────────────────────────────────

val qwertyDef = LayoutDef(rows = listOf(
    RowDef(listOf(KeyDef(49,"1",10f,edgeFlags=4), KeyDef(50,"2"), KeyDef(51,"3"), KeyDef(52,"4"), KeyDef(53,"5"), KeyDef(54,"6"), KeyDef(55,"7"), KeyDef(56,"8"), KeyDef(57,"9"), KeyDef(48,"0",edgeFlags=2))),
    RowDef(listOf(KeyDef(113,"q",edgeFlags=1), KeyDef(119,"w"), KeyDef(101,"e"), KeyDef(114,"r"), KeyDef(116,"t"), KeyDef(121,"y"), KeyDef(117,"u"), KeyDef(105,"i"), KeyDef(111,"o"), KeyDef(112,"p",edgeFlags=2))),
    RowDef(listOf(KeyDef(97,"a",11f,edgeFlags=1), KeyDef(115,"s"), KeyDef(100,"d"), KeyDef(102,"f"), KeyDef(103,"g"), KeyDef(104,"h"), KeyDef(106,"j"), KeyDef(107,"k"), KeyDef(108,"l",11f,edgeFlags=2))),
    RowDef(listOf(KeyDef(-1,"⇧",14f,KeyType.STICKY,edgeFlags=1), KeyDef(122,"z"), KeyDef(120,"x"), KeyDef(99,"c"), KeyDef(118,"v"), KeyDef(98,"b"), KeyDef(110,"n"), KeyDef(109,"m"), KeyDef(-5,"⌫",14f,KeyType.REPEATABLE,edgeFlags=2))),
    RowDef(listOf(KeyDef(-2,"?123",14f,KeyType.ACTION,edgeFlags=1), KeyDef(44,","), KeyDef(32,"",42f,KeyType.SPACE), KeyDef(46,"."), KeyDef(-4,"⏎",14f,KeyType.ACTION,edgeFlags=2)))
))

val symbolDef = LayoutDef(rows = listOf(
    RowDef(listOf(KeyDef(49,"1",edgeFlags=4), KeyDef(50,"2"), KeyDef(51,"3"), KeyDef(52,"4"), KeyDef(53,"5"), KeyDef(54,"6"), KeyDef(55,"7"), KeyDef(56,"8"), KeyDef(57,"9"), KeyDef(48,"0",edgeFlags=2))),
    RowDef(listOf(KeyDef(64,"@",edgeFlags=1), KeyDef(35,"#"), KeyDef(36,"$"), KeyDef(37,"%"), KeyDef(38,"&"), KeyDef(45,"-"), KeyDef(43,"+"), KeyDef(40,"("), KeyDef(41,")"), KeyDef(47,"/",edgeFlags=2))),
    RowDef(listOf(KeyDef(33,"!",11f,edgeFlags=1), KeyDef(34,"\""), KeyDef(39,"'"), KeyDef(58,":"), KeyDef(59,";"), KeyDef(47,"/"), KeyDef(63,"?"), KeyDef(126,"~"), KeyDef(96,"`",11f,edgeFlags=2))),
    RowDef(listOf(KeyDef(-8,"ABC",14f,KeyType.ACTION,edgeFlags=1), KeyDef(92,"\\"), KeyDef(124,"|"), KeyDef(60,"<"), KeyDef(62,">"), KeyDef(123,"{"), KeyDef(125,"}"), KeyDef(-5,"⌫",14f,KeyType.REPEATABLE,edgeFlags=2))),
    RowDef(listOf(KeyDef(-2,"?123",14f,KeyType.ACTION,edgeFlags=1), KeyDef(44,","), KeyDef(32,"",42f,KeyType.SPACE), KeyDef(46,"."), KeyDef(-4,"⏎",14f,KeyType.ACTION,edgeFlags=2)))
))

// ── Keyboard Model ─────────────────────────────────────────────────────────

class Key(
    var x: Int = 0, var y: Int = 0,
    var width: Int = 0, var height: Int = 0,
    var code: Int = 0, var label: String = "",
    var keyType: KeyType = KeyType.REGULAR,
    var isPressed: Boolean = false, var isStickyOn: Boolean = false
) {
    val isSticky get() = keyType == KeyType.STICKY
    val isRepeatable get() = keyType == KeyType.REPEATABLE
    
    fun isInside(tx: Int, ty: Int) = tx in x..(x+width) && ty in y..(y+height)
    
    fun displayLabel(shifted: Boolean): String {
        if (code == 32) return ""
        if (code < 0) return label
        val ch = if (shifted && code.toChar().isLowerCase()) code.toChar().uppercaseChar() else code.toChar()
        return ch.toString()
    }
}

class KeyboardLayout {
    val keys = mutableListOf<Key>()
    var width = 0
    var height = 0
    
    companion object {
        fun from(def: LayoutDef, screenW: Int, keyH: Int): KeyboardLayout {
            val layout = KeyboardLayout()
            var y = 0
            val gap = 2
            for (row in def.rows) {
                val totalPercent = row.keys.sumOf { it.widthPercent }
                val scale = screenW / totalPercent
                var x = 0
                for (k in row.keys) {
                    val kw = (k.widthPercent * scale).toInt() - gap
                    layout.keys.add(Key(x, y, kw, keyH - gap, k.code, k.label, k.keyType))
                    x += (k.widthPercent * scale).toInt()
                }
                y += keyH
            }
            layout.width = screenW
            layout.height = y
            return layout
        }
    }
}

// ── KeyboardView ───────────────────────────────────────────────────────────

class KeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onKeyListener: ((Int) -> Unit)? = null
    var layout: KeyboardLayout? = null
    var isShifted = false
    
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
    private var currentKey: Key? = null
    private var repeatRunnable: Runnable? = null
    
    private val keyBgNormal = Color.argb(128, 42, 42, 42)
    private val keyBgPressed = Color.argb(128, 180, 180, 170)
    private val keyTextColor = Color.argb(128, 220, 220, 215)
    private val bgColor = Color.argb(128, 26, 26, 26)

    fun setLayout(def: LayoutDef) {
        layout = KeyboardLayout.from(def, resources.displayMetrics.widthPixels, 96)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val l = layout
        if (l != null) setMeasuredDimension(l.width, l.height)
        else super.onMeasure(wSpec, hSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgColor)
        val l = layout ?: return
        val padding = 2f
        for (key in l.keys) {
            val bg = when {
                key.isSticky && isShifted -> keyBgPressed
                key.isPressed -> keyBgPressed
                else -> keyBgNormal
            }
            paint.color = bg
            canvas.drawRoundRect(
                key.x + padding, key.y + padding,
                (key.x + key.width) - padding, (key.y + key.height) - padding,
                8f, 8f, paint
            )
            val label = key.displayLabel(isShifted)
            if (label.isNotEmpty()) {
                paint.color = keyTextColor
                paint.textSize = if (key.code < 0 && key.code != 32) 28f else 32f
                canvas.drawText(label, key.x + key.width / 2f, key.y + key.height / 2f + paint.textSize / 3f, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val l = layout ?: return true
        val x = event.x.toInt(); val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentKey = l.keys.find { it.isInside(x, y) }
                currentKey?.isPressed = true
                if (currentKey?.isRepeatable == true) {
                    triggerKey()
                    repeatRunnable = object : Runnable {
                        override fun run() { triggerKey(); postDelayed(this, 50) }
                    }
                    postDelayed(repeatRunnable!!, 400)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val inside = currentKey?.isInside(x, y) == true
                if (!inside) { currentKey?.isPressed = false; currentKey = null; removeCallbacks(repeatRunnable) }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(repeatRunnable)
                currentKey?.let { key ->
                    if (key.isInside(x, y) && !key.isRepeatable) triggerKey()
                    key.isPressed = false
                }
                currentKey = null
                invalidate()
            }
        }
        return true
    }

    private fun triggerKey() {
        val key = currentKey ?: return
        onKeyListener?.invoke(key.code)
    }
}

// ── IME Service ────────────────────────────────────────────────────────────

class GreyBIME : InputMethodService() {
    
    private lateinit var keyboardView: KeyboardView
    private var currentDef = qwertyDef

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.setLayout(currentDef)
        keyboardView.onKeyListener = { code -> handleKey(code) }
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.setLayout(currentDef)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun handleKey(code: Int) {
        val ic = currentInputConnection ?: return
        when (code) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -1 -> { keyboardView.isShifted = !keyboardView.isShifted; keyboardView.invalidate() }
            -2 -> {
                currentDef = if (currentDef == qwertyDef) symbolDef else qwertyDef
                keyboardView.setLayout(currentDef)
            }
            -4 -> {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }
            -8 -> { currentDef = qwertyDef; keyboardView.setLayout(currentDef) }
            else -> {
                var ch = code.toChar()
                if (keyboardView.isShifted && ch.isLowerCase()) ch = ch.uppercaseChar()
                ic.commitText(ch.toString(), 1)
                if (keyboardView.isShifted) { keyboardView.isShifted = false; keyboardView.invalidate() }
            }
        }
    }
}
