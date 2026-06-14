package com.greyb.keyboard

import android.content.*
import android.graphics.Typeface
import android.os.*
import android.view.*
import android.view.inputmethod.*
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.*
import android.inputmethodservice.InputMethodService
import android.text.InputType

class Keyboard : InputMethodService() {

    // ── Mode ────────────────────────────────────────────────────────────────
    enum class Mode { QWERTY, SYMBOLS, EMOJI, CLIPBOARD }
    private var mode = Mode.QWERTY

    // ── Shift ───────────────────────────────────────────────────────────────
    enum class ShiftState { OFF, ONCE, LOCKED }
    private var shiftState = ShiftState.OFF
    private var lastShiftTap = 0L

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var rootFrame: FrameLayout
    private lateinit var qwertyView: View
    private lateinit var symbolsView: View
    private lateinit var emojiView: View
    private lateinit var clipboardView: View
    private var toolsPopup: PopupWindow? = null

    // ── Material Symbols font ────────────────────────────────────────────────
    private lateinit var msFont: Typeface

    // Icon codepoints (Material Symbols Outlined)
    private val ICON_CLIPBOARD  = "\uEA9E"   // content_paste
    private val ICON_TOOLS      = "\uE262"   // edit_note
    private val ICON_EMOJI      = "\uE76E"   // emoji_emotions
    private val ICON_SHIFT      = "\uE5D8"   // arrow_upward
    private val ICON_SHIFT_LOCK = "\uE5D7"   // keyboard_capslock
    private val ICON_BACKSPACE  = "\uE14A"   // backspace
    private val ICON_ENTER      = "\uE31B"   // keyboard_return

    // ── Clipboard ────────────────────────────────────────────────────────────
    private val clipboardItems = mutableListOf<String>()
    private val PREFS_NAME = "greyb_kb"
    private val PREFS_CLIP = "clipboard"
    private val MAX_CLIP = 10
    private lateinit var clipAdapter: ClipboardAdapter

    // ── Emoji data ───────────────────────────────────────────────────────────
    private val emojiCategories = mapOf(
        "faces"      to listOf("😀","😁","😂","🤣","😃","😄","😅","😆","😇","😈","😉","😊","😋","😌","😍","🥰","😎","😏","😐","😑","😒","😓","😔","😕","😖","😗","😘","😙","😚","😛","😜","😝","😞","😟","😠","😡","😢","😣","😤","😥","😦","😧","😨","😩","😪","😫","😬","😭","😮","😯","😰","😱","😲","😳","😴","😵","😶","😷","🙁","🙂","🙃","🙄"),
        "people"     to listOf("👋","🤚","🖐","✋","🖖","👌","🤌","🤏","✌","🤞","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝","👍","👎","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍","💅","🤳","💪","🦾","🦵","🦶","👂","🦻","👃","👶","🧒","👦","👧","🧑","👱","👨","🧔","👩","🧓","👴","👵","🙍","🙎","🙅","🙆","💁","🙋","🧏","🙇","🤦","🤷"),
        "animals"    to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍"),
        "food"       to listOf("🍕","🍔","🍟","🌭","🍿","🧂","🥓","🥚","🍳","🧇","🥞","🧈","🍞","🥐","🥖","🥨","🧀","🥗","🥙","🥪","🌮","🌯","🫔","🧆","🥜","🌰","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🍠","🍢","🍣","🍤","🍥","🥮","🍡","🥟","🦪","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯","🍷","🍸","🍹","🍺","🍻"),
        "activities" to listOf("⚽","🏀","🏈","⚾","🥎","🏐","🏉","🥏","🎾","🪃","🏸","🏒","🏑","🥍","🏏","🪅","🎯","🪀","🪁","🎱","🔮","🪄","🎮","🕹","🎲","🧩","🪆","🧸","🪅","🎭","🎨","🖼","🎰","🎟","🎪","🤹","🎬","🎤","🎧","🎼","🎹","🥁","🪘","🎷","🎺","🎸","🪕","🎻","🎳","🏹","🛹","🛼","🛷","⛸","🥌","🎿","⛷","🏂","🪂","🏋"),
        "objects"    to listOf("💡","🔦","🕯","🪔","🧯","🛢","💰","💳","💎","⚖","🔧","🔨","⚒","🛠","⛏","🔩","🪛","🔫","🪃","🗡","⚔","🛡","🪚","🔪","🗜","🔗","⛓","🪝","🧲","🪜","🧰","🪤","🧲","🪣","🚪","🛏","🛋","🪑","🚽","🚿","🛁","🪠","🧴","🧷","🧹","🧺","🧻","🪣","🧼","🫧","🪥","🧽","🪒","🧴","🔑","🗝","🔐","🔏","🔒","🔓","🪪")
    )
    private var currentEmojiCategory = "faces"

