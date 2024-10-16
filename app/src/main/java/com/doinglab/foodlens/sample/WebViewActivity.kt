package com.doinglab.foodlens.sample

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview)

        // WebView를 초기화
        val webView: WebView = findViewById(R.id.webview)

        // ID 토큰 수신
        val idToken = intent.getStringExtra("idToken")

        // WebView 설정
        webView.apply {
            settings.javaScriptEnabled = true  // JavaScript 활성화
            webViewClient = WebViewClient()    // 외부 브라우저가 아닌 WebView에서 열리도록 설정
        }

        // URL 로드 (예시: ID 토큰을 쿼리 파라미터로 포함)
        val url = "http://snapdiet.myoung.my:8080/articles?idToken=$idToken"
        webView.loadUrl(url)
    }
}
