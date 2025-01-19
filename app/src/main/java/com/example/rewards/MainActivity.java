package com.example.rewards;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID2 = "load_queries_channel";
    private static final int NOTIFICATION_ID = 1;
    private boolean isLoadingQueries=false;
    private boolean isDesktop=false;
    private WebView webView;
    public ImageButton loadQueriesButton;
    private ImageButton imageButton;
    private ImageButton signOutButton;
    private int count;
    private ImageButton homeButton;
    private EditText searchCount;
    private String[] queryList;
    private int queryIndex = 0;
    private Handler handler = new Handler();
    public static MainActivity instance;
    public static MainActivity getInstance() {
        return instance;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        homeButton =findViewById(R.id.home);
        searchCount=findViewById(R.id.editTextText1);
        loadQueriesButton = findViewById(R.id.loadQueriesButton);
        signOutButton = findViewById(R.id.signOutButton);
        imageButton=findViewById(R.id.imageButton);
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://www.bing.com");
        homeButton.setOnClickListener(v -> goHome());
        signOutButton.setOnClickListener(v -> signOut());
        imageButton.setOnClickListener(v -> openBingInDesktopMode());
        loadQueriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadQueries();
            }
        });
        if (ContextCompat.checkSelfPermission(this, "com.example.rewards.DYNAMIC_RECEIVER_PERMISSION") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"com.example.rewards.DYNAMIC_RECEIVER_PERMISSION"}, 1);
        }
    }
    public void alarm(){
        instance=this;
        PeriodicWorkRequest loadQueriesWorkRequest =
                new PeriodicWorkRequest.Builder(LoadQueriesWorker.class, 12, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(this).enqueue(loadQueriesWorkRequest);
//          ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleWithFixedDelay(new Runnable() {
//            @Override
//            public void run() {
//                runOnUiThread(() -> loadQueriesButton.performClick());
//            }
//        }, 0, 24, TimeUnit.HOURS);
    }

    public void loadQueries() {
        try {
            count=Integer.parseInt(searchCount.getText().toString());
            if(!isLoadingQueries){
                isLoadingQueries=true;
            }
            InputStream inputStream = getAssets().open("query.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            if (line != null) {
                queryList = line.replace(" ", "+").split(",");
                sendNotification("Queries Started Loading", "queries Loading Has Started Successfully.");
                loadNextQuery();
                count++;
            } else {
                Toast.makeText(this, "No queries found in file.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error reading query file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("WebViewActivity", "Error reading file", e);
        }
    }

    private void loadNextQuery() {
        if (queryList != null && isLoadingQueries) {
            String query = queryList[queryIndex];
            String url = "https://www.bing.com/search?q=" + query + "&PC=U316&FORM=CHROMN#";
            if (isDesktop) {
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.launchUrl(this, Uri.parse(url));
            } else {
                webView.loadUrl(url);
            }
            queryIndex = (queryIndex + 1) % queryList.length;
            count--;
            if (count > 0) {
                handler.postDelayed(() -> loadNextQuery(), 7000);
            } else {
                isLoadingQueries = false;
                sendNotification("Queries Completed", "All queries have been loaded successfully.");
                goHome();
            }
        } else {
            isLoadingQueries = false;
            goHome();
            Toast.makeText(this, "All queries loaded or stopped.", Toast.LENGTH_SHORT).show();
        }
    }
    private void openBingInDesktopMode(){
        if(!isDesktop){
            isDesktop=true;
            Toast.makeText(this, "Desktop Mode Activated Click Start.", Toast.LENGTH_SHORT).show();
        }else{
            isDesktop=false;
            Toast.makeText(this, "Desktop Mode Deactivated.", Toast.LENGTH_SHORT).show();
        }
    }
    private void goHome(){
        isLoadingQueries=false;
        webView.clearHistory();
        webView.loadUrl("https://www.bing.com");
    }
    private void signOut() {
        // Clear cookies
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        isLoadingQueries=false;
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        webView.loadUrl("https://www.bing.com");
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {

            if (webView.copyBackForwardList().getCurrentIndex() <= 1) {
                isLoadingQueries = false;
                handler.removeCallbacksAndMessages(null);
                Toast.makeText(this, "Query loading stopped.", Toast.LENGTH_SHORT).show();
            }
                webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    private void sendNotification(String title, String message) {
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID2,
                    "Load Queries Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for loading queries");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID2)
                .setSmallIcon(R.drawable.baseline_notifications_active_24) // Replace with your app's icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}