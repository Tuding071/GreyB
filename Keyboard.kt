package com.greyb.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ─── Keyboard Data Models ─────────────────────────────────────────────────

enum class KeyType {
    REGULAR, MODIFIER, STICKY, REPEATABLE, ACTION, SPACE
}

data class KeyData(
    val code: Int,
    val label: String,
    val width: Float,
    val keyType: KeyType = KeyType.REGULAR,
    val edgeFlags: Int = 0
)

data class KeyboardRowData(
    val keys: List<KeyData>
)

data class KeyboardLayoutData(
    val rows: List<KeyboardRowData>
)

// ─── Predefined Layouts ───────────────────────────────────────────────────

val qwertyLayout = KeyboardLayoutData(
    rows = listOf(
        KeyboardRowData(listOf(
            KeyData(49, "1", 10f, edgeFlags = 4),
            KeyData(50, "2", 10f),
            KeyData(51, "3", 10f),
            KeyData(52, "4", 10f),
            KeyData(53, "5", 10f),
            KeyData(54, "6", 10f),
            KeyData(55, "7", 10f),
            KeyData(56, "8", 10f),
            KeyData(57, "9", 10f),
            KeyData(48, "0", 10f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(113, "q", 10f, edgeFlags = 1),
            KeyData(119, "w", 10f),
            KeyData(101, "e", 10f),
            KeyData(114, "r", 10f),
            KeyData(116, "t", 10f),
            KeyData(121, "y", 10f),
            KeyData(117, "u", 10f),
            KeyData(105, "i", 10f),
            KeyData(111, "o", 10f),
            KeyData(112, "p", 10f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(97, "a", 11f, edgeFlags = 1),
            KeyData(115, "s", 10f),
            KeyData(100, "d", 10f),
            KeyData(102, "f", 10f),
            KeyData(103, "g", 10f),
            KeyData(104, "h", 10f),
            KeyData(106, "j", 10f),
            KeyData(107, "k", 10f),
            KeyData(108, "l", 11f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(-1, "⇧", 14f, KeyType.STICKY, edgeFlags = 1),
            KeyData(122, "z", 10f),
            KeyData(120, "x", 10f),
            KeyData(99, "c", 10f),
            KeyData(118, "v", 10f),
            KeyData(98, "b", 10f),
            KeyData(110, "n", 10f),
            KeyData(109, "m", 10f),
            KeyData(-5, "⌫", 14f, KeyType.REPEATABLE, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(-2, "?123", 14f, KeyType.ACTION, edgeFlags = 1),
            KeyData(44, ",", 10f),
            KeyData(32, "", 42f, KeyType.SPACE),
            KeyData(46, ".", 10f),
            KeyData(-4, "⏎", 14f, KeyType.ACTION, edgeFlags = 2)
        ))
    )
)

val symbolLayout = KeyboardLayoutData(
    rows = listOf(
        KeyboardRowData(listOf(
            KeyData(49, "1", 10f, edgeFlags = 4),
            KeyData(50, "2", 10f),
            KeyData(51, "3", 10f),
            KeyData(52, "4", 10f),
            KeyData(53, "5", 10f),
            KeyData(54, "6", 10f),
            KeyData(55, "7", 10f),
            KeyData(56, "8", 10f),
            KeyData(57, "9", 10f),
            KeyData(48, "0", 10f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(64, "@", 10f, edgeFlags = 1),
            KeyData(35, "#", 10f),
            KeyData(36, "$", 10f),
            KeyData(37, "%", 10f),
            KeyData(38, "&", 10f),
            KeyData(45, "-", 10f),
            KeyData(43, "+", 10f),
            KeyData(40, "(", 10f),
            KeyData(41, ")", 10f),
            KeyData(47, "/", 10f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(33, "!", 11f, edgeFlags = 1),
            KeyData(34, "\"", 10f),
            KeyData(39, "'", 10f),
            KeyData(58, ":", 10f),
            KeyData(59, ";", 10f),
            KeyData(47, "/", 10f),
            KeyData(63, "?", 10f),
            KeyData(126, "~", 10f),
            KeyData(96, "`", 11f, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(-8, "ABC", 14f, KeyType.ACTION, edgeFlags = 1),
            KeyData(92, "\\", 10f),
            KeyData(124, "|", 10f),
            KeyData(60, "<", 10f),
            KeyData(62, ">", 10f),
            KeyData(123, "{", 10f),
            KeyData(125, "}", 10f),
            KeyData(-5, "⌫", 14f, KeyType.REPEATABLE, edgeFlags = 2)
        )),
        KeyboardRowData(listOf(
            KeyData(-2, "?123", 14f, KeyType.ACTION, edgeFlags = 1),
            KeyData(44, ",", 10f),
            KeyData(32, "", 42f, KeyType.SPACE),
            KeyData(46, ".", 10f),
            KeyData(-4, "⏎", 14f, KeyType.ACTION, edgeFlags = 2)
        ))
    )
)

// ─── ViewModel ──────────────────────────────────────────────────────────────

class KeyboardViewModel : ViewModel() {
    private val _currentLayout = mutableStateOf(qwertyLayout)
    val currentLayout: KeyboardLayoutData by _currentLayout

    private val _isShifted = mutableStateOf(false)
    val isShifted: Boolean by _isShifted

    private val _pressedKeys = mutableStateMapOf<Int, Boolean>()
    val pressedKeys: Map<Int, Boolean> = _pressedKeys

    var inputConnection: InputConnection? = null

    fun onKeyDown(key: KeyData) {
        _pressedKeys[key.hashCode()] = true
    }

    fun onKeyUp(key: KeyData) {
        _pressedKeys[key.hashCode()] = false
        handleKeyAction(key)
    }

    fun onKeyCancel(key: KeyData) {
        _pressedKeys[key.hashCode()] = false
    }

    private fun handleKeyAction(key: KeyData) {
        val ic = inputConnection ?: return
        when (key.code) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -1 -> _isShifted.value = !_isShifted.value
            -2 -> _currentLayout.value = if (_currentLayout.value == qwertyLayout) symbolLayout else qwertyLayout
            -4 -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            -8 -> _currentLayout.value = qwertyLayout
            -99 -> {} // Close keyboard handled by system
            else -> {
                var ch = key.code.toChar()
                if (_isShifted.value && ch.isLowerCase()) {
                    ch = ch.uppercaseChar()
                }
                ic.commitText(ch.toString(), 1)
                if (_isShifted.value) {
                    _isShifted.value = false
                }
            }
        }
    }

    fun getDisplayLabel(key: KeyData): String {
        if (key.code == 32) return ""
        if (key.code < 0) return key.label
        var ch = key.code.toChar()
        if (_isShifted.value && ch.isLowerCase()) {
            ch = ch.uppercaseChar()
        }
        return if (ch.isLetter()) ch.toString() else key.label
    }
}

// ─── Composable Keyboard ───────────────────────────────────────────────────

@androidx.compose.runtime.Composable
fun KeyboardScreen(viewModel: KeyboardViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x801A1A1A))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        viewModel.currentLayout.rows.forEach { row ->
            KeyboardRow(row, viewModel)
        }
    }
}

@androidx.compose.runtime.Composable
fun KeyboardRow(row: KeyboardRowData, viewModel: KeyboardViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        row.keys.forEach { key ->
            KeyboardKey(key, viewModel)
        }
    }
}

@androidx.compose.runtime.Composable
fun KeyboardKey(key: KeyData, viewModel: KeyboardViewModel) {
    val isPressed = viewModel.pressedKeys[key.hashCode()] ?: false
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    viewModel.onKeyDown(key)
                    if (key.keyType == KeyType.REPEATABLE) {
                        viewModel.onKeyUp(key)
                        while (isActive) {
                            delay(50)
                            viewModel.onKeyUp(key)
                        }
                    }
                }
                is PressInteraction.Release -> {
                    if (key.keyType != KeyType.REPEATABLE) {
                        viewModel.onKeyUp(key)
                    }
                }
                is PressInteraction.Cancel -> {
                    viewModel.onKeyCancel(key)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width((key.width * 3.6).dp)
            .height(48.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    key.keyType == KeyType.STICKY && viewModel.isShifted -> Color(0x80B4B4AA)
                    isPressed -> Color(0x80B4B4AA)
                    else -> Color(0x802A2A2A)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = viewModel.getDisplayLabel(key),
            color = Color(0x80DCDCD7),
            fontSize = if (key.code < 0 && key.code != 32) 14.sp else 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─── IME Service ────────────────────────────────────────────────────────────

class GreyBIME : InputMethodService(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private lateinit var keyboardViewModel: KeyboardViewModel

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        return ComposeView(this).apply {
            setContent {
                keyboardViewModel = viewModel()
                KeyboardScreen(keyboardViewModel)
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardViewModel.inputConnection = currentInputConnection
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