    // ── Backspace repeat ─────────────────────────────────────────────────────
    private val bsHandler = Handler(Looper.getMainLooper())
    private var bsRunnable: Runnable? = null

    // ════════════════════════════════════════════════════════════════════════
    override fun onCreate() {
        super.onCreate()
        msFont = Typeface.createFromAsset(assets, "MaterialSymbols-Outlined.ttf")
        loadClipboard()
    }

    override fun onCreateInputView(): View {
        rootFrame = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        qwertyView   = buildQwerty()
        symbolsView  = buildSymbols()
        emojiView    = buildEmoji()
        clipboardView = buildClipboard()

        showMode(Mode.QWERTY)
        return rootFrame
    }

    // ── Mode switching ───────────────────────────────────────────────────────
    private fun showMode(m: Mode) {
        mode = m
        rootFrame.removeAllViews()
        val v = when (m) {
            Mode.QWERTY    -> qwertyView
            Mode.SYMBOLS   -> symbolsView
            Mode.EMOJI     -> emojiView
            Mode.CLIPBOARD -> clipboardView
        }
        rootFrame.addView(v)
    }

    // ════════════════════════════════════════════════════════════════════════
    // QWERTY
    // ════════════════════════════════════════════════════════════════════════
    private fun buildQwerty(): View {
        val v = layoutInflater.inflate(R.layout.qwerty, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)
        applyMsIcon(v, R.id.btn_emoji,     ICON_EMOJI)
        applyMsIcon(v, R.id.btn_backspace, ICON_BACKSPACE)
        updateShiftIcon(v)
        updateEnterIcon(v)

        // letter keys
        val letters = "qwertyuiopasdfghjklzxcvbnm"
        val ids = listOf(
            R.id.kq,R.id.kw,R.id.ke,R.id.kr,R.id.kt,R.id.ky,R.id.ku,R.id.ki,R.id.ko,R.id.kp,
            R.id.ka,R.id.ks,R.id.kd,R.id.kf,R.id.kg,R.id.kh,R.id.kj,R.id.kk,R.id.kl,
            R.id.kz,R.id.kx,R.id.kc,R.id.kv,R.id.kb,R.id.kn,R.id.km
        )
        ids.forEachIndexed { i, id ->
            v.findViewById<TextView>(id).setOnClickListener {
                typeChar(letters[i])
            }
        }

        // number row
        "1234567890".forEachIndexed { i, c ->
            val numIds = listOf(R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,
                                R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.k0)
            v.findViewById<TextView>(numIds[i]).setOnClickListener { typeText(c.toString()) }
        }

        v.findViewById<TextView>(R.id.btn_shift).setOnClickListener   { handleShift(v) }
        v.findViewById<TextView>(R.id.btn_backspace).apply {
            setOnClickListener      { doBackspace() }
            setOnLongClickListener  { startBackspaceRepeat(); true }
            setOnTouchListener      { _, e -> if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) stopBackspaceRepeat(); false }
        }
        v.findViewById<TextView>(R.id.btn_space).setOnClickListener   { typeText(" ") }
        v.findViewById<TextView>(R.id.btn_comma).setOnClickListener   { typeText(",") }
        v.findViewById<TextView>(R.id.btn_period).setOnClickListener  { typeText(".") }
        v.findViewById<TextView>(R.id.btn_enter).setOnClickListener   { doEnter() }
        v.findViewById<TextView>(R.id.btn_symbols).setOnClickListener { showMode(Mode.SYMBOLS) }
        v.findViewById<TextView>(R.id.btn_emoji).setOnClickListener   { showMode(Mode.EMOJI) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener   { showTools(v.findViewById(R.id.btn_tools)) }

        return v
    }

