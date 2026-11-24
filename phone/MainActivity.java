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
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.json.JSONObject;
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
    private FileTransferServer fileTransferServer;
    private TextView fileTransferStatus;
    private static final int REQUEST_FILE_PICK = 42;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        radar = new RadarBackground(this);
        scroll.setBackground(radar);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(page);

        // HEADER / BRAND
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("S", 25, GREEN, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(rounded(0xFF0B2416, 16, GREEN));
        top.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(11), 0, 0, 0);
        brand.addView(text("SYNKRON", 21, 0xFFE8FFF0, true));
        brand.addView(text("REMOTE  /  CONTROL NODE", 9, MUTED, true));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = text("● SYSTEM READY", 9, GREEN, true);
        live.setPadding(dp(9), dp(8), dp(9), dp(8));
        live.setBackground(rounded(0xFF0B1D12, 20, LINE));
        top.addView(live);
        page.addView(top);
        page.addView(text("PERSONAL REMOTE CONTROL SYSTEM", 9, MUTED, true), params(-1,-2,0,18,0,8));

        // CENTRAL REACTOR
        LinearLayout reactorCard = card(page, "SYNK CORE", "MAINFRAME  //  DEVICE CONTROL");
        CoreReactorView core = new CoreReactorView(this);
        reactorCard.addView(core, params(-1, 190, 0, 0, 0, 8));
        LinearLayout coreStats = new LinearLayout(this);
        coreStats.setGravity(Gravity.CENTER);
        coreStats.addView(metric("NODE", "PHONE"), new LinearLayout.LayoutParams(0,-2,1));
        coreStats.addView(metric("MODE", "LOCAL"), new LinearLayout.LayoutParams(0,-2,1));
        coreStats.addView(metric("PORT", "8765"), new LinearLayout.LayoutParams(0,-2,1));
        reactorCard.addView(coreStats);

        // SYSTEM STATUS
        LinearLayout statusCard = card(page, "SYSTEM STATUS", "LIVE MONITOR  //  PHONE NODE");
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        status = text("● READY TO TRANSMIT", 12, GREEN, true);
        statusRow.addView(status, new LinearLayout.LayoutParams(0,-2,1));
        statusRow.addView(text("WEBSOCKET / 8765", 9, MUTED, true));
        statusCard.addView(statusRow);
        View line = new View(this); line.setBackgroundColor(LINE);
        statusCard.addView(line, params(-1,1,0,12,0,10));
        LinearLayout stats = new LinearLayout(this);
        stats.addView(metric("PROTOCOL", "WS"), new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(metric("NETWORK", "WI-FI"), new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(metric("ROLE", "HOST"), new LinearLayout.LayoutParams(0,-2,1));
        statusCard.addView(stats);

        // CONTROL DECK
        LinearLayout controls = card(page, "CONTROL DECK", "STREAMING  //  ACCESSIBILITY");
        Button access = button("ENABLE CONTROL SERVICE", false);
        controls.addView(access, params(-1,48,0,0,0,8));
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button start = button("▶  START SCREEN STREAM", true);
        controls.addView(start, params(-1,52,0,0,0,8));
        start.setOnClickListener(v -> {
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(m.createScreenCaptureIntent(),REQUEST_CAPTURE);
        });
        Button stop = button("■  STOP SCREEN STREAM", false);
        controls.addView(stop, params(-1,46,0,0,0,0));
        stop.setOnClickListener(v -> {
            Intent i=new Intent(this,CaptureService.class); i.setAction(CaptureService.ACTION_STOP); startService(i);
            setStatus("● STREAM STOPPED",MUTED);
        });

        // DEVICE RADAR
        LinearLayout devices = card(page, "DEVICE RADAR", "NETWORK TOPOLOGY  //  CONNECTED NODES");
        addDeviceNode(devices, "01", "THIS PHONE", "HOST NODE  •  SCREEN SOURCE", true);
        addDeviceNode(devices, "02", "WINDOWS PC", "WAITING FOR CONNECTION  •  PORT 8766", false);
        addDeviceNode(devices, "03", "WEAR OS WATCH", "SCREEN RECEIVER  •  PORT 8765", false);

        // ADDRESS
        LinearLayout network = card(page, "NETWORK ADDRESS", "LOCAL LINK  //  SHARE WITH YOUR DEVICES");
        ipValue = text(getLocalIp(), 20, GREEN, true);
        ipValue.setTextIsSelectable(true);
        network.addView(ipValue);
        network.addView(text("Один Wi-Fi для локального подключения. Удалённый SYNKRON LINK будет отдельным режимом.",10,MUTED,false), params(-1,-2,0,7,0,0));
        Button copy = button("COPY IP ADDRESS", false);
        network.addView(copy, params(-1,44,0,12,0,0));
        copy.setOnClickListener(v -> {
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SYNKRON phone IP",getLocalIp()));
            Toast.makeText(this,"IP скопирован",Toast.LENGTH_SHORT).show();
        });

        // FILE TRANSFER
        LinearLayout files = card(page, "FILE TRANSFER", "DATA EXCHANGE  //  PHONE ⇄ WINDOWS");
        fileTransferStatus = text("● WAITING FOR WINDOWS  •  PORT 8767", 10, GREEN, true);
        files.addView(fileTransferStatus, params(-1,-2,0,0,0,12));
        Button sendPhoneFile = button("↑  SEND FILE TO WINDOWS", true);
        files.addView(sendPhoneFile, params(-1,50,0,0,0,8));
        sendPhoneFile.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("*/*"); pick.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick, REQUEST_FILE_PICK);
        });
        files.addView(text("Incoming files are saved in the app's SYNKRON-Received folder. Keep this app open during transfer.",10,MUTED,false));

        // WINDOWS SCREEN RECEIVER
        LinearLayout pcMirror = card(page, "PC SCREEN BEAM", "WINDOWS → PHONE  //  INCOMING DISPLAY");
        pcStreamStatus = text("● WAITING FOR PC  •  PORT 8766", 10, GREEN, true);
        pcMirror.addView(pcStreamStatus, params(-1,-2,0,0,0,10));
        pcPreview = new ImageView(this);
        pcPreview.setBackground(rounded(0xFF020604, 12, LINE));
        pcPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pcMirror.addView(pcPreview, params(-1,210,0,0,0,8));
        pcMirror.addView(text("Запусти PC → PHONE stream в Windows-клиенте. Оба устройства должны быть в одной Wi-Fi сети.",10,MUTED,false));

        LinearLayout guide = card(page, "QUICK START", "INITIALIZATION  //  3 STEPS");
        guide.addView(step("01","Разреши службу управления","Включи SYNKRON Remote в настройках специальных возможностей."));
        guide.addView(step("02","Запусти трансляцию","Подтверди системный запрос Android на захват экрана."));
        guide.addView(step("03","Подключи часы","Введи IP телефона в приложении на часах."));
        TextView footer = text("SYNKRON SYSTEMS  •  SECURE LOCAL SESSION", 9, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(16),0,dp(8));
        page.addView(footer);
        setContentView(scroll);
        startPcScreenServer();
        startFileTransferServer();
    }

    private void addDeviceNode(LinearLayout parent,String number,String title,String detail,boolean active) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10),dp(10),dp(10),dp(10));
        row.setBackground(rounded(0x6607130C,12,active?GREEN:LINE));
        TextView id = text(number,11,active?GREEN:MUTED,true);
        row.addView(id,params(30,-2,0,0,10,0));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title,11,0xFFE5FFEC,true));
        labels.addView(text(detail,8,MUTED,false),params(-1,-2,0,3,0,0));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(text(active?"● ONLINE":"○ STANDBY",8,active?GREEN:MUTED,true));
        parent.addView(row,params(-1,-2,0,0,0,8));
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
    @Override protected void onActivityResult(int request,int result,Intent data) { super.onActivityResult(request,result,data); if(request==REQUEST_FILE_PICK && result==RESULT_OK && data!=null) { sendPhoneFile(data.getData()); return; }
        if(request==REQUEST_CAPTURE&&result==RESULT_OK&&data!=null){Intent i=new Intent(this,CaptureService.class);i.putExtra(CaptureService.EXTRA_RESULT_CODE,result);i.putExtra(CaptureService.EXTRA_DATA,data);startForegroundService(i);setStatus("● ТРАНСЛЯЦИЯ АКТИВНА",GREEN);}else if(request==REQUEST_CAPTURE)setStatus("● ДОСТУП К ЭКРАНУ НЕ ПОЛУЧЕН",0xFFFFB45E); }

    private void startFileTransferServer() {
        try {
            fileTransferServer = new FileTransferServer(new InetSocketAddress("0.0.0.0", 8767));
            fileTransferServer.start();
        } catch (Exception e) {
            if (fileTransferStatus != null) fileTransferStatus.setText("ERR:// FILE LINK UNAVAILABLE");
        }
    }
    private void sendPhoneFile(Uri uri) {
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new IllegalStateException("Cannot open file");
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                byte[] bytes = out.toByteArray();
                String name = "phone-file";
                android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c != null) { try { int ix = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (ix >= 0 && c.moveToFirst()) name = c.getString(ix); } finally { c.close(); } }
                JSONObject meta = new JSONObject(); meta.put("type", "file"); meta.put("name", name); meta.put("size", bytes.length);
                boolean sent = false;
                if (fileTransferServer != null) for (WebSocket peer : fileTransferServer.getConnections()) {
                    if (peer.isOpen()) { peer.send(meta.toString()); peer.send(bytes); sent = true; }
                }
                final boolean ok = sent; final String sentName = name;
                runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText(ok ? "● ФАЙЛ ОТПРАВЛЕН  •  " + sentName : "ERR:// ПК НЕ ПОДКЛЮЧЁН"); });
            } catch (Exception e) {
                runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("ERR:// НЕ УДАЛОСЬ ПРОЧИТАТЬ ФАЙЛ"); });
            }
        }).start();
    }
    private class FileTransferServer extends WebSocketServer {
        private volatile String incomingName = "received-file";
        FileTransferServer(InetSocketAddress address) { super(address); setReuseAddr(true); }
        @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
            runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("● ПК ПОДКЛЮЧЁН  •  READY"); });
        }
        @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("● ОЖИДАНИЕ ПК  •  PORT 8767"); });
        }
        @Override public void onMessage(WebSocket conn, String message) {
            try { JSONObject m = new JSONObject(message); if ("file".equals(m.optString("type"))) incomingName = new File(m.optString("name", "received-file")).getName(); }
            catch (Exception ignored) { }
        }
        @Override public void onMessage(WebSocket conn, ByteBuffer bytes) {
            try {
                ByteBuffer copy = bytes.slice(); byte[] data = new byte[copy.remaining()]; copy.get(data);
                File dir = new File(getExternalFilesDir(null), "SYNKRON-Received"); if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, incomingName);
                try (FileOutputStream stream = new FileOutputStream(out)) { stream.write(data); }
                String saved = out.getName();
                runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("● ПОЛУЧЕНО: " + saved + "  //  SYNKRON-Received"); });
            } catch (Exception e) {
                runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("ERR:// SAVE FAILED"); });
            }
        }
        @Override public void onError(WebSocket conn, Exception ex) { runOnUiThread(() -> { if (fileTransferStatus != null) fileTransferStatus.setText("ERR:// FILE SOCKET"); }); }
        @Override public void onStart() { }
    }

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
        try { if (fileTransferServer != null) fileTransferServer.stop(500); } catch (Exception ignored) { }
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

