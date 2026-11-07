package com.home.lunakiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String DEFAULT_URL = "http://192.168.3.7:3000";

    private WebView web;
    private SharedPreferences sp;
    private TextView hint;
    private int cx = 480;
    private int cy = 270;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("cfg", MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.requestFocus();

        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        hint = new TextView(this);
        hint.setText("方向键移动光标    OK 键点击    菜单键改地址");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(20);
        hint.setBackgroundColor(0xBB000000);
        hint.setPadding(36, 18, 36, 18);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT);
        hp.leftMargin = 40;
        hp.topMargin = 40;
        root.addView(hint, hp);

        setContentView(root);

        String url = DEFAULT_URL;
        if (getIntent() != null && getIntent().getStringExtra("url") != null) {
            url = getIntent().getStringExtra("url");
            sp.edit().putString("url", url).apply();
        } else {
            url = sp.getString("url", DEFAULT_URL);
        }
        web.loadUrl(url);

        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                hint.setVisibility(View.GONE);
            }
        }, 7000);
    }

    private void cursorJs(boolean click) {
        if (cx < 5) cx = 5;
        if (cy < 5) cy = 5;
        StringBuilder js = new StringBuilder();
        js.append("(function(){");
        js.append("var W=window.innerWidth||960,H=window.innerHeight||540;");
        js.append("var x=").append(cx).append(",y=").append(cy).append(";");
        js.append("if(x>W-10)x=W-10;if(y>H-10)y=H-10;");
        js.append("var c=document.getElementById('__wbcur');");
        js.append("if(!c){c=document.createElement('div');c.id='__wbcur';");
        js.append("c.style.cssText='position:fixed;width:24px;height:24px;border:2px solid #00e5ff;");
        js.append("border-radius:50%;pointer-events:none;z-index:2147483647;box-shadow:0 0 10px #00e5ff;transition:left .05s,top .05s';");
        js.append("document.body.appendChild(c);}");
        js.append("c.style.left=x+'px';c.style.top=y+'px';");
        js.append("var t=document.elementFromPoint(x,y);");
        js.append("if(t){t.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:x,clientY:y}));}");
        if (click) {
            js.append("if(t){['mousedown','mouseup','click'].forEach(function(n){");
            js.append("t.dispatchEvent(new MouseEvent(n,{bubbles:true,clientX:x,clientY:y}));});}");
        }
        js.append("})()");
        web.evaluateJavascript(js.toString(), null);
    }

    private void askUrl() {
        final EditText et = new EditText(this);
        et.setText(sp.getString("url", DEFAULT_URL));
        new AlertDialog.Builder(this)
                .setTitle("服务器地址")
                .setView(et)
                .setPositiveButton("打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String u = et.getText().toString().trim();
                        if (u.length() == 0) return;
                        if (!u.startsWith("http")) u = "http://" + u;
                        sp.edit().putString("url", u).apply();
                        web.loadUrl(u);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                cy -= 50;
                cursorJs(false);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                cy += 50;
                cursorJs(false);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                cx -= 50;
                cursorJs(false);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                cx += 50;
                cursorJs(false);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                cursorJs(true);
                return true;
            case KeyEvent.KEYCODE_MENU:
                askUrl();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (web.canGoBack()) {
                    web.goBack();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
