package br.com.cicero.wms;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * WMS Cícero Papelaria: abre o WMS (o mesmo do navegador) em tela cheia.
 * O conteúdo vem do site, então toda atualização do WMS já aparece aqui sem reinstalar o APK.
 */
public class MainActivity extends Activity {

    private static final String URL_WMS = "https://inventario-cicero.netlify.app/";
    private static final String HOST_WMS = "inventario-cicero.netlify.app";
    private static final int PEDIDO_CAMERA = 1;

    private WebView web;
    private View telaErro;
    private ProgressBar carregando;
    private PermissionRequest camPendente;
    private long ultimoVoltar = 0;
    private boolean deuErro = false;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        // coletor não apaga a tela no meio da separação
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#1F4FA3"));

        FrameLayout raiz = new FrameLayout(this);
        web = new WebView(this);
        raiz.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        carregando = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        carregando.setIndeterminate(true);
        FrameLayout.LayoutParams lpBarra = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8);
        lpBarra.gravity = Gravity.TOP;
        raiz.addView(carregando, lpBarra);

        telaErro = montarTelaErro();
        telaErro.setVisibility(View.GONE);
        raiz.addView(telaErro, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);           // o WMS guarda o login no aparelho
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);                     // fonte grande do sistema não quebra as telas
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        String versao = "1";
        try { versao = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        s.setUserAgentString(s.getUserAgentString() + " WMSCiceroApp/" + versao);
        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST_WMS.equalsIgnoreCase(u.getHost())) return false;
                abrirFora(u);                    // link de fora do WMS abre no navegador
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap icone) {
                deuErro = false;
                carregando.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                carregando.setVisibility(View.GONE);
                if (!deuErro) telaErro.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    deuErro = true;
                    telaErro.setVisibility(View.VISIBLE);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(() -> {
                    boolean querCamera = false;
                    for (String r : req.getResources()) if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) querCamera = true;
                    if (!querCamera) { req.deny(); return; }
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        camPendente = req;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PEDIDO_CAMERA);
                    }
                });
            }
        });

        web.setDownloadListener((url, ua, disp, tipo, tam) -> {
            if (url != null && url.startsWith("http")) abrirFora(Uri.parse(url));
            else Toast.makeText(this, "Para baixar arquivos, use o WMS no computador.", Toast.LENGTH_LONG).show();
        });

        if (salvo != null) web.restoreState(salvo);
        else web.loadUrl(URL_WMS);
    }

    private View montarTelaErro() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Color.WHITE);
        l.setPadding(48, 48, 48, 48);
        TextView t = new TextView(this);
        t.setText("Sem conexão com o WMS");
        t.setTextSize(22);
        t.setTextColor(Color.parseColor("#1F4FA3"));
        t.setGravity(Gravity.CENTER);
        TextView t2 = new TextView(this);
        t2.setText("Confira o Wi-Fi do coletor e toque em Tentar de novo.");
        t2.setTextSize(16);
        t2.setTextColor(Color.DKGRAY);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 16, 0, 32);
        Button b = new Button(this);
        b.setText("Tentar de novo");
        b.setOnClickListener(v -> {
            telaErro.setVisibility(View.GONE);
            String atual = web.getUrl();
            if (atual == null || atual.startsWith("about:") || atual.startsWith("data:")) web.loadUrl(URL_WMS);
            else web.reload();
        });
        l.addView(t);
        l.addView(t2);
        l.addView(b);
        return l;
    }

    private void abrirFora(Uri u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
        catch (Exception e) { Toast.makeText(this, "Não há navegador para abrir este link.", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    public void onRequestPermissionsResult(int cod, String[] perms, int[] res) {
        super.onRequestPermissionsResult(cod, perms, res);
        if (cod != PEDIDO_CAMERA || camPendente == null) return;
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) camPendente.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        else camPendente.deny();
        camPendente = null;
    }

    @Override
    public void onBackPressed() {
        // voltar dentro do WMS; na tela inicial, precisa tocar duas vezes para sair
        if (telaErro.getVisibility() != View.VISIBLE && web.canGoBack()) { web.goBack(); return; }
        long agora = System.currentTimeMillis();
        if (agora - ultimoVoltar < 2000) { super.onBackPressed(); return; }
        ultimoVoltar = agora;
        Toast.makeText(this, "Toque em voltar de novo para sair do WMS", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle s) {
        super.onSaveInstanceState(s);
        web.saveState(s);
    }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }

    @Override
    protected void onPause() { web.onPause(); super.onPause(); }
}
