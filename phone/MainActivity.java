package com.akira.remote;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.util.Collections;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 41;
    private static final int BG = 0xFF050907;
    private static final int PANEL = 0xFF0A1510;
    private static final int GREEN = 0xFF39FF88;
    private static final int MUTED = 0xFF79A98A;
    private static final int LINE = 0xFF1D5635;
    private TextView status, ipValue;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = text("A", 27, GREEN, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(rounded(0xFF102A1B, 14, GREEN));
        top.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        brand.addView(text("AKIRA REMOTE", 20, 0xFFE5FFEC, true));
        brand.addView(text("PHONE CONTROL STATION  /  v0.1", 10, MUTED, false));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = text("● LOCAL", 10, GREEN, true);
        live.setPadding(dp(9), dp(7), dp(9), dp(7));
        live.setBackground(rounded(0xFF102719, 20, LINE));
        top.addView(live);
        page.addView(top);
        page.addView(text("Управление трансляцией экрана", 17, 0xFFE5FFEC, true), params(-1,-2,0,22,0,0));

        LinearLayout statusCard = card(page, "СОСТОЯНИЕ СИСТЕМЫ", "SYSTEM STATUS  //  LIVE MONITOR");
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        status = text("● ГОТОВ К ЗАПУСКУ", 14, GREEN, true);
        statusRow.addView(status, new LinearLayout.LayoutParams(0,-2,1));
        statusRow.addView(text("WS  /  8765", 10, MUTED, true));
        statusCard.addView(statusRow);
        View divider = new View(this); divider.setBackgroundColor(LINE);
        statusCard.addView(divider, params(-1,1,0,14,0,12));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.HORIZONTAL);
        details.addView(metric("ПРОТОКОЛ", "WebSocket"), new LinearLayout.LayoutParams(0,-2,1));
        details.addView(metric("ПОРТ", "8765"), new LinearLayout.LayoutParams(0,-2,1));
        details.addView(metric("СЕТЬ", "Wi-Fi / LAN"), new LinearLayout.LayoutParams(0,-2,1));
        statusCard.addView(details);

        LinearLayout network = card(page, "АДРЕС УСТРОЙСТВА", "TARGET ADDRESS  //  SHARE WITH WATCH");
        ipValue = text(getLocalIp(), 22, GREEN, true); ipValue.setTextIsSelectable(true); network.addView(ipValue);
        network.addView(text("Телефон и часы должны быть подключены к одной Wi-Fi сети.",11,MUTED,false), params(-1,-2,0,7,0,0));
        Button copy = button("СКОПИРОВАТЬ IP", false); network.addView(copy, params(-1,44,0,14,0,0));
        copy.setOnClickListener(v -> { ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("AKIRA phone IP",getLocalIp())); Toast.makeText(this,"IP скопирован",Toast.LENGTH_SHORT).show(); });

        LinearLayout controls = card(page, "УПРАВЛЕНИЕ", "CONTROL DECK  //  USER ACTIONS");
        Button access = button("01   ВКЛЮЧИТЬ СЛУЖБУ УПРАВЛЕНИЯ", false); controls.addView(access,params(-1,50,0,0,0,9));
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button start = button("02   НАЧАТЬ ТРАНСЛЯЦИЮ ЭКРАНА", true); controls.addView(start,params(-1,54,0,0,0,9));
        start.setOnClickListener(v -> { MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); startActivityForResult(m.createScreenCaptureIntent(),REQUEST_CAPTURE); });
        Button stop = button("03   ОСТАНОВИТЬ ТРАНСЛЯЦИЮ", false); controls.addView(stop,params(-1,46,0,0,0,0));
        stop.setOnClickListener(v -> { Intent i=new Intent(this,CaptureService.class); i.setAction(CaptureService.ACTION_STOP); startService(i); setStatus("● ТРАНСЛЯЦИЯ ОСТАНОВЛЕНА",MUTED); });

        LinearLayout guide = card(page,"КАК ПОДКЛЮЧИТЬ ЧАСЫ","QUICK START  //  3 STEPS");
        guide.addView(step("01","Разреши службу управления","Открой настройки специальных возможностей и включи AKIRA Remote."));
        guide.addView(step("02","Запусти трансляцию","Нажми кнопку запуска и подтверди системный запрос Android."));
        guide.addView(step("03","Подключи часы","Введи IP телефона в приложении на часах и нажми CONNECT."));
        LinearLayout foot=new LinearLayout(this); foot.setGravity(Gravity.CENTER); foot.setPadding(0,dp(20),0,dp(4));
        foot.addView(text("AKIRA SYSTEMS   •   LOCAL NETWORK ONLY",9,MUTED,true)); page.addView(foot);
        setContentView(scroll);
    }
    private LinearLayout card(LinearLayout parent,String title,String sub) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(15),dp(16),dp(16)); box.setBackground(rounded(PANEL,18,LINE));
        parent.addView(box,params(-1,-2,0,0,0,14)); box.addView(text(title,13,0xFFE5FFEC,true)); box.addView(text(sub,9,MUTED,false),params(-1,-2,0,4,0,14)); return box;
    }
    private LinearLayout metric(String label,String value) { LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.addView(text(label,9,MUTED,true)); c.addView(text(value,12,0xFFE5FFEC,true),params(-1,-2,0,4,0,0)); return c; }
    private LinearLayout step(String n,String title,String desc) { LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.TOP); row.setPadding(0,dp(5),0,dp(10)); TextView num=text(n,12,GREEN,true); num.setPadding(0,0,dp(12),0); row.addView(num); LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.addView(text(title,12,0xFFE5FFEC,true)); words.addView(text(desc,10,MUTED,false),params(-1,-2,0,3,0,0)); row.addView(words,new LinearLayout.LayoutParams(0,-2,1)); return row; }
    private Button button(String label,boolean primary) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(11); b.setTypeface(Typeface.create("sans-serif-monospace",Typeface.BOLD)); b.setTextColor(primary?0xFF041009:GREEN); b.setBackgroundTintList(ColorStateList.valueOf(primary?GREEN:0xFF10251A)); return b; }
    private TextView text(String value,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(bold?Typeface.create("sans-serif-monospace",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); return t; }
    private GradientDrawable rounded(int color,int radius,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),stroke); return d; }
    private LinearLayout.LayoutParams params(int w,int h,int l,int t,int r,int b) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private void setStatus(String value,int color) { status.setText(value); status.setTextColor(color); }
    @Override protected void onActivityResult(int request,int result,Intent data) { super.onActivityResult(request,result,data); if(request==REQUEST_CAPTURE&&result==RESULT_OK&&data!=null){Intent i=new Intent(this,CaptureService.class);i.putExtra(CaptureService.EXTRA_RESULT_CODE,result);i.putExtra(CaptureService.EXTRA_DATA,data);startForegroundService(i);setStatus("● ТРАНСЛЯЦИЯ АКТИВНА",GREEN);}else if(request==REQUEST_CAPTURE)setStatus("● ДОСТУП К ЭКРАНУ НЕ ПОЛУЧЕН",0xFFFFB45E); }
    private String getLocalIp() { try { for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())) for(java.net.InetAddress a:Collections.list(ni.getInetAddresses())) if(!a.isLoopbackAddress()&&a instanceof Inet4Address)return a.getHostAddress(); }catch(Exception ignored){} return "IP не найден — проверь Wi-Fi"; }
}