final class CoreReactorView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator spin;
    private float angle=0f;
    private final float density;
    CoreReactorView(Context c) {
        super(c); density=c.getResources().getDisplayMetrics().density;
        spin=ValueAnimator.ofFloat(0f,360f); spin.setDuration(14000); spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new android.view.animation.LinearInterpolator());
        spin.addUpdateListener(a->{angle=(Float)a.getAnimatedValue();invalidate();});
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();spin.start();}
    @Override protected void onDetachedFromWindow(){spin.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); float cx=getWidth()/2f,cy=getHeight()/2f;
        float r=Math.min(getWidth(),getHeight())*.39f;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*density);p.setColor(0x5539FF88);
        c.drawCircle(cx,cy,r,p);c.drawCircle(cx,cy,r*.78f,p);c.drawCircle(cx,cy,r*.52f,p);
        p.setStrokeWidth(3*density);p.setColor(0xFF39FF88);
        RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);c.drawArc(oval,angle,92,false,p);
        p.setStrokeWidth(2*density);p.setColor(0xFF168A50);
        RectF inner=new RectF(cx-r*.78f,cy-r*.78f,cx+r*.78f,cy+r*.78f);c.drawArc(inner,-angle*1.4f,135,false,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0x2239FF88);c.drawCircle(cx,cy,r*.47f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1*density);p.setColor(0xFF39FF88);c.drawCircle(cx,cy,r*.47f,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xFFE5FFEC);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-monospace",Typeface.BOLD));p.setTextSize(13*density);
        c.drawText("SYNK CORE",cx,cy-2*density,p);
        p.setColor(0xFF39FF88);p.setTextSize(8*density);c.drawText("NODE // ACTIVE",cx,cy+15*density,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
}
