package com.radha.ai

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// ---------------------------------------------------------------- ORB

class OrbView(c: Context) : View(c) {
    var state = 0 // 0 idle, 1 listening, 2 thinking, 3 speaking
    var level = 0f
    private var t = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var avatar: Bitmap? = null
    private var avatarShader: BitmapShader? = null
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarMatrix = Matrix()

    fun setAvatar(b: Bitmap) {
        avatar = b
        avatarShader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        avatarPaint.shader = avatarShader
        invalidate()
    }
    private val anim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 5000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f
        val accent = when (state) {
            1 -> 0xFF22D3EE.toInt()
            2 -> 0xFFA855F7.toInt()
            3 -> 0xFF34D399.toInt()
            else -> 0xFF3B82F6.toInt()
        }
        level *= 0.9f
        val rad = Math.toRadians(t.toDouble() * (if (state == 3) 12 else 3))
        val pulse = Math.sin(rad).toFloat() * (if (state == 3) 0.05f else 0.025f) +
            (level.coerceIn(0f, 10f) / 10f) * 0.1f

        // outer glow
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, r,
            intArrayOf((accent and 0x00FFFFFF) or 0x66000000, accent and 0x00FFFFFF),
            null, Shader.TileMode.CLAMP
        )
        cv.drawCircle(cx, cy, r, p)

        // core
        val core = r * 0.58f * (1f + pulse)
        p.shader = RadialGradient(
            cx, cy - core * 0.3f, core * 1.2f,
            intArrayOf(0xFF1E3A8A.toInt(), 0xFF050B2E.toInt()),
            null, Shader.TileMode.CLAMP
        )
        cv.drawCircle(cx, cy, core, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = accent
        cv.drawCircle(cx, cy, core, p)

        // rotating arcs
        val speed = if (state == 2) 4f else 1f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 8f
        p.color = Color.WHITE
        val r1 = r * 0.74f
        rect.set(cx - r1, cy - r1, cx + r1, cy + r1)
        cv.drawArc(rect, t * speed, 90f, false, p)
        p.color = accent
        p.strokeWidth = 6f
        val r2 = r * 0.84f
        rect.set(cx - r2, cy - r2, cx + r2, cy + r2)
        cv.drawArc(rect, -t * speed * 1.4f, 70f, false, p)
        cv.drawArc(rect, -t * speed * 1.4f + 180f, 40f, false, p)

        // avatar (or text label if the image is missing)
        val av = avatar
        if (av != null) {
            val inner = core * 0.94f
            val s = (inner * 2f) / av.width
            avatarMatrix.reset()
            avatarMatrix.setScale(s, s)
            avatarMatrix.postTranslate(cx - inner, cy - inner)
            avatarShader?.setLocalMatrix(avatarMatrix)
            cv.drawCircle(cx, cy, inner, avatarPaint)
        } else {
            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = core * 0.34f
            cv.drawText("RADHA", cx, cy + p.textSize * 0.35f, p)
        }
    }
}

// ---------------------------------------------------------------- AI

object Ai {
    private const val MODEL = "gpt-4o-mini"
    private val history = JSONArray()

    private val tools by lazy {
        JSONArray(
            """[
{"type":"function","function":{"name":"open_app","description":"Open an installed app by name","parameters":{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}}},
{"type":"function","function":{"name":"call_contact","description":"Prepare a phone call to a contact name or number. The user confirms on screen.","parameters":{"type":"object","properties":{"name_or_number":{"type":"string"}},"required":["name_or_number"]}}},
{"type":"function","function":{"name":"whatsapp_message","description":"Open a WhatsApp chat with a contact and a drafted message. The user must press send.","parameters":{"type":"object","properties":{"contact":{"type":"string"},"message":{"type":"string"}},"required":["contact","message"]}}},
{"type":"function","function":{"name":"set_alarm","description":"Set an alarm","parameters":{"type":"object","properties":{"hour":{"type":"integer"},"minute":{"type":"integer"},"label":{"type":"string"}},"required":["hour","minute"]}}},
{"type":"function","function":{"name":"open_url","description":"Open a website","parameters":{"type":"object","properties":{"url":{"type":"string"}},"required":["url"]}}},
{"type":"function","function":{"name":"web_search","description":"Search Google in the browser","parameters":{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}}},
{"type":"function","function":{"name":"flashlight","description":"Turn the flashlight on or off","parameters":{"type":"object","properties":{"on":{"type":"boolean"}},"required":["on"]}}},
{"type":"function","function":{"name":"battery_status","description":"Get the battery percentage","parameters":{"type":"object","properties":{}}}}
]"""
        )
    }

