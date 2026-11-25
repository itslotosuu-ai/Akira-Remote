package com.akira.orbitbrowser

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private val green = Color.rgb(0, 235, 130)
    private val dark = Color.rgb(5, 12, 12)
    private val prefs by lazy { getSharedPreferences("orbit_browser", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        buildInterface()
        if (savedInstanceState == null) {
            browser.loadUrl("https://www.google.com/")
        } else {
            browser.restoreState(savedInstanceState)
        }
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 8)
            setBackgroundColor(dark)
        }

        val brand = TextView(this).apply {
            text = "ORBIT  /  BROWSER"
            setTextColor(green)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setPadding(0, 2, 0, 8)
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, -2))

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            hint = "Search or enter address"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 13f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(12, 5, 8, 5)
            background = rounded(Color.rgb(20, 31, 31), green, 1)
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_GO ||
                    action == EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    navigateFromAddress()
                    true
                } else false
            }
        }
        addressRow.addView(address, LinearLayout.LayoutParams(0, 42, 1f))
        addressRow.addView(makeButton("GO") { navigateFromAddress() },
            LinearLayout.LayoutParams(48, 42).apply { leftMargin = 5 })
        root.addView(addressRow)

        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 7, 0, 7)
        }
        shortcuts.addView(makeButton("G") { load("https://www.google.com/") }, weighted())
        shortcuts.addView(makeButton("AI") { load("https://gemini.google.com/app") }, weighted())
        shortcuts.addView(makeButton("★") { openBookmark() }, weighted())
        shortcuts.addView(makeButton("MIC") { startVoiceSearch() }, weighted())
        root.addView(shortcuts)

        status = TextView(this).apply {
            text = "READY  •  WEB"
            setTextColor(green)
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 5)
        }
        root.addView(status)

        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.safeBrowsingEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    return uri.scheme != "https" && uri.scheme != "http"
                }
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    address.setText(url ?: "")
                    status.text = "LOADING…"
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    address.setText(url ?: "")
                    status.text = "PAGE READY"
                }
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) status.text = "PAGE ERROR"
                }
            }
        }
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }
        bottom.addView(makeButton("BACK") { if (browser.canGoBack()) browser.goBack() }, weighted())
        bottom.addView(makeButton("NEXT") { if (browser.canGoForward()) browser.goForward() }, weighted())
        bottom.addView(makeButton("REFRESH") { browser.reload() }, weighted())
        bottom.addView(makeButton("SAVE") { saveBookmark() }, weighted())
        root.addView(bottom)
        setContentView(root)
    }

    private fun weighted() = LinearLayout.LayoutParams(0, 38, 1f).apply {
        leftMargin = 2
        rightMargin = 2
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 9f
        isAllCaps = false
        setTextColor(green)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = rounded(Color.rgb(17, 29, 28), Color.rgb(0, 115, 72), 1)
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(2, 0, 2, 0)
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int, width: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 18f
        setColor(fill)
        setStroke(width, stroke)
    }

    private fun load(url: String) {
        browser.loadUrl(url)
    }

    private fun navigateFromAddress() {
        val raw = address.text.toString().trim()
        if (raw.isBlank()) return
        val url = if (raw.contains(" ") || !raw.contains(".") && !raw.startsWith("http")) {
            "https://www.google.com/search?q=" + Uri.encode(raw)
        } else if (raw.startsWith("https://") || raw.startsWith("http://")) {
            raw
        } else {
            "https://$raw"
        }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        browser.loadUrl(url)
    }

    private fun saveBookmark() {
        val current = browser.url ?: return
        prefs.edit().putString("bookmark", current).apply()
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
    }

    private fun openBookmark() {
        val saved = prefs.getString("bookmark", null)
        if (saved == null) Toast.makeText(this, "No bookmark saved", Toast.LENGTH_SHORT).show()
        else browser.loadUrl(saved)
    }

    private fun startVoiceSearch() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        launchRecognizer()
    }

    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search the web")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            startActivityForResult(intent, 42)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice input is not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            launchRecognizer()
        }
    }

    @Deprecated("Deprecated by Android, retained for Wear OS recognizer compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                address.setText(spoken)
                navigateFromAddress()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        browser.saveState(outState)
    }

    @Deprecated("Deprecated by Android, retained for Wear OS back handling")
    override fun onBackPressed() {
        if (::browser.isInitialized && browser.canGoBack()) browser.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
