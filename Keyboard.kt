package com.greyb.keyboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GreyBIME : LifecycleOwnerIME() {

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
    }

    @Composable
    override fun KeyboardContent() {
        GreyBTheme {
            GreyBKeyboard(onKeyPress = { key -> handleKeyPress(key) })
        }
    }

    private fun handleKeyPress(key: KeyDef) {
        val ic = currentInputConnection ?: return
        when (key) {
            is KeyDef.CharKey -> ic.commitText(key.char.toString(), 1)
            is KeyDef.ActionKey -> when (key.action) {
                Action.DELETE -> ic.deleteSurroundingText(1, 0)
                Action.ENTER -> {
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                }
                Action.SPACE -> ic.commitText(" ", 1)
                Action.SHIFT -> {}
                Action.SYMBOLS -> {}
                Action.EMOJI -> {}
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

enum class Action { DELETE, ENTER, SPACE, SHIFT, SYMBOLS, EMOJI }

@Composable
fun GreyBKeyboard(onKeyPress: (KeyDef) -> Unit) {
    var isShifted by remember { mutableStateOf(false) }

    val qwertyRow = listOf('q','w','e','r','t','y','u','i','o','p').map { KeyDef.CharKey(it) }
    val asdfRow   = listOf('a','s','d','f','g','h','j','k','l').map { KeyDef.CharKey(it) }
    val zxcvRow   = listOf('z','x','c','v','b','n','m').map { KeyDef.CharKey(it) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val keyBgColor   = MaterialTheme.colorScheme.surfaceVariant
    val keyTextColor = MaterialTheme.colorScheme.onSurface
    val accentColor  = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth().background(surfaceColor).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf('1','2','3','4','5','6','7','8','9','0').forEach { num ->
                KeyButton(KeyDef.CharKey(num), onKeyPress, Modifier.weight(1f), keyBgColor, keyTextColor)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            qwertyRow.forEach { KeyButton(it, onKeyPress, Modifier.weight(1f), keyBgColor, keyTextColor, isShifted) }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            asdfRow.forEach { KeyButton(it, onKeyPress, Modifier.weight(1f), keyBgColor, keyTextColor, isShifted) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton(
                key = KeyDef.ActionKey(Action.SHIFT, "⇧", Icons.Default.KeyboardCapslock),
                onClick = { isShifted = !isShifted; onKeyPress(KeyDef.ActionKey(Action.SHIFT, "⇧")) },
                modifier = Modifier.weight(1.4f),
                bgColor = if (isShifted) accentColor else keyBgColor,
                textColor = keyTextColor,
                showIcon = true
            )
            zxcvRow.forEach { KeyButton(it, onKeyPress, Modifier.weight(1f), keyBgColor, keyTextColor, isShifted) }
            KeyButton(
                key = KeyDef.ActionKey(Action.DELETE, "⌫", Icons.AutoMirrored.Filled.KeyboardArrowLeft),
                onClick = { onKeyPress(KeyDef.ActionKey(Action.DELETE, "⌫")) },
                modifier = Modifier.weight(1.4f),
                bgColor = keyBgColor,
                textColor = keyTextColor,
                showIcon = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton(KeyDef.ActionKey(Action.SYMBOLS, "?123"), { onKeyPress(it) }, Modifier.weight(1.3f), keyBgColor, accentColor)
            KeyButton(KeyDef.ActionKey(Action.EMOJI, "😊", Icons.Default.EmojiEmotions), { onKeyPress(it) }, Modifier.weight(1f), keyBgColor, keyTextColor, showIcon = true)
            KeyButton(KeyDef.ActionKey(Action.SPACE, "space"), { onKeyPress(it) }, Modifier.weight(4f), keyBgColor, keyTextColor, label = "")
            KeyButton(KeyDef.CharKey('.'), onKeyPress, Modifier.weight(1f), keyBgColor, keyTextColor)
            KeyButton(KeyDef.ActionKey(Action.ENTER, "⏎", Icons.AutoMirrored.Filled.Send), { onKeyPress(it) }, Modifier.weight(1.3f), accentColor, surfaceColor, showIcon = true)
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick(key) },
        contentAlignment = Alignment.Center
    ) {
        if (showIcon && icon != null) {
            Icon(icon, contentDescription = displayText, tint = textColor, modifier = Modifier.size(20.dp))
        } else {
            Text(displayText, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}