    // ════════════════════════════════════════════════════════════════════════
    // SYMBOLS
    // ════════════════════════════════════════════════════════════════════════
    private fun buildSymbols(): View {
        val v = layoutInflater.inflate(R.layout.symbols, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)
        applyMsIcon(v, R.id.btn_emoji,     ICON_EMOJI)
        applyMsIcon(v, R.id.btn_backspace, ICON_BACKSPACE)

        val syms = listOf(
            "!","@","#","$","%","^","&","*","(",")",
            "-","+","=","/",":",";","\"","'","?","!",
            "[","]","{","}","<",">","|","\\","~"
        )
        val symIds = listOf(
            R.id.ks1,R.id.ks2,R.id.ks3,R.id.ks4,R.id.ks5,
            R.id.ks6,R.id.ks7,R.id.ks8,R.id.ks9,R.id.ks10,
            R.id.ks11,R.id.ks12,R.id.ks13,R.id.ks14,R.id.ks15,
            R.id.ks16,R.id.ks17,R.id.ks18,R.id.ks19,R.id.ks20,
            R.id.ks21,R.id.ks22,R.id.ks23,R.id.ks24,R.id.ks25,
            R.id.ks26,R.id.ks27,R.id.ks28,R.id.ks29
        )
        symIds.forEachIndexed { i, id ->
            v.findViewById<TextView>(id).setOnClickListener { typeText(syms[i]) }
        }

        "1234567890".forEachIndexed { i, c ->
            val numIds = listOf(R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,
                                R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.k0)
            v.findViewById<TextView>(numIds[i]).setOnClickListener { typeText(c.toString()) }
        }

        v.findViewById<TextView>(R.id.btn_backspace).apply {
            setOnClickListener     { doBackspace() }
            setOnLongClickListener { startBackspaceRepeat(); true }
            setOnTouchListener     { _, e -> if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) stopBackspaceRepeat(); false }
        }
        v.findViewById<TextView>(R.id.btn_space).setOnClickListener  { typeText(" ") }
        v.findViewById<TextView>(R.id.btn_comma).setOnClickListener  { typeText(",") }
        v.findViewById<TextView>(R.id.btn_period).setOnClickListener { typeText(".") }
        v.findViewById<TextView>(R.id.btn_enter).setOnClickListener  { doEnter() }
        v.findViewById<TextView>(R.id.btn_abc).setOnClickListener    { showMode(Mode.QWERTY) }
        v.findViewById<TextView>(R.id.btn_emoji).setOnClickListener  { showMode(Mode.EMOJI) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener  { showTools(v.findViewById(R.id.btn_tools)) }

        return v
    }

    // ════════════════════════════════════════════════════════════════════════
    // EMOJI
    // ════════════════════════════════════════════════════════════════════════
    private fun buildEmoji(): View {
        val v = layoutInflater.inflate(R.layout.emoji, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)

        val grid = v.findViewById<GridLayout>(R.id.emoji_grid)
        loadEmojiGrid(grid, currentEmojiCategory)

        val tabs = mapOf(
            R.id.tab_faces      to "faces",
            R.id.tab_people     to "people",
            R.id.tab_animals    to "animals",
            R.id.tab_food       to "food",
            R.id.tab_activities to "activities",
            R.id.tab_objects    to "objects"
        )
        tabs.forEach { (id, cat) ->
            v.findViewById<TextView>(id).setOnClickListener {
                currentEmojiCategory = cat
                loadEmojiGrid(grid, cat)
            }
        }

        v.findViewById<TextView>(R.id.btn_abc).setOnClickListener       { showMode(Mode.QWERTY) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener     { showTools(v.findViewById(R.id.btn_tools)) }

        return v
    }

    private fun loadEmojiGrid(grid: GridLayout, category: String) {
        grid.removeAllViews()
        val emojis = emojiCategories[category] ?: return
        val cellSize = (resources.displayMetrics.widthPixels - 32.dp) / 8
        emojis.forEach { emoji ->
            val tv = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width  = cellSize
                    height = cellSize
                }
                setOnClickListener { typeText(emoji) }
            }
            grid.addView(tv)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CLIPBOARD
    // ════════════════════════════════════════════════════════════════════════
    private fun buildClipboard(): View {
        val v = layoutInflater.inflate(R.layout.clipboard, null)

        applyMsIcon(v, R.id.btn_clipboard_active, ICON_CLIPBOARD)

        clipAdapter = ClipboardAdapter(
            clipboardItems,
            onPaste = { item ->
                typeText(item)
                showMode(Mode.QWERTY)
            },
            onDelete = { pos ->
                clipboardItems.removeAt(pos)
                clipAdapter.notifyItemRemoved(pos)
                saveClipboard()
            }
        )

        v.findViewById<RecyclerView>(R.id.clipboard_list).apply {
            layoutManager = LinearLayoutManager(this@Keyboard)
            adapter = clipAdapter
        }

        v.findViewById<TextView>(R.id.btn_clipboard_close).setOnClickListener {
            showMode(Mode.QWERTY)
        }

        return v
    }

    private fun openClipboard() {
        // capture current selection into clipboard
        val ic = currentInputConnection ?: return showMode(Mode.CLIPBOARD)
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) addToClipboard(sel.toString())
        showMode(Mode.CLIPBOARD)
    }

