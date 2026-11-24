package com.akira.remote;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.animation.ValueAnimator;
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
import android.widget.ImageView;
import android.widget.Toast;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.util.Collections;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 41;
    private static final int BG = 0xFF050907;
    private static final int PANEL = 0xFF0A1510;
    private static final int GREEN = 0xFF39FF88;
    private static final int MUTED = 0xFF79A98A;
    private static final int LINE = 0xFF1D5635;
    private TextView status, ipValue;
    private RadarBackground radar;
    private ImageView pcPreview;
    private TextView pcStreamStatus;
    private PcScreenServer pcScreenServer;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        radar = new RadarBackground(this);
        scroll.setBackground(radar);
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

        LinearLayout pcMirror = card(page, "ЭКРАН WINDOWS", "PC → PHONE  //  LAN SCREEN RECEIVER");
        pcStreamStatus = text("● ОЖИДАНИЕ ПК  •  PORT 8766", 12, GREEN, true);
        pcMirror.addView(pcStreamStatus, params(-1, -2, 0, 0, 0, 10));
        pcPreview = new ImageView(this);
        pcPreview.setBackground(rounded(0xFF020604, 12, LINE));
        pcPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pcMirror.addView(pcPreview, params(-1, 220, 0, 0, 0, 8));
        pcMirror.addView(text("Открой AKIRA REMOTE на Windows, введи IP телефона и нажми START PC STREAM. Оба устройства должны быть в одной Wi-Fi сети.", 10, MUTED, false), params(-1, -2, 0, 4, 0, 0));

        LinearLayout guide = card(page,"КАК ПОДКЛЮЧИТЬ ЧАСЫ","QUICK START  //  3 STEPS");
        guide.addView(step("01","Разреши службу управления","Открой настройки специальных возможностей и включи AKIRA Remote."));
        guide.addView(step("02","Запусти трансляцию","Нажми кнопку запуска и подтверди системный запрос Android."));
        guide.addView(step("03","Подключи часы","Введи IP телефона в приложении на часах и нажми CONNECT."));
        LinearLayout foot=new LinearLayout(this); foot.setGravity(Gravity.CENTER); foot.setPadding(0,dp(20),0,dp(4));
        foot.addView(text("AKIRA SYSTEMS   •   LOCAL NETWORK ONLY",9,MUTED,true)); page.addView(foot);
        setContentView(scroll);
        startPcScreenServer();
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
    private void startPcScreenServer() {
        try {
            pcScreenServer = new PcScreenServer(new InetSocketAddress("0.0.0.0", 8766));
            pcScreenServer.start();
        } catch (Exception e) {
            if (pcStreamStatus != null) pcStreamStatus.setText("ERR:// PORT 8766 UNAVAILABLE");
        }
    }
    private class PcScreenServer extends WebSocketServer {
        PcScreenServer(InetSocketAddress address) { super(address); setReuseAddr(true); }
        @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
            runOnUiThread(() -> { if (pcStreamStatus != null) pcStreamStatus.setText("● ПК ПОДКЛЮЧЁН  •  RX ACTIVE"); });
        }
        @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            runOnUiThread(() -> { if (pcStreamStatus != null) pcStreamStatus.setText("● ОЖИДАНИЕ ПК  •  PORT 8766"); });
        }
        @Override public void onMessage(WebSocket conn, String message) { }
        @Override public void onMessage(WebSocket conn, ByteBuffer bytes) {
            try {
                ByteBuffer copy = bytes.slice();
                byte[] data = new byte[copy.remaining()];
                copy.get(data);
                Bitmap frame = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (frame != null) runOnUiThread(() -> {
                    if (pcPreview != null) pcPreview.setImageBitmap(frame);
                    if (pcStreamStatus != null) pcStreamStatus.setText("● ПРИЁМ ЭКРАНА WINDOWS  •  " + frame.getWidth() + "×" + frame.getHeight());
                });
            } catch (Exception ignored) { }
        }
        @Override public void onError(WebSocket conn, Exception ex) {
            runOnUiThread(() -> { if (pcStreamStatus != null) pcStreamStatus.setText("ERR:// PC STREAM SOCKET"); });
        }
        @Override public void onStart() { }
    }
    @Override protected void onDestroy() {
        try { if (pcScreenServer != null) pcScreenServer.stop(500); } catch (Exception ignored) { }
        if (radar != null) radar.stop();
        super.onDestroy();
    }
    private String getLocalIp() { try { for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())) for(java.net.InetAddress a:Collections.list(ni.getInetAddresses())) if(!a.isLoopbackAddress()&&a instanceof Inet4Address)return a.getHostAddress(); }catch(Exception ignored){} return "IP не найден — проверь Wi-Fi"; }
}


final class RadarBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final ValueAnimator animator;
    private float angle = 0f;
    RadarBackground(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(18000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(a -> { angle = (Float)a.getAnimatedValue(); invalidateSelf(); });
        animator.start();
    }
    @Override public void draw(Canvas canvas) {
        RectF b = new RectF(getBounds());
        float cx = b.width() * 0.82f, cy = b.height() * 0.17f;
        float r = Math.min(b.width(), b.height()) * 0.30f;
        int save = canvas.save();
        canvas.rotate(angle, cx, cy);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.15f * density);
        paint.setColor(0x1839FF88);
        for (int i=1;i<=4;i++) canvas.drawCircle(cx,cy,r*i/4f,paint);
        paint.setColor(0x2639FF88);
        paint.setStrokeWidth(2f*density);
        RectF arc = new RectF(cx-r,cy-r,cx+r,cy+r);
        canvas.drawArc(arc,18,72,false,paint);
        canvas.drawArc(arc,152,34,false,paint);
        canvas.drawArc(arc,248,58,false,paint);
        paint.setStrokeWidth(1f*density);
        paint.setColor(0x3039FF88);
        for(int i=0;i<48;i++) {
            double a=Math.toRadians(i*7.5);
            float x1=cx+(float)Math.cos(a)*(r-5*density), y1=cy+(float)Math.sin(a)*(r-5*density);
            float x2=cx+(float)Math.cos(a)*(r+3*density), y2=cy+(float)Math.sin(a)*(r+3*density);
            canvas.drawLine(x1,y1,x2,y2,paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x1C39FF88);
        canvas.drawCircle(cx,cy,3*density,paint);
        canvas.restoreToCount(save);
    }
    void stop() { animator.cancel(); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
