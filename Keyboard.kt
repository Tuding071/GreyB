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

// ── Utilities ────────────────────────────────────────────────────────────────

object CharUtil {
    fun Char.cycleCharacter(locale: Locale): Char =
        if (Character.isUpperCase(this)) lowercase(locale)[0] else uppercase(locale)[0]
}

object StringUtil {
    fun String.isPunctuation(): Boolean = matches(Regex("[_\\-,.]"))
}

object DimensUtil {
    fun android.content.res.TypedArray.getDimensionOrFraction(index: Int, base: Int, defVal: Int): Int {
        val value = peekValue(index) ?: return defVal
        return when (value.type) {
            TypedValue.TYPE_DIMENSION -> getDimensionPixelOffset(index, defVal)
            TypedValue.TYPE_FRACTION -> getFraction(index, base, base, defVal.toFloat()).roundToInt()
            else -> defVal
        }
    }
}

// ── Keyboard model ───────────────────────────────────────────────────────────

class Keyboard(context: Context, layoutRes: Int) {

    companion object {
        const val TAG_KEYBOARD = "Keyboard"
        const val TAG_ROW = "Row"
        const val TAG_KEY = "Key"
        const val EDGE_LEFT = 0x01
        const val EDGE_RIGHT = 0x02
        const val EDGE_TOP = 0x04
        const val EDGE_BOTTOM = 0x08
        const val KEYCODE_ENTER = '\n'.code
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -2
        const val KEYCODE_DONE = -4
        const val KEYCODE_DELETE = -5
        const val KEYCODE_CLOSE_KEYBOARD = -99
        const val NOT_A_KEY = -1
        private val KEY_STATE_NORMAL = intArrayOf()
        private val KEY_STATE_PRESSED = intArrayOf(android.R.attr.state_pressed)
        private val KEY_STATE_NORMAL_ON = intArrayOf(android.R.attr.state_checkable, android.R.attr.state_checked)
        private val KEY_STATE_NORMAL_OFF = intArrayOf(android.R.attr.state_checkable)
        private val KEY_STATE_PRESSED_ON = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_checkable, android.R.attr.state_checked)
        private val KEY_STATE_PRESSED_OFF = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_checkable)
    }

    private val displayWidth = context.resources.displayMetrics.widthPixels
    private val displayHeight = context.resources.displayMetrics.heightPixels
    internal var keyboardWidth = 0
    internal var keyboardHeight = 0
    private var defaultKeyWidth = displayWidth / 10
    private var defaultKeyHeight = defaultKeyWidth
    private var defaultKeyHorizontalGap = 0
    private var defaultKeyVerticalGap = 0
    internal val keys = ArrayList<Key>()
    internal var isShifted = false
    private val shiftKeys = arrayOfNulls<Key>(2)

    init {
        loadKeyboard(context, context.resources.getXml(layoutRes))
    }

    @Throws(XmlPullParserException::class)
    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var x = 0; var y = 0
        var inKey = false; var inRow = false
        var currentRow: Row? = null; var currentKey: Key? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    TAG_KEYBOARD -> parseKeyboardAttributes(context.resources, parser)
                    TAG_ROW -> {
                        x = 0; inRow = true
                        currentRow = Row(context.resources, parser)
                    }
                    TAG_KEY -> {
                        inKey = true
                        currentKey = currentRow?.let {
                            Key(context.resources, context.theme, x, y, parser, it).also { k ->
                                keys.add(k)
                                if (k.codes.isNotEmpty() && k.codes[0] == KEYCODE_SHIFT) {
                                    for (i in shiftKeys.indices) { if (shiftKeys[i] == null) { shiftKeys[i] = k; break } }
                                }
                                it.keys.add(k)
                            }
                        }
                    }
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (inKey) {
                    inKey = false
                    currentKey?.let { x += it.width + it.horizontalGap; if (x > keyboardWidth) keyboardWidth = x }
                } else if (inRow) {
                    inRow = false
                    currentRow?.let { y += it.keyHeight + it.keyVerticalGap }
                }
            }
        }
        keyboardHeight = y - defaultKeyVerticalGap
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val ta = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
        with(DimensUtil) {
            defaultKeyWidth = ta.getDimensionOrFraction(R.styleable.Keyboard_keyWidth, displayWidth, defaultKeyWidth)
            defaultKeyHeight = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHeight, displayHeight, defaultKeyHeight)
            defaultKeyHorizontalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHorizontalGap, displayWidth, defaultKeyHorizontalGap)
            defaultKeyVerticalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyVerticalGap, displayHeight, defaultKeyVerticalGap)
        }
        ta.recycle()
    }

    fun getKeyIndex(x: Int, y: Int): Int {
        for (i in keys.indices) { if (keys[i].isInside(x, y)) return i }
        return NOT_A_KEY
    }

    fun setShifted(state: Boolean): Boolean {
        shiftKeys.forEach { it?.isOn = state }
        if (isShifted != state) { isShifted = state; return true }
        return false
    }

    inner class Row(res: Resources, parser: XmlResourceParser) {
        internal val keyHeight: Int
        internal val keyVerticalGap: Int
        internal val keys = ArrayList<Key>()
        private val keyWidth: Int
        private val keyHorizontalGap: Int
        internal val rowEdgeFlags: Int
        internal val keyboardMode: Int
        init {
            val ta = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
            with(DimensUtil) {
                keyWidth = ta.getDimensionOrFraction(R.styleable.Keyboard_keyWidth, displayWidth, defaultKeyWidth)
                keyHeight = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHeight, displayHeight, defaultKeyHeight)
                keyHorizontalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHorizontalGap, displayWidth, defaultKeyHorizontalGap)
                keyVerticalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyVerticalGap, displayHeight, defaultKeyVerticalGap)
            }
            ta.recycle()
            val ta2 = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Row)
            rowEdgeFlags = ta2.getInt(R.styleable.Keyboard_Row_rowEdgeFlags, 0)
            keyboardMode = ta2.getResourceId(R.styleable.Keyboard_Row_keyboardMode, 0)
            ta2.recycle()
        }
    }

    inner class Key(res: Resources, theme: Resources.Theme, x: Int, y: Int, parser: XmlResourceParser, parentRow: Row) {
        val x: Int; val y: Int
        internal val width: Int; internal val height: Int
        internal val horizontalGap: Int; internal val verticalGap: Int
        internal var codes = intArrayOf()
        private val edgeFlags: Int
        private val sticky: Boolean
        internal var isOn = false
        internal val isRepeatable: Boolean
        internal var label: CharSequence = ""
        internal val icon: Drawable?
        private var _isPressed = false
        internal val isPressed get() = _isPressed

        init {
            val ta = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
            with(DimensUtil) {
                width = ta.getDimensionOrFraction(R.styleable.Keyboard_keyWidth, displayWidth, defaultKeyWidth)
                height = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHeight, displayHeight, defaultKeyHeight)
                horizontalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyHorizontalGap, displayWidth, defaultKeyHorizontalGap)
                verticalGap = ta.getDimensionOrFraction(R.styleable.Keyboard_keyVerticalGap, displayHeight, defaultKeyVerticalGap)
            }
            this.x = x + horizontalGap; this.y = y
            ta.recycle()
            val ta2 = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
            val tv = TypedValue()
            ta2.getValue(R.styleable.Keyboard_Key_codes, tv)
            codes = when (tv.type) {
                TypedValue.TYPE_INT_DEC, TypedValue.TYPE_INT_HEX -> intArrayOf(tv.data)
                else -> intArrayOf()
            }
            edgeFlags = ta2.getInt(R.styleable.Keyboard_Key_keyEdgeFlags, 0) or parentRow.rowEdgeFlags
            sticky = ta2.getBoolean(R.styleable.Keyboard_Key_isSticky, false)
            isRepeatable = ta2.getBoolean(R.styleable.Keyboard_Key_isRepeatable, false)
            label = ta2.getText(R.styleable.Keyboard_Key_keyLabel) ?: ""
            icon = ta2.getDrawable(R.styleable.Keyboard_Key_keyIcon)?.also {
                it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                it.applyTheme(theme)
            }
            if (codes.isEmpty() && label.isNotEmpty()) codes = intArrayOf(label[0].code)
            ta2.recycle()
        }

        fun isInside(x: Int, y: Int): Boolean {
            val l = edgeFlags and EDGE_LEFT > 0; val r = edgeFlags and EDGE_RIGHT > 0
            val t = edgeFlags and EDGE_TOP > 0; val b = edgeFlags and EDGE_BOTTOM > 0
            return (x >= this.x || l && x <= this.x + width) &&
                (x < this.x + width || r && x >= this.x) &&
                (y >= this.y || t && y <= this.y + height) &&
                (y < this.y + height || b && y >= this.y)
        }

        fun adjustLabelCase(locale: Locale): String {
            var lbl = label
            if (isShifted && lbl.isNotEmpty() && lbl.length < 3 && Character.isLowerCase(lbl[0]))
                lbl = lbl.toString().uppercase(locale)
            return lbl.toString()
        }

        fun onPressed() { _isPressed = true }
        fun onReleased(inside: Boolean) { _isPressed = false; if (sticky && inside) isOn = !isOn }

        fun getDrawableState(): IntArray = when {
            isOn -> if (_isPressed) KEY_STATE_PRESSED_ON else KEY_STATE_NORMAL_ON
            sticky -> if (_isPressed) KEY_STATE_PRESSED_OFF else KEY_STATE_NORMAL_OFF
            _isPressed -> KEY_STATE_PRESSED
            else -> KEY_STATE_NORMAL
        }
    }
}