    fun addToClipboard(text: String) {
        clipboardItems.remove(text)
        clipboardItems.add(0, text)
        if (clipboardItems.size > MAX_CLIP) clipboardItems.removeAt(clipboardItems.size - 1)
        saveClipboard()
        if (::clipAdapter.isInitialized) clipAdapter.notifyDataSetChanged()
    }

    private fun saveClipboard() {
        val joined = clipboardItems.take(MAX_CLIP).joinToString("\u001F")
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREFS_CLIP, joined).apply()
    }

    private fun loadClipboard() {
        val joined = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_CLIP, "") ?: ""
        clipboardItems.clear()
        if (joined.isNotEmpty()) clipboardItems.addAll(joined.split("\u001F"))
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOOLS DROP-UP
    // ════════════════════════════════════════════════════════════════════════
    private fun showTools(anchor: View) {
        toolsPopup?.dismiss()

        val popup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF2C2C2C.toInt())
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }

        listOf("Select All", "Copy", "Paste").forEach { label ->
            val item = TextView(this).apply {
                text  = label
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16.dp, 12.dp, 16.dp, 12.dp)
                setOnClickListener {
                    toolsPopup?.dismiss()
                    when (label) {
                        "Select All" -> currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                        "Copy"       -> currentInputConnection?.performContextMenuAction(android.R.id.copy)
                        "Paste"      -> {
                            val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .primaryClip?.getItemAt(0)?.coerceToText(this@Keyboard)?.toString()
                            if (!clip.isNullOrEmpty()) {
                                typeText(clip)
                                addToClipboard(clip)
                            }
                        }
                    }
                }
            }
            popup.addView(item)
        }

        toolsPopup = PopupWindow(popup,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 8f
            showAsDropDown(anchor, 0, -anchor.height * 4)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // INPUT HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private fun typeChar(c: Char) {
        val out = when (shiftState) {
            ShiftState.OFF    -> c.lowercaseChar()
            ShiftState.ONCE   -> c.uppercaseChar()
            ShiftState.LOCKED -> c.uppercaseChar()
        }
        typeText(out.toString())
        if (shiftState == ShiftState.ONCE) {
            shiftState = ShiftState.OFF
            updateShiftIcon(qwertyView)
        }
    }

    private fun typeText(s: String) {
        currentInputConnection?.commitText(s, 1)
    }

    private fun doBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun startBackspaceRepeat() {
        bsRunnable = object : Runnable {
            override fun run() {
                doBackspace()
                bsHandler.postDelayed(this, 50)
            }
        }
        bsHandler.postDelayed(bsRunnable!!, 400)
    }

    private fun stopBackspaceRepeat() {
        bsRunnable?.let { bsHandler.removeCallbacks(it) }
        bsRunnable = null
    }

    private fun doEnter() {
        val ei = currentInputEditorInfo ?: run {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER))
            return
        }
        val action = ei.imeOptions and EditorInfo.IME_MASK_ACTION
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER))
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SHIFT
    // ════════════════════════════════════════════════════════════════════════
    private fun handleShift(v: View) {
        val now = System.currentTimeMillis()
        shiftState = when {
            shiftState == ShiftState.LOCKED               -> ShiftState.OFF
            now - lastShiftTap < 200                      -> ShiftState.LOCKED
            shiftState == ShiftState.OFF                  -> ShiftState.ONCE
            else                                          -> ShiftState.OFF
        }
        lastShiftTap = now
        updateShiftIcon(v)
        updateLetterKeys(v)
    }

    private fun updateShiftIcon(v: View) {
        val tv = v.findViewById<TextView>(R.id.btn_shift) ?: return
        tv.typeface = msFont
        tv.text = when (shiftState) {
            ShiftState.OFF    -> ICON_SHIFT
            ShiftState.ONCE   -> ICON_SHIFT
            ShiftState.LOCKED -> ICON_SHIFT_LOCK
        }
        tv.setTextColor(when (shiftState) {
            ShiftState.OFF    -> 0xFFFFFFFF.toInt()
            ShiftState.ONCE   -> 0xFF4FC3F7.toInt()
            ShiftState.LOCKED -> 0xFF4FC3F7.toInt()
        })
    }

    private fun updateLetterKeys(v: View) {
        val upper = shiftState != ShiftState.OFF
        val letters = "qwertyuiopasdfghjklzxcvbnm"
        val ids = listOf(
            R.id.kq,R.id.kw,R.id.ke,R.id.kr,R.id.kt,R.id.ky,R.id.ku,R.id.ki,R.id.ko,R.id.kp,
            R.id.ka,R.id.ks,R.id.kd,R.id.kf,R.id.kg,R.id.kh,R.id.kj,R.id.kk,R.id.kl,
            R.id.kz,R.id.kx,R.id.kc,R.id.kv,R.id.kb,R.id.kn,R.id.km
        )
        ids.forEachIndexed { i, id ->
            v.findViewById<TextView>(id)?.text =
                if (upper) letters[i].uppercaseChar().toString()
                else       letters[i].toString()
        }
    }

    private fun updateEnterIcon(v: View) {
        val tv = v.findViewById<TextView>(R.id.btn_enter) ?: return
        tv.typeface = msFont
        tv.text = ICON_ENTER
    }

    // ── onStartInputView: update enter key per field ─────────────────────────
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (mode == Mode.QWERTY || mode == Mode.SYMBOLS) {
            val v = if (mode == Mode.QWERTY) qwertyView else symbolsView
            val tv = v.findViewById<TextView>(R.id.btn_enter) ?: return
            tv.typeface = msFont
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            tv.text = when (action) {
                EditorInfo.IME_ACTION_SEARCH -> "\uE8B6"  // search icon
                EditorInfo.IME_ACTION_SEND   -> "\uE163"  // send icon
                EditorInfo.IME_ACTION_GO     -> "\uE5C8"  // arrow_forward
                EditorInfo.IME_ACTION_DONE   -> "\uE876"  // check
                else                         -> ICON_ENTER
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════
    private fun applyMsIcon(root: View, id: Int, icon: String) {
        root.findViewById<TextView>(id)?.apply {
            typeface = msFont
            text = icon
        }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        stopBackspaceRepeat()
        toolsPopup?.dismiss()
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CLIPBOARD ADAPTER
// ════════════════════════════════════════════════════════════════════════════
class ClipboardAdapter(
    private val items: MutableList<String>,
    private val onPaste: (String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.VH>() {

    inner class VH(val root: LinearLayout, val text: TextView, val del: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 10, 12, 10)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val text = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val del = TextView(ctx).apply {
            text = "✕"
            setTextColor(0xFF888888.toInt())
            textSize = 16f
            setPadding(16, 0, 4, 0)
        }
        root.addView(text)
        root.addView(del)
        return VH(root, text, del)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        h.text.text = items[pos]
        h.root.setOnClickListener { onPaste(items[pos]) }
        h.del.setOnClickListener  { onDelete(h.adapterPosition) }
    }

    override fun getItemCount() = items.size
}