    private fun post(key: String, body: JSONObject): JSONObject {
        val c = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 60000
        c.setRequestProperty("Authorization", "Bearer $key")
        c.setRequestProperty("Content-Type", "application/json")
        c.doOutput = true
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code !in 200..299) {
            val msg = try {
                JSONObject(text).getJSONObject("error").getString("message")
            } catch (e: Exception) {
                text.take(150)
            }
            throw Exception("HTTP $code: $msg")
        }
        return JSONObject(text)
    }

    fun chat(key: String, lang: String, text: String, exec: (String, JSONObject) -> String): String {
        val sys = JSONObject().put("role", "system").put(
            "content",
            "You are RADHA AI, a fast, warm and intelligent female personal voice assistant on the user's " +
                "Android phone, like a helpful human assistant. Reply in $lang. Keep spoken replies to 1-3 " +
                "short sentences, no markdown, no emojis. Use the tools to act on the phone when asked. " +
                "Calls and WhatsApp messages are only prepared; the user confirms on screen, so never say " +
                "they were completed. Never claim an action succeeded unless the tool result says so. " +
                "Current time: ${java.util.Date()}."
        )
        val msgs = JSONArray()
        msgs.put(sys)
        for (i in 0 until history.length()) msgs.put(history.get(i))
        msgs.put(JSONObject().put("role", "user").put("content", text))

        var reply = ""
        for (round in 0 until 4) {
            val body = JSONObject().put("model", MODEL).put("messages", msgs).put("tools", tools)
            val msg = post(key, body).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            msgs.put(msg)
            val calls = msg.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                reply = if (msg.isNull("content")) "" else msg.getString("content")
                break
            }
            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val f = call.getJSONObject("function")
                val args = try { JSONObject(f.getString("arguments")) } catch (e: Exception) { JSONObject() }
                val result = try { exec(f.getString("name"), args) } catch (e: Exception) { "Failed: ${e.message}" }
                msgs.put(
                    JSONObject().put("role", "tool").put("tool_call_id", call.getString("id")).put("content", result)
                )
            }
        }
        history.put(JSONObject().put("role", "user").put("content", text))
        history.put(JSONObject().put("role", "assistant").put("content", reply))
        while (history.length() > 12) history.remove(0)
        return reply
    }
}

