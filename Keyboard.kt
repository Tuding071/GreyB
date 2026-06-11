// Keyboard.kt
package com.greyb.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class GreyBIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var kv: KeyboardView
    private lateinit var qwerty: Keyboard

    override fun onCreate() {
        super.onCreate()
        qwerty = Keyboard(this, R.xml.qwerty)
    }

    override fun onCreateInputView(): View {
        kv = KeyboardView(this, null)
        kv.keyboard = qwerty
        kv.isPreviewEnabled = false
        kv.setOnKeyboardActionListener(this)
        kv.setBackgroundColor(0xFF2A2D34.toInt())
        return kv
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        kv.keyboard = qwerty
    }

    override fun onEvaluateFullscreenMode() = false

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
            Keyboard.KEYCODE_SHIFT -> {
                kv.isShifted = !kv.isShifted
            }
            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                var code = primaryCode
                if (kv.isShifted) code = Character.toUpperCase(code)
                ic.commitText(code.toChar().toString(), 1)
                if (kv.isShifted) kv.isShifted = false
            }
        }
    }

    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
}
