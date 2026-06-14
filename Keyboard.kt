package com.greyb.keyboard

import android.content.*
import android.graphics.Typeface
import android.os.*
import android.view.*
import android.view.inputmethod.*
import android.widget.*
import androidx.recyclerview.widget.*
import android.inputmethodservice.InputMethodService

class Keyboard : InputMethodService() {

    enum class Mode { QWERTY, SYMBOLS, EMOJI, CLIPBOARD }
    private var mode = Mode.QWERTY

    enum class ShiftState { OFF, ONCE, LOCKED }
    private var shiftState = ShiftState.OFF
    private var lastShiftTap = 0L

    private lateinit var rootFrame: FrameLayout
    private lateinit var qwertyView: View
    private lateinit var symbolsView: View
    private lateinit var emojiView: View
    private lateinit var clipboardView: View
    private var toolsPopup: PopupWindow? = null

    private lateinit var msFont: Typeface

    private val ICON_CLIPBOARD  = "\uEA9E"
    private val ICON_TOOLS      = "\uE262"
    private val ICON_EMOJI      = "\uE76E"
    private val ICON_SHIFT      = "\uE5D8"
    private val ICON_SHIFT_LOCK = "\uE5D7"
    private val ICON_BACKSPACE  = "\uE14A"
    private val ICON_ENTER      = "\uE31B"

    private val clipboardItems = mutableListOf<String>()
    private val PREFS_NAME = "greyb_kb"
    private val PREFS_CLIP = "clipboard"
    private val MAX_CLIP = 10
    private lateinit var clipAdapter: ClipboardAdapter

    private val emojiCategories = mapOf(
        "faces"      to listOf("😀","😁","😂","🤣","😃","😄","😅","😆","😇","😉","😊","😋","😌","😍","🥰","😎","😏","😐","😑","😒","😓","😔","😕","😖","😗","😘","😙","😚","😛","😜","😝","😞","😟","😠","😡","😢","😣","😤","😥","😦","😧","😨","😩","😪","😫","😬","😭","😮","😯","😰","😱","😲","😳","😴","😵","😶","😷","🙁","🙂","🙃","🙄"),
        "people"     to listOf("👋","🤚","🖐","✋","🖖","👌","🤌","🤏","✌","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝","👍","👎","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍","💅","🤳","💪","🦾","🦵","🦶","👂","🦻","👃","👶","🧒","👦","👧","🧑","👱","👨","🧔","👩","🧓","👴","👵","🙍","🙎","🙅","🙆","💁","🙋","🧏","🙇","🤦","🤷"),
        "animals"    to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍"),
        "food"       to listOf("🍕","🍔","🍟","🌭","🍿","🧂","🥓","🥚","🍳","🧇","🥞","🧈","🍞","🥐","🥖","🥨","🧀","🥗","🥙","🥪","🌮","🌯","🧆","🥜","🌰","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🍠","🍢","🍣","🍤","🍥","🥮","🍡","🥟","🦪","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯","🍷","🍸","🍹","🍺","🍻"),
        "activities" to listOf("⚽","🏀","🏈","⚾","🥎","🏐","🏉","🥏","🎾","🪃","🏸","🏒","🏑","🥍","🏏","🎯","🪀","🪁","🎱","🔮","🪄","🎮","🕹","🎲","🧩","🧸","🎭","🎨","🖼","🎰","🎟","🎪","🤹","🎬","🎤","🎧","🎼","🎹","🥁","🪘","🎷","🎺","🎸","🪕","🎻","🎳","🏹","🛹","🛼","🛷","⛸","🥌","🎿","⛷","🏂","🪂","🏋","🤸","🤺","🏇","🧘"),
        "objects"    to listOf("💡","🔦","🕯","🪔","🧯","🛢","💰","💳","💎","⚖","🔧","🔨","⚒","🛠","⛏","🔩","🪛","🔗","⛓","🪝","🧲","🪜","🧰","🪤","🧲","🪣","🚪","🛏","🛋","🪑","🚽","🚿","🛁","🧴","🧷","🧹","🧺","🧻","🪣","🧼","🫧","🪥","🧽","🪒","🔑","🗝","🔐","🔏","🔒","🔓","🪪","📱","💻","🖥","🖨","🖱","🖲","💾","💿","📀","📷","📸")
    )
    private var currentEmojiCategory = "faces"

