package com.greyb.keyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Key model ────────────────────────────────────────────────────────────────

enum class KeyType { CHAR, SHIFT, DELETE, ENTER, SPACE, SYMBOLS, BACK_TO_ALPHA }

data class KeyDef(
    val label: String,
    val code: Int = 0,
    val type: KeyType = KeyType.CHAR,
    val widthWeight: Float = 1f
)

// ── Layouts ──────────────────────────────────────────────────────────────────

object QwertyLayout {
    val rows: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("1", 49), KeyDef("2", 50), KeyDef("3", 51),
            KeyDef("4", 52), KeyDef("5", 53), KeyDef("6", 54),
            KeyDef("7", 55), KeyDef("8", 56), KeyDef("9", 57),
            KeyDef("0", 48)
        ),
        listOf(
            KeyDef("q", 113), KeyDef("w", 119), KeyDef("e", 101),
            KeyDef("r", 114), KeyDef("t", 116), KeyDef("y", 121),
            KeyDef("u", 117), KeyDef("i", 105), KeyDef("o", 111),
            KeyDef("p", 112)
        ),
        listOf(
            KeyDef("a", 97),  KeyDef("s", 115), KeyDef("d", 100),
            KeyDef("f", 102), KeyDef("g", 103), KeyDef("h", 104),
            KeyDef("j", 106), KeyDef("k", 107), KeyDef("l", 108)
        ),
        listOf(
            KeyDef("⇧", type = KeyType.SHIFT, widthWeight = 1.4f),
            KeyDef("z", 122), KeyDef("x", 120), KeyDef("c", 99),
            KeyDef("v", 118), KeyDef("b", 98),  KeyDef("n", 110),
            KeyDef("m", 109),
            KeyDef("⌫", type = KeyType.DELETE, widthWeight = 1.4f)
        ),
        listOf(
            KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.4f),
            KeyDef(",", 44),
            KeyDef("", type = KeyType.SPACE, widthWeight = 4f),
            KeyDef(".", 46),
            KeyDef("⏎", type = KeyType.ENTER, widthWeight = 1.4f)
        )
    )
}

object SymbolLayout {
    val rows: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("1", 49), KeyDef("2", 50), KeyDef("3", 51),
            KeyDef("4", 52), KeyDef("5", 53), KeyDef("6", 54),
            KeyDef("7", 55), KeyDef("8", 56), KeyDef("9", 57),
            KeyDef("0", 48)
        ),
        listOf(
            KeyDef("@", 64),  KeyDef("#", 35), KeyDef("$", 36),
            KeyDef("%", 37),  KeyDef("&", 38), KeyDef("-", 45),
            KeyDef("+", 43),  KeyDef("(", 40), KeyDef(")", 41),
            KeyDef("/", 47)
        ),
        listOf(
            KeyDef("!", 33),  KeyDef("\"", 34), KeyDef("'", 39),
            KeyDef(":", 58),  KeyDef(";", 59),  KeyDef("/", 47),
            KeyDef("?", 63),  KeyDef("~", 126), KeyDef("`", 96)
        ),
        listOf(
            KeyDef("ABC", type = KeyType.BACK_TO_ALPHA, widthWeight = 1.4f),
            KeyDef("\\", 92), KeyDef("|", 124), KeyDef("<", 60),
            KeyDef(">", 62),  KeyDef("{", 123), KeyDef("}", 125),
            KeyDef("⌫", type = KeyType.DELETE, widthWeight = 1.4f)
        ),
        listOf(
            KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.4f),
            KeyDef(",", 44),
            KeyDef("", type = KeyType.SPACE, widthWeight = 4f),
            KeyDef(".", 46),
            KeyDef("⏎", type = KeyType.ENTER, widthWeight = 1.4f)
        )
    )
}

// ── Colors ───────────────────────────────────────────────────────────────────