// ── KeyboardView ─────────────────────────────────────────────────────────────

class KeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface KeyboardActionListener {
        fun onKey(primaryCode: Int)
        fun onStopInput()
    }

    companion object {
        private const val REPEAT_KEY_DELAY = 50L
        private const val REPEAT_KEY_START_DELAY = 400L
        private const val MAX_ALPHA = 255
    }

    private val keyBackground: Drawable?
    private val keyBgPadding = Rect(0, 0, 0, 0)
    private var labelTextSize = 14
    private var keyTextColor = -0x1000000
    private val callbacks = ArrayList<KeyboardActionListener>()

    internal var keyboard: Keyboard? = null
    private var keyboardChanged = false
    private var drawPending = false
    private val dirtyRect = Rect()
    private var buffer: Bitmap? = null
    private var bitmapCanvas: Canvas? = Canvas()

    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        alpha = MAX_ALPHA
        color = Color.TRANSPARENT
    }

    private var currentKeyIndex = Keyboard.NOT_A_KEY
    private var abortKey = false
    private var lastPointerCount = 1
    private var lastPointerX = 0f
    private var lastPointerY = 0f

    private val performLongPress = Runnable {
        if (isPressed && isLongClickable) performLongClick()
    }

    private val performRepeatKey = object : Runnable {
        override fun run() { sendKeyEvent(); postDelayed(this, REPEAT_KEY_DELAY) }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.KeyboardView, defStyleAttr, 0)
        keyBackground = ta.getDrawable(R.styleable.KeyboardView_keyBackground)
        keyBackground?.getPadding(keyBgPadding)
        keyTextColor = ta.getColor(R.styleable.KeyboardView_keyTextColorPrimary, keyTextColor)
        labelTextSize = ta.getDimensionPixelSize(R.styleable.KeyboardView_labelTextSize, labelTextSize)
        ta.recycle()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    fun addCallback(cb: KeyboardActionListener) { if (!callbacks.contains(cb)) callbacks.add(cb) }
    fun removeCallback(cb: KeyboardActionListener) { callbacks.remove(cb) }

    fun setKeyboard(kb: Keyboard) {
        removeAllCallbacks()
        keyboard = kb
        abortKey = true
        keyboardChanged = true
        currentKeyIndex = Keyboard.NOT_A_KEY
        invalidateAllKeys()
    }

    fun setShifted(shifted: Boolean): Boolean {
        val kb = keyboard ?: return false
        return if (kb.setShifted(shifted)) { invalidateAllKeys(); true } else false
    }

    fun isShifted() = keyboard?.isShifted ?: false
    fun getLocale(): Locale = resources.configuration.locales[0]

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val kb = keyboard
        if (kb == null) setMeasuredDimension(paddingLeft + paddingRight, paddingTop + paddingBottom)
        else setMeasuredDimension(kb.keyboardWidth + paddingLeft + paddingRight, kb.keyboardHeight + paddingTop + paddingBottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); invalidateAllKeys() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawPending || buffer == null || keyboardChanged) onBufferDraw(null)
        buffer?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun onBufferDraw(invalidatedKey: Keyboard.Key?) {
        if (buffer == null || keyboardChanged && (buffer?.width != width || buffer?.height != height)) {
            buffer = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
            bitmapCanvas?.setBitmap(buffer)
        }
        keyboardChanged = false
        val kb = keyboard ?: return
        val canvas = bitmapCanvas ?: return
        canvas.setBitmap(buffer)
        canvas.clipRect(dirtyRect)
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        canvas.translate(0f, 0f)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        if (invalidatedKey != null) onKeyDraw(invalidatedKey)
        else kb.keys.forEach { onKeyDraw(it) }
        drawPending = false
        dirtyRect.setEmpty()
    }

    private fun onKeyDraw(key: Keyboard.Key) {
        val canvas = bitmapCanvas ?: return
        val state = key.getDrawableState()
        keyBackground?.state = state
        key.icon?.state = state
        val bounds = keyBackground?.bounds
        if (key.width != bounds?.right || key.height != bounds.bottom)
            keyBackground?.setBounds(0, 0, key.width, key.height)
        canvas.save()
        canvas.translate((key.x + paddingLeft).toFloat(), (key.y + paddingTop).toFloat())
        keyBackground?.draw(canvas)
        val lbl = key.adjustLabelCase(getLocale())
        if (lbl.isNotEmpty()) {
            paint.color = keyTextColor
            paint.textSize = labelTextSize.toFloat()
            canvas.drawText(
                lbl,
                (key.width - keyBgPadding.left - keyBgPadding.right) / 2f + keyBgPadding.left,
                (key.height - keyBgPadding.top - keyBgPadding.bottom) / 2f + (paint.textSize - paint.descent()) / 2f + keyBgPadding.top,
                paint
            )
        } else if (key.icon != null) {
            val x = (key.width - keyBgPadding.left - keyBgPadding.right - key.icon.intrinsicWidth) / 2f + keyBgPadding.left
            val y = (key.height - keyBgPadding.top - keyBgPadding.bottom - key.icon.intrinsicHeight) / 2f + keyBgPadding.top
            canvas.translate(x, y)
            key.icon.draw(canvas)
        }
        canvas.restore()
    }

    private fun invalidateAllKeys() {
        dirtyRect.union(0, 0, width, height)
        drawPending = true
        postInvalidate()
    }

    private fun invalidateKey(idx: Int) {
        val kb = keyboard ?: return
        if (drawPending || idx < 0 || idx >= kb.keys.size) return
        val key = kb.keys[idx]
        val l = key.x + paddingLeft; val t = key.y + paddingTop
        val r = key.x + key.width + paddingLeft; val b = key.y + key.height + paddingTop
        dirtyRect.union(l, t, r, b)
        onBufferDraw(key)
        postInvalidate(l, t, r, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        val result: Boolean
        if (pointerCount == lastPointerCount) {
            result = if (pointerCount == 1) { val r = handleTouchEvent(event); lastPointerX = event.x; lastPointerY = event.y; r } else true
        } else {
            result = if (pointerCount == 1) {
                val down = MotionEvent.obtain(event.eventTime, event.eventTime, MotionEvent.ACTION_DOWN, event.x, event.y, event.metaState)
                val r = handleTouchEvent(down); down.recycle()
                if (event.action == MotionEvent.ACTION_UP) handleTouchEvent(event) else r
            } else {
                val up = MotionEvent.obtain(event.eventTime, event.eventTime, MotionEvent.ACTION_UP, lastPointerX, lastPointerY, event.metaState)
                val r = handleTouchEvent(up); up.recycle(); r
            }
        }
        if (event.action == MotionEvent.ACTION_UP) performClick()
        lastPointerCount = pointerCount
        return result
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        val kb = keyboard ?: return true
        if (abortKey && event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_CANCEL) return true
        val touchX = (event.x - paddingLeft).toInt()
        val touchY = (event.y - paddingTop).toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortKey = false
                currentKeyIndex = kb.getKeyIndex(touchX, touchY)
                if (currentKeyIndex == Keyboard.NOT_A_KEY) return true
                val key = kb.keys[currentKeyIndex]
                isPressed = true; key.onPressed()
                invalidateKey(currentKeyIndex)
                postDelayed(performLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                if (key.isRepeatable) { sendKeyEvent(); postDelayed(performRepeatKey, REPEAT_KEY_START_DELAY) }
            }
            MotionEvent.ACTION_MOVE -> {
                removeAllCallbacks()
                if (currentKeyIndex == Keyboard.NOT_A_KEY) return true
                val key = kb.keys[currentKeyIndex]
                if (key.isPressed) {
                    if (key.isInside(touchX, touchY)) postDelayed(performLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                    else { isPressed = false; key.onReleased(false); invalidateKey(currentKeyIndex) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                abortKey = true; removeAllCallbacks()
                if (currentKeyIndex == Keyboard.NOT_A_KEY) return true
                val key = kb.keys[currentKeyIndex]
                if (key.isPressed) {
                    val inside = key.isInside(touchX, touchY)
                    isPressed = false; key.onReleased(inside); invalidateKey(currentKeyIndex)
                    if (!key.isRepeatable) sendKeyEvent()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun sendKeyEvent() {
        if (currentKeyIndex == Keyboard.NOT_A_KEY) return
        val kb = keyboard ?: return
        val key = kb.keys[currentKeyIndex]
        if (key.codes.isNotEmpty()) callbacks.forEach { it.onKey(key.codes[0]) }
    }

    private fun removeAllCallbacks() {
        removeCallbacks(performLongPress)
        removeCallbacks(performRepeatKey)
    }

    override fun onDetachedFromWindow() {
        removeAllCallbacks()
        buffer = null
        bitmapCanvas = null
        super.onDetachedFromWindow()
    }
}

// ── IME Service ──────────────────────────────────────────────────────────────

class GreyBIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolKeyboard: Keyboard

    override fun onCreate() {
        super.onCreate()
        qwertyKeyboard = Keyboard(this, R.xml.keyboard_qwerty)
        symbolKeyboard = Keyboard(this, R.xml.keyboard_symbol)
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.input, null) as KeyboardView
        keyboardView.setKeyboard(qwertyKeyboard)
        keyboardView.addCallback(keyboardActionListener)
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.setKeyboard(qwertyKeyboard)
        keyboardView.setShifted(info?.initialCapsMode != 0)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        keyboardView.removeCallback(keyboardActionListener)
        super.onDestroy()
    }

    private val keyboardActionListener = object : KeyboardView.KeyboardActionListener {
        override fun onKey(primaryCode: Int) {
            val ic = currentInputConnection ?: return
            when (primaryCode) {
                Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
                Keyboard.KEYCODE_SHIFT -> keyboardView.setShifted(!keyboardView.isShifted())
                Keyboard.KEYCODE_MODE_CHANGE -> {
                    keyboardView.setKeyboard(
                        if (keyboardView.keyboard === qwertyKeyboard) symbolKeyboard else qwertyKeyboard
                    )
                }
                Keyboard.KEYCODE_DONE, Keyboard.KEYCODE_ENTER -> {
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                }
                Keyboard.KEYCODE_CLOSE_KEYBOARD -> requestHideSelf(0)
                else -> {
                    var ch = primaryCode.toChar()
                    if (keyboardView.isShifted()) ch = ch.uppercaseChar()
                    ic.commitText(ch.toString(), 1)
                    if (keyboardView.isShifted()) keyboardView.setShifted(false)
                }
            }
        }
        override fun onStopInput() = requestHideSelf(0)
    }
}