    private val bsHandler = Handler(Looper.getMainLooper())
    private var bsRunnable: Runnable? = null

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
        qwertyView    = buildQwerty()
        symbolsView   = buildSymbols()
        emojiView     = buildEmoji()
        clipboardView = buildClipboard()
        showMode(Mode.QWERTY)
        return rootFrame
    }

    private fun showMode(m: Mode) {
        mode = m
        rootFrame.removeAllViews()
        rootFrame.addView(when (m) {
            Mode.QWERTY    -> qwertyView
            Mode.SYMBOLS   -> symbolsView
            Mode.EMOJI     -> emojiView
            Mode.CLIPBOARD -> clipboardView
        })
    }

    // ── QWERTY ───────────────────────────────────────────────────────────────
    private fun buildQwerty(): View {
        val v = layoutInflater.inflate(R.layout.qwerty, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)
        applyMsIcon(v, R.id.btn_emoji,     ICON_EMOJI)
        applyMsIcon(v, R.id.btn_backspace, ICON_BACKSPACE)
        applyMsIcon(v, R.id.btn_enter,     ICON_ENTER)
        v.findViewById<TextView>(R.id.btn_symbols).text = "?123"
        updateShiftIcon(v)

        val letters = "qwertyuiopasdfghjklzxcvbnm"
        val ids = listOf(
            R.id.kq,R.id.kw,R.id.ke,R.id.kr,R.id.kt,R.id.ky,R.id.ku,R.id.ki,R.id.ko,R.id.kp,
            R.id.ka,R.id.ks,R.id.kd,R.id.kf,R.id.kg,R.id.kh,R.id.kj,R.id.kk,R.id.kl,
            R.id.kz,R.id.kx,R.id.kc,R.id.kv,R.id.kb,R.id.kn,R.id.km
        )
        ids.forEachIndexed { i, id ->
            v.findViewById<TextView>(id).setOnClickListener { typeChar(letters[i]) }
        }

        val numIds = listOf(R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,
                            R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.k0)
        "1234567890".forEachIndexed { i, c ->
            v.findViewById<TextView>(numIds[i]).setOnClickListener { typeText(c.toString()) }
        }

        v.findViewById<TextView>(R.id.btn_shift).setOnClickListener { handleShift(v) }
        v.findViewById<TextView>(R.id.btn_backspace).apply {
            setOnClickListener     { doBackspace() }
            setOnLongClickListener { startBackspaceRepeat(); true }
            setOnTouchListener     { _, e ->
                if (e.action == MotionEvent.ACTION_UP ||
                    e.action == MotionEvent.ACTION_CANCEL) stopBackspaceRepeat()
                false
            }
        }
        v.findViewById<TextView>(R.id.btn_space).setOnClickListener   { typeText(" ") }
        v.findViewById<TextView>(R.id.btn_comma).setOnClickListener   { typeText(",") }
        v.findViewById<TextView>(R.id.btn_period).setOnClickListener  { typeText(".") }
        v.findViewById<TextView>(R.id.btn_enter).setOnClickListener   { doEnter() }
        v.findViewById<TextView>(R.id.btn_symbols).setOnClickListener { showMode(Mode.SYMBOLS) }
        v.findViewById<TextView>(R.id.btn_emoji).setOnClickListener   { showMode(Mode.EMOJI) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener   {
            showTools(v.findViewById(R.id.btn_tools))
        }
        return v
    }

    // ── SYMBOLS ──────────────────────────────────────────────────────────────
    private fun buildSymbols(): View {
        val v = layoutInflater.inflate(R.layout.symbols, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)
        applyMsIcon(v, R.id.btn_emoji,     ICON_EMOJI)
        applyMsIcon(v, R.id.btn_backspace, ICON_BACKSPACE)
        applyMsIcon(v, R.id.btn_enter,     ICON_ENTER)

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

        val numIds = listOf(R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,
                            R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.k0)
        "1234567890".forEachIndexed { i, c ->
            v.findViewById<TextView>(numIds[i]).setOnClickListener { typeText(c.toString()) }
        }

        v.findViewById<TextView>(R.id.btn_backspace).apply {
            setOnClickListener     { doBackspace() }
            setOnLongClickListener { startBackspaceRepeat(); true }
            setOnTouchListener     { _, e ->
                if (e.action == MotionEvent.ACTION_UP ||
                    e.action == MotionEvent.ACTION_CANCEL) stopBackspaceRepeat()
                false
            }
        }
        v.findViewById<TextView>(R.id.btn_space).setOnClickListener  { typeText(" ") }
        v.findViewById<TextView>(R.id.btn_comma).setOnClickListener  { typeText(",") }
        v.findViewById<TextView>(R.id.btn_period).setOnClickListener { typeText(".") }
        v.findViewById<TextView>(R.id.btn_enter).setOnClickListener  { doEnter() }
        v.findViewById<TextView>(R.id.btn_abc).setOnClickListener    { showMode(Mode.QWERTY) }
        v.findViewById<TextView>(R.id.btn_emoji).setOnClickListener  { showMode(Mode.EMOJI) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener  {
            showTools(v.findViewById(R.id.btn_tools))
        }
        return v
    }

    // ── EMOJI ────────────────────────────────────────────────────────────────
    private fun buildEmoji(): View {
        val v = layoutInflater.inflate(R.layout.emoji, null)

        applyMsIcon(v, R.id.btn_clipboard, ICON_CLIPBOARD)
        applyMsIcon(v, R.id.btn_tools,     ICON_TOOLS)

        val grid = v.findViewById<GridLayout>(R.id.emoji_grid)
        loadEmojiGrid(grid, currentEmojiCategory)

        mapOf(
            R.id.tab_faces      to "faces",
            R.id.tab_people     to "people",
            R.id.tab_animals    to "animals",
            R.id.tab_food       to "food",
            R.id.tab_activities to "activities",
            R.id.tab_objects    to "objects"
        ).forEach { (id, cat) ->
            v.findViewById<TextView>(id).setOnClickListener {
                currentEmojiCategory = cat
                loadEmojiGrid(grid, cat)
            }
        }

        v.findViewById<TextView>(R.id.btn_abc).setOnClickListener         { showMode(Mode.QWERTY) }
        v.findViewById<TextView>(R.id.btn_clipboard).setOnClickListener   { openClipboard() }
        v.findViewById<TextView>(R.id.btn_tools).setOnClickListener       {
            showTools(v.findViewById(R.id.btn_tools))
        }
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

    // ── CLIPBOARD ────────────────────────────────────────────────────────────
    private fun buildClipboard(): View {
        val v = layoutInflater.inflate(R.layout.clipboard, null)

        applyMsIcon(v, R.id.btn_clipboard_active, ICON_CLIPBOARD)

        clipAdapter = ClipboardAdapter(
            clipboardItems,
            onPaste  = { item -> typeText(item); showMode(Mode.QWERTY) },
            onDelete = { pos  ->
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
        val ic  = currentInputConnection
        val sel = ic?.getSelectedText(0)
        if (!sel.isNullOrEmpty()) addToClipboard(sel.toString())
        showMode(Mode.CLIPBOARD)
    }

    private fun addToClipboard(text: String) {
        clipboardItems.remove(text)
        clipboardItems.add(0, text)
        if (clipboardItems.size > MAX_CLIP) clipboardItems.removeAt(clipboardItems.size - 1)
        saveClipboard()
        if (::clipAdapter.isInitialized) clipAdapter.notifyDataSetChanged()
    }

    private fun saveClipboard() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREFS_CLIP, clipboardItems.take(MAX_CLIP).joinToString("\u001F"))
            .apply()
    }

    private fun loadClipboard() {
        val s = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_CLIP, "") ?: ""
        clipboardItems.clear()
        if (s.isNotEmpty()) clipboardItems.addAll(s.split("\u001F"))
    }

    // ── TOOLS DROP-UP ────────────────────────────────────────────────────────
    private fun showTools(anchor: View) {
        toolsPopup?.dismiss()
        val popup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF2C2C2C.toInt())
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }
        listOf("Select All", "Copy", "Paste").forEach { label ->
            popup.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16.dp, 12.dp, 16.dp, 12.dp)
                setOnClickListener {
                    toolsPopup?.dismiss()
                    when (label) {
                        "Select All" -> currentInputConnection
                            ?.performContextMenuAction(android.R.id.selectAll)
                        "Copy"       -> currentInputConnection
                            ?.performContextMenuAction(android.R.id.copy)
                        "Paste"      -> {
                            val clip = (getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager)
                                .primaryClip?.getItemAt(0)
                                ?.coerceToText(this@Keyboard)?.toString()
                            if (!clip.isNullOrEmpty()) {
                                typeText(clip)
                                addToClipboard(clip)
                            }
                        }
                    }
                }
            })
        }
        toolsPopup = PopupWindow(popup,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 8f
            showAsDropDown(anchor, 0, -(anchor.height * 4))
        }
    }

    // ── INPUT ────────────────────────────────────────────────────────────────
    private fun typeChar(c: Char) {
        val out = if (shiftState != ShiftState.OFF) c.uppercaseChar() else c.lowercaseChar()
        typeText(out.toString())
        if (shiftState == ShiftState.ONCE) {
            shiftState = ShiftState.OFF
            updateShiftIcon(qwertyView)
            updateLetterKeys(qwertyView)
        }
    }

    private fun typeText(s: String) { currentInputConnection?.commitText(s, 1) }

    private fun doBackspace() { currentInputConnection?.deleteSurroundingText(1, 0) }

    private fun startBackspaceRepeat() {
        bsRunnable = object : Runnable {
            override fun run() { doBackspace(); bsHandler.postDelayed(this, 50) }
        }
        bsHandler.postDelayed(bsRunnable!!, 400)
    }

    private fun stopBackspaceRepeat() {
        bsRunnable?.let { bsHandler.removeCallbacks(it) }
        bsRunnable = null
    }

    private fun doEnter() {
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER))
        }
    }

    // ── SHIFT ────────────────────────────────────────────────────────────────
    private fun handleShift(v: View) {
        val now = System.currentTimeMillis()
        shiftState = when {
            shiftState == ShiftState.LOCKED  -> ShiftState.OFF
            now - lastShiftTap < 200         -> ShiftState.LOCKED
            shiftState == ShiftState.OFF     -> ShiftState.ONCE
            else                             -> ShiftState.OFF
        }
        lastShiftTap = now
        updateShiftIcon(v)
        updateLetterKeys(v)
    }

    private fun updateShiftIcon(v: View) {
        v.findViewById<TextView>(R.id.btn_shift)?.apply {
            typeface = msFont
            text = if (shiftState == ShiftState.LOCKED) ICON_SHIFT_LOCK else ICON_SHIFT
            setTextColor(if (shiftState == ShiftState.OFF) 0xFFFFFFFF.toInt() else 0xFF4FC3F7.toInt())
        }
    }

    private fun updateLetterKeys(v: View) {
        val upper = shiftState != ShiftState.OFF
        val letters = "qwertyuiopasdfghjklzxcvbnm"
        listOf(
            R.id.kq,R.id.kw,R.id.ke,R.id.kr,R.id.kt,R.id.ky,R.id.ku,R.id.ki,R.id.ko,R.id.kp,
            R.id.ka,R.id.ks,R.id.kd,R.id.kf,R.id.kg,R.id.kh,R.id.kj,R.id.kk,R.id.kl,
            R.id.kz,R.id.kx,R.id.kc,R.id.kv,R.id.kb,R.id.kn,R.id.km
        ).forEachIndexed { i, id ->
            v.findViewById<TextView>(id)?.text =
                if (upper) letters[i].uppercaseChar().toString() else letters[i].toString()
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        
        val enterIcon = when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "\uE8B6"
            EditorInfo.IME_ACTION_SEND   -> "\uE163"
            EditorInfo.IME_ACTION_GO     -> "\uE5C8"
            EditorInfo.IME_ACTION_DONE   -> "\uE876"
            else                         -> ICON_ENTER
        }
        
        // Update enter icon on both qwerty and symbols views
        val views = mutableListOf<View>()
        if (::qwertyView.isInitialized) views.add(qwertyView)
        if (::symbolsView.isInitialized) views.add(symbolsView)
        
        views.forEach { view ->
            view.findViewById<TextView>(R.id.btn_enter)?.apply {
                typeface = msFont
                text = enterIcon
            }
        }
    }

    // ── UTILS ────────────────────────────────────────────────────────────────
    private fun applyMsIcon(root: View, id: Int, icon: String) {
        root.findViewById<TextView>(id)?.apply { typeface = msFont; text = icon }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        stopBackspaceRepeat()
        toolsPopup?.dismiss()
    }
}

// ── CLIPBOARD ADAPTER ────────────────────────────────────────────────────────
class ClipboardAdapter(
    private val items: MutableList<String>,
    private val onPaste: (String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.VH>() {

    inner class VH(val root: LinearLayout, val text: TextView, val del: TextView)
        : RecyclerView.ViewHolder(root)

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
