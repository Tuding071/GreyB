package com.greyb.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GreyBIME : InputMethodService() {

    private lateinit var composeView: ComposeView

    override fun onCreateInputView(): View {
        composeView = ComposeView(this).apply {
            setContent {
                GreyBTheme {
                    GreyBKeyboard(
                        onKeyPress = { key ->
                            handleKeyPress(key)
                        }
                    )
                }
            }
        }
        return composeView
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onComputeInsets(outInsets: InputMethodService.Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets != null) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composeView.requestLayout()
    }

    private fun handleKeyPress(key: KeyDef) {
        val ic = currentInputConnection ?: return
        when (key) {
            is KeyDef.CharKey -> {
                ic.commitText(key.char.toString(), 1)
            }
            is KeyDef.ActionKey -> {
                when (key.action) {
                    Action.DELETE -> ic.deleteSurroundingText(1, 0)
                    Action.ENTER -> {
                        ic.sendKeyEvent(
                            android.view.KeyEvent(
                                android.view.KeyEvent.ACTION_DOWN,
                                android.view.KeyEvent.KEYCODE_ENTER
                            )
                        )
                        ic.sendKeyEvent(
                            android.view.KeyEvent(
                                android.view.KeyEvent.ACTION_UP,
                                android.view.KeyEvent.KEYCODE_ENTER
                            )
                        )
                    }
                    Action.SPACE -> ic.commitText(" ", 1)
                    Action.SHIFT -> { /* handled in UI */ }
                    Action.SYMBOLS -> { /* switch to symbols */ }
                    Action.EMOJI -> { /* switch to emoji */ }
                }
            }
        }
    }
}

@Composable
fun GreyBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            surface = Color(0xFF1A1A1A),
            background = Color(0xFF1A1A1A),
            onSurface = Color(0xFFDCDCD7),
            primary = Color(0xFFB4B4AA),
            surfaceVariant = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFA0A0A0),
        ),
        content = content
    )
}

sealed class KeyDef {
    data class CharKey(val char: Char, val label: String = char.toString()) : KeyDef()
    data class ActionKey(val action: Action, val label: String, val icon: ImageVector? = null) : KeyDef()
}

enum class Action {
    DELETE, ENTER, SPACE, SHIFT, SYMBOLS, EMOJI
}

@Composable
fun GreyBKeyboard(
    onKeyPress: (KeyDef) -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }

    val qwertyRow = listOf(
        KeyDef.CharKey('q'), KeyDef.CharKey('w'), KeyDef.CharKey('e'),
        KeyDef.CharKey('r'), KeyDef.CharKey('t'), KeyDef.CharKey('y'),
        KeyDef.CharKey('u'), KeyDef.CharKey('i'), KeyDef.CharKey('o'),
        KeyDef.CharKey('p')
    )

    val asdfRow = listOf(
        KeyDef.CharKey('a'), KeyDef.CharKey('s'), KeyDef.CharKey('d'),
        KeyDef.CharKey('f'), KeyDef.CharKey('g'), KeyDef.CharKey('h'),
        KeyDef.CharKey('j'), KeyDef.CharKey('k'), KeyDef.CharKey('l')
    )

    val zxcvRow = listOf(
        KeyDef.CharKey('z'), KeyDef.CharKey('x'), KeyDef.CharKey('c'),
        KeyDef.CharKey('v'), KeyDef.CharKey('b'), KeyDef.CharKey('n'),
        KeyDef.CharKey('m')
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val keyBgColor = MaterialTheme.colorScheme.surfaceVariant
    val keyTextColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Number row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val numbers = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
            numbers.forEach { num ->
                KeyButton(
                    key = KeyDef.CharKey(num),
                    onClick = onKeyPress,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBgColor,
                    textColor = keyTextColor
                )
            }
        }

        // QWERTY row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            qwertyRow.forEach { key ->
                KeyButton(
                    key = key,
                    onClick = onKeyPress,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBgColor,
                    textColor = keyTextColor,
                    isShifted = isShifted
                )
            }
        }

        // ASDF row with side padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            asdfRow.forEach { key ->
                KeyButton(
                    key = key,
                    onClick = onKeyPress,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBgColor,
                    textColor = keyTextColor,
                    isShifted = isShifted
                )
            }
        }

        // ZXCV row with shift and backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Shift key
            KeyButton(
                key = KeyDef.ActionKey(Action.SHIFT, "⇧", Icons.Default.KeyboardCapslock),
                onClick = {
                    isShifted = !isShifted
                    onKeyPress(KeyDef.ActionKey(Action.SHIFT, "⇧"))
                },
                modifier = Modifier.weight(1.4f),
                bgColor = if (isShifted) accentColor else keyBgColor,
                textColor = keyTextColor,
                showIcon = true
            )

            zxcvRow.forEach { key ->
                KeyButton(
                    key = key,
                    onClick = onKeyPress,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBgColor,
                    textColor = keyTextColor,
                    isShifted = isShifted
                )
            }

            // Backspace key
            KeyButton(
                key = KeyDef.ActionKey(Action.DELETE, "⌫", Icons.AutoMirrored.Filled.KeyboardArrowLeft),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.DELETE, "⌫")) },
                modifier = Modifier.weight(1.4f),
                bgColor = keyBgColor,
                textColor = keyTextColor,
                showIcon = true
            )
        }

        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // ?123 key
            KeyButton(
                key = KeyDef.ActionKey(Action.SYMBOLS, "?123"),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.SYMBOLS, "?123")) },
                modifier = Modifier.weight(1.3f),
                bgColor = keyBgColor,
                textColor = accentColor
            )

            // Emoji key
            KeyButton(
                key = KeyDef.ActionKey(Action.EMOJI, "😊", Icons.Default.EmojiEmotions),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.EMOJI, "😊")) },
                modifier = Modifier.weight(1f),
                bgColor = keyBgColor,
                textColor = keyTextColor,
                showIcon = true
            )

            // Space key
            KeyButton(
                key = KeyDef.ActionKey(Action.SPACE, "space"),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.SPACE, "space")) },
                modifier = Modifier.weight(4f),
                bgColor = keyBgColor,
                textColor = keyTextColor,
                label = ""
            )

            // Period key
            KeyButton(
                key = KeyDef.CharKey('.'),
                onClick = onKeyPress,
                modifier = Modifier.weight(1f),
                bgColor = keyBgColor,
                textColor = keyTextColor
            )

            // Enter key
            KeyButton(
                key = KeyDef.ActionKey(Action.ENTER, "⏎", Icons.AutoMirrored.Filled.Send),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.ENTER, "⏎")) },
                modifier = Modifier.weight(1.3f),
                bgColor = accentColor,
                textColor = surfaceColor,
                showIcon = true
            )
        }
    }
}

@Composable
fun KeyButton(
    key: KeyDef,
    onClick: (KeyDef) -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = Color.Gray,
    textColor: Color = Color.White,
    isShifted: Boolean = false,
    showIcon: Boolean = false,
    label: String? = null
) {
    val displayText = when {
        label != null -> label
        key is KeyDef.CharKey && isShifted -> key.char.uppercase()
        key is KeyDef.CharKey -> key.char.toString()
        key is KeyDef.ActionKey -> key.label
        else -> ""
    }

    val icon = if (key is KeyDef.ActionKey) key.icon else null

    Box(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 1.5.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick(key) },
        contentAlignment = Alignment.Center
    ) {
        if (showIcon && icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = displayText,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = displayText,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
