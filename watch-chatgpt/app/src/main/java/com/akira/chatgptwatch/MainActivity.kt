package com.akira.chatgptwatch

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var chat: WebView
    private lateinit var address: EditText
    private lateinit var state: TextView
    private val green = Color.rgb(0, 235, 130)
    private val backgroundColor = Color.rgb(4, 10, 10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        buildUi()
        if (savedInstanceState == null) {
            chat.loadUrl("https://chatgpt.com/")
        } else {
            chat.restoreState(savedInstanceState)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(10, 10, 10, 8)
            setBackgroundColor(backgroundColor)
        }

        val title = TextView(this).apply {
            text = "CHATGPT"
            textSize = 15f
            letterSpacing = 0.22f
            gravity = Gravity.CENTER
            setTextColor(green)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 2, 0, 1)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "WRIST AI TERMINAL"
            textSize = 8f
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(105, 150, 130))
            setPadding(0, 0, 0, 7)
        }
        root.addView(subtitle)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(makeButton("HOME") { chat.loadUrl("https://chatgpt.com/") }, weighted())
        controls.addView(makeButton("VOICE") { requestVoice() }, weighted())
        controls.addView(makeButton("↻") { chat.reload() }, weighted())
        root.addView(controls)

        state = TextView(this).apply {
            text = "CONNECTING"
            textSize = 8f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(green)
            setPadding(0, 5, 0, 5)
        }
        root.addView(state)

        chat = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.safeBrowsingEnabled = true
            settings.userAgentString = settings.userAgentString
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val scheme = request?.url?.scheme ?: return false
                    return scheme != "https" && scheme != "http"
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    state.text = "LOADING CHATGPT"
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    state.text = "CHATGPT READY"
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) state.text = "CONNECTION ERROR"
                }
            }
        }
        root.addView(chat, LinearLayout.LayoutParams(-1, 0, 1f))

        address = EditText(this).apply {
            hint = "Ask by voice…"
            setHintTextColor(Color.rgb(105, 125, 115))
            setTextColor(Color.WHITE)
            textSize = 12f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(12, 2, 12, 2)
            background = rounded(Color.rgb(15, 25, 23), Color.rgb(0, 120, 75))
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    insertPrompt(address.text.toString())
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(address.windowToken, 0)
                    true
                } else false
            }
        }
        root.addView(address, LinearLayout.LayoutParams(-1, 38).apply { topMargin = 6 })

        val send = makeButton("INSERT INTO CHAT") {
            insertPrompt(address.text.toString())
        }
        root.addView(send, LinearLayout.LayoutParams(-1, 36).apply { topMargin = 5 })

        setContentView(root)
    }

    private fun weighted() = LinearLayout.LayoutParams(0, 34, 1f).apply {
        leftMargin = 3
        rightMargin = 3
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 9f
        isAllCaps = false
        setTextColor(green)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = rounded(Color.rgb(13, 25, 22), Color.rgb(0, 115, 72))
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(2, 0, 2, 0)
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 22f
        setColor(fill)
        setStroke(1, stroke)
    }

    private fun requestVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 21)
            return
        }
        launchRecognizer()
    }

    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            startActivityForResult(intent, 22)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice input is not available on this watch", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 21 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            launchRecognizer()
        }
    }

    @Deprecated("Android callback retained for Wear OS speech recognizer compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 22 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                address.setText(spoken)
                insertPrompt(spoken)
            }
        }
    }

    private fun insertPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val safeText = JSONObject.quote(prompt)
        val script = "(function(){var e=document.querySelector('#prompt-textarea')||document.querySelector('[contenteditable=true]')||document.querySelector('textarea');if(!e)return 'NO_COMPOSER';e.focus();if(e.tagName==='TEXTAREA'){var s=Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set;s.call(e,$safeText);}else{e.innerText=$safeText;}e.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:$safeText}));return 'INSERTED';})()"
        chat.evaluateJavascript(script) { result ->
            if (result == "\"NO_COMPOSER\"") {
                Toast.makeText(this, "Tap the ChatGPT message field first, then try again", Toast.LENGTH_LONG).show()
            } else {
                state.text = "MESSAGE INSERTED • TAP SEND"
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        chat.saveState(outState)
    }

    @Deprecated("Wear OS back navigation fallback")
    override fun onBackPressed() {
        if (::chat.isInitialized && chat.canGoBack()) chat.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::chat.isInitialized) chat.destroy()
        super.onDestroy()
    }
}