// ---------------------------------------------------------------- ACTIVITY

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var orb: OrbView
    private lateinit var status: TextView
    private lateinit var reply: TextView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private val ui = Handler(Looper.getMainLooper())
    private var busy = false
    private var relisten = false
    private val prefs by lazy { getSharedPreferences("radha", MODE_PRIVATE) }
    private val langs = arrayOf("en-IN" to "English", "mr-IN" to "मराठी", "hi-IN" to "हिन्दी")

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun key() = prefs.getString("key", "") ?: ""
    private fun langIdx() = prefs.getInt("lang", 0).coerceIn(0, langs.size - 1)
    private fun langTag() = langs[langIdx()].first
    private fun langName() = langs[langIdx()].second

    private fun chip(txt: String, click: (TextView) -> Unit): TextView = TextView(this).apply {
        text = txt
        setTextColor(Color.WHITE)
        textSize = 15f
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(12), dp(18), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(Color.parseColor("#14204A"))
            setStroke(dp(1), Color.parseColor("#3B82F6"))
        }
        setOnClickListener { click(this) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val bg = Color.parseColor("#05070F")
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(36), dp(16), dp(20))
        }
        val title = TextView(this).apply {
            text = "RADHA AI"
            setTextColor(Color.WHITE)
            textSize = 18f
            letterSpacing = 0.2f
            typeface = Typeface.DEFAULT_BOLD
        }
        orb = OrbView(this).apply { setOnClickListener { listen() } }
        try {
            val bytes = Base64.decode(Avatar.B64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { orb.setAvatar(it) }
        } catch (e: Exception) {
        }
        status = TextView(this).apply {
            text = "Tap the orb and speak"
            setTextColor(Color.parseColor("#7DD3FC"))
            textSize = 15f
            gravity = Gravity.CENTER
        }
        reply = TextView(this).apply {
            setTextColor(Color.parseColor("#E5E7EB"))
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        val sv = ScrollView(this).apply { addView(reply) }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val talk = chip("🎤 Talk") { listen() }
        val lang = chip(langName()) { v ->
            prefs.edit().putInt("lang", (langIdx() + 1) % langs.size).apply()
            (v).text = langName()
            applyTtsLang()
        }
        val set = chip("⚙ Settings") { showSettings() }
        val lp = { LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) } }
        row.addView(talk, lp())
        row.addView(lang, lp())
        row.addView(set, lp())

        root.addView(title)
        root.addView(orb, LinearLayout.LayoutParams(dp(300), dp(300)).apply { topMargin = dp(24) })
        root.addView(status)
        root.addView(sv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(row)
        setContentView(root)

        tts = TextToSpeech(this, this)

        if (key().isBlank()) showSettings()
        else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) askPerms()
        handleIntent(intent)
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        handleIntent(i)
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (i.getBooleanExtra("listen", false) || i.action == Intent.ACTION_ASSIST) {
            ui.postDelayed({ listen() }, 700)
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }

    // ----- TTS
    override fun onInit(s: Int) {
        ttsReady = s == TextToSpeech.SUCCESS
        if (!ttsReady) return
        applyTtsLang()
        tts?.setPitch(1.1f)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { ui.post { orb.state = 3 } }
            override fun onDone(id: String?) { ui.post { finishSpeak() } }
            override fun onError(id: String?) { ui.post { finishSpeak() } }
        })
    }

    private fun applyTtsLang() {
        val r = tts?.setLanguage(Locale.forLanguageTag(langTag()))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speak(text: String, again: Boolean) {
        reply.text = text
        relisten = again
        busy = true
        if (!ttsReady) { finishSpeak(); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "radha")
    }

    private fun finishSpeak() {
        orb.state = 0
        busy = false
        status.text = "Tap the orb and speak"
        if (relisten) { relisten = false; listen() }
    }

    // ----- Speech recognition
    private fun listen() {
        if (busy) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            askPerms(); return
        }
        if (key().isBlank()) {
            status.text = "Please add your OpenAI API key first"
            showSettings(); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition is not available on this phone"; return
        }
        tts?.stop()
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) { orb.level = maxOf(orb.level, rms) }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onPartialResults(p: Bundle?) {
                val t = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!t.isNullOrBlank()) reply.text = t
            }
            override fun onError(e: Int) {
                orb.state = 0
                status.text = when (e) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "I didn't hear anything. Tap the orb to talk."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network problem"
                    else -> "Speech error ($e). Tap the orb to try again."
                }
            }
            override fun onResults(res: Bundle?) {
                val t = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t.isNullOrBlank()) { orb.state = 0; status.text = "Tap the orb and speak" } else handle(t)
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        orb.state = 1
        status.text = "Listening…"
        r.startListening(i)
    }

    private fun handle(text: String) {
        val low = text.lowercase()
        reply.text = text
        if (low.contains("goodbye") || low.contains("good bye") || low == "stop" || low.contains("बाय")) {
            speak("Goodbye.", false); return
        }
        busy = true
        orb.state = 2
        status.text = "Thinking…"
        Thread {
            var ans = ""
            var err: String? = null
            try {
                ans = Ai.chat(key(), langName(), text) { n, a -> runTool(n, a) }
            } catch (e: Exception) {
                err = e.message ?: "Unknown error"
            }
            ui.post {
                if (err != null) speak("I could not get a reply. $err", false)
                else speak(ans.ifBlank { "Okay." }, true)
            }
        }.start()
    }

    // ----- Phone tools
    private fun findNumber(q: String): String? {
        if (q.count { it.isDigit() } >= 6) return q.filter { it.isDigit() || it == '+' }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$q%"), null
        )?.use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    private fun go(i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun runTool(name: String, a: JSONObject): String {
        when (name) {
            "open_app" -> {
                val q = a.optString("name")
                val pm = packageManager
                val apps = pm.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
                )
                val m = apps.firstOrNull { it.loadLabel(pm).toString().equals(q, true) }
                    ?: apps.firstOrNull { it.loadLabel(pm).toString().contains(q, true) }
                    ?: return "No installed app found named $q"
                val li = pm.getLaunchIntentForPackage(m.activityInfo.packageName)
                    ?: return "Cannot launch $q"
                go(li)
                return "Opened ${m.loadLabel(pm)}"
            }
            "call_contact" -> {
                val q = a.optString("name_or_number")
                val num = findNumber(q) ?: return "No number found for $q (check contacts permission)"
                runOnUiThread {
                    AlertDialog.Builder(this).setTitle("Call $q?").setMessage(num)
                        .setPositiveButton("Call") { _, _ ->
                            val uri = Uri.parse("tel:" + Uri.encode(num))
                            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                                go(Intent(Intent.ACTION_CALL, uri))
                            else go(Intent(Intent.ACTION_DIAL, uri))
                        }
                        .setNegativeButton("Cancel", null).show()
                }
                return "A confirmation dialog to call $q was shown. The call has not been made yet."
            }
            "whatsapp_message" -> {
                val c = a.optString("contact")
                var num = (findNumber(c) ?: return "No number found for $c").filter { it.isDigit() }
                if (num.length == 10) num = "91$num"
                val msg = a.optString("message")
                go(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num?text=" + Uri.encode(msg))))
                return "WhatsApp chat opened with the message drafted. It is NOT sent until the user presses send."
            }
            "set_alarm" -> {
                go(
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, a.optInt("hour"))
                        .putExtra(AlarmClock.EXTRA_MINUTES, a.optInt("minute"))
                        .putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label", "RADHA AI"))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                )
                return "Alarm request sent to the clock app"
            }
            "open_url" -> {
                var u = a.optString("url")
                if (!u.startsWith("http")) u = "https://$u"
                go(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                return "Opened $u"
            }
            "web_search" -> {
                go(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(a.optString("query")))))
                return "Opened Google search"
            }
            "flashlight" -> {
                val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cm.setTorchMode(cm.cameraIdList.first(), a.optBoolean("on", true))
                return "Flashlight set to ${a.optBoolean("on", true)}"
            }
            "battery_status" -> {
                val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                return "Battery is ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} percent"
            }
        }
        return "Unknown tool $name"
    }

    // ----- Settings / permissions
    private fun askPerms() {
        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            ), 1
        )
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), 0)
        }
        val et = EditText(this).apply {
            hint = "OpenAI API key (sk-...)"
            setText(key())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
        }
        box.addView(et)
        box.addView(Button(this).apply {
            text = "Grant permissions"
            setOnClickListener { askPerms() }
        })
        box.addView(Button(this).apply {
            text = "Set as default assistant (power button)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
        })
        AlertDialog.Builder(this).setTitle("RADHA AI Settings").setView(box)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("key", et.text.toString().trim()).apply()
            }.show()
    }
}

// ---------------------------------------------------------------- ASSISTANT SERVICES
// These declare RADHA AI voice-interaction components for Android.

class RadhaInteractionService : VoiceInteractionService()

class RadhaSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = RadhaSession(this)
}

class RadhaSession(ctx: Context) : VoiceInteractionSession(ctx) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val i = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("listen", true)
        context.startActivity(i)
        hide()
    }
}

class RadhaRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent?, cb: Callback?) {}
    override fun onCancel(cb: Callback?) {}
    override fun onStopListening(cb: Callback?) {}
}