private val BG       = Color(0xFF1A1A1A)
private val KEY_BG   = Color(0xFF2A2A2A)
private val KEY_TEXT = Color(0xFFDCDCD7)
private val ACCENT   = Color(0xFFB4B4AA)
private val ENTER_BG = Color(0xFF3A3A3A)

// ── Compose UI ───────────────────────────────────────────────────────────────

@Composable
fun KeyboardScreen(
    rows: List<List<KeyDef>>,
    isShifted: Boolean,
    onKey: (KeyDef) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 3.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            KeyRow(row = row, isShifted = isShifted, onKey = onKey)
        }
    }
}

@Composable
fun KeyRow(
    row: List<KeyDef>,
    isShifted: Boolean,
    onKey: (KeyDef) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        row.forEach { key ->
            KeyButton(
                key = key,
                isShifted = isShifted,
                onKey = onKey,
                modifier = Modifier.weight(key.widthWeight)
            )
        }
    }
}

@Composable
fun KeyButton(
    key: KeyDef,
    isShifted: Boolean,
    onKey: (KeyDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLabel = when {
        key.type == KeyType.CHAR && isShifted && key.label.length == 1 -> key.label.uppercase()
        key.type == KeyType.SPACE -> "space"
        else -> key.label
    }

    val bgColor = when (key.type) {
        KeyType.SHIFT -> if (isShifted) ACCENT else KEY_BG
        KeyType.ENTER -> ENTER_BG
        else          -> KEY_BG
    }

    val textColor = when (key.type) {
        KeyType.SYMBOLS, KeyType.BACK_TO_ALPHA -> ACCENT
        else -> KEY_TEXT
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onKey(key) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayLabel,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ── KeyboardView ──────────────────────────────────────────────────────────────

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface KeyboardActionListener {
        fun onKey(code: Int, type: KeyType)
    }

    private var listener: KeyboardActionListener? = null
    internal var isShifted = false
    private var isSymbol = false

    private val composeView = ComposeView(context)

    init {
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
        renderKeyboard()
    }

    fun setListener(l: KeyboardActionListener) { listener = l }

    fun setShifted(state: Boolean) {
        isShifted = state
        renderKeyboard()
    }

    private fun renderKeyboard() {
        composeView.setContent {
            var shifted by remember { mutableStateOf(isShifted) }
            var symbol by remember { mutableStateOf(isSymbol) }
            val currentRows = if (symbol) SymbolLayout.rows else QwertyLayout.rows

            KeyboardScreen(
                rows = currentRows,
                isShifted = shifted,
                onKey = { key ->
                    when (key.type) {
                        KeyType.SHIFT -> {
                            shifted = !shifted
                            isShifted = shifted
                        }
                        KeyType.SYMBOLS -> {
                            symbol = true
                            isSymbol = true
                        }
                        KeyType.BACK_TO_ALPHA -> {
                            symbol = false
                            isSymbol = false
                        }
                        else -> {
                            if (shifted && key.type == KeyType.CHAR) {
                                shifted = false
                                isShifted = false
                            }
                            listener?.onKey(key.code, key.type)
                        }
                    }
                }
            )
        }
    }
}

// ── IME Service ──────────────────────────────────────────────────────────────

class GreyBIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardView.setListener(object : KeyboardView.KeyboardActionListener {
            override fun onKey(code: Int, type: KeyType) {
                val ic = currentInputConnection ?: return
                when (type) {
                    KeyType.DELETE -> ic.deleteSurroundingText(1, 0)
                    KeyType.ENTER  -> {
                        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                    }
                    KeyType.SPACE -> ic.commitText(" ", 1)
                    KeyType.CHAR  -> {
                        val ch = if (keyboardView.isShifted) code.toChar().uppercaseChar() else code.toChar()
                        ic.commitText(ch.toString(), 1)
                    }
                    else -> {}
                }
            }
        })
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.setShifted(info?.initialCapsMode != 0)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false
}
