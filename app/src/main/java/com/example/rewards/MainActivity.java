package com.example.rewards;


import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private boolean isLoadingQueries=false;
    private WebView webView;
    private Button loadQueriesButton;
    private Button signOutButton;
    private int count;
    private ImageView homeButton;
    private EditText searchCount;
    private String[] queryList;
    private int queryIndex = 0;
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        homeButton =findViewById(R.id.home);
        homeButton.setOnClickListener(v -> goHome());
        searchCount=findViewById(R.id.editTextText1);



        webView = findViewById(R.id.webView);
        loadQueriesButton = findViewById(R.id.loadQueriesButton);
        signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> signOut());

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // Load the URL
        webView.loadUrl("https://www.bing.com");

        // Load queries when button is clicked
        loadQueriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadQueries();
            }
        });
    }

    private void loadQueries() {
        count=Integer.parseInt(searchCount.getText().toString());
        try {
            if(!isLoadingQueries){
                isLoadingQueries=true;
            }
            // Read the queries from the query.txt file in the assets folder
            InputStream inputStream = getAssets().open("query.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            if (line != null) {
                queryList = line.replace(" ", "+").split(",");
                queryIndex = 0; // Reset query index
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
        if (  (queryIndex+1)!=count && queryList != null && queryIndex < queryList.length && isLoadingQueries) {
            String query = queryList[queryIndex];
            webView.setWebViewClient(new WebViewClient());
            String url = "https://www.bing.com/search?q=" + query + "&PC=U316&FORM=CHROMN#";
            webView.loadUrl(url);
            queryIndex++;

            // Load the next query after a delay
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadNextQuery();
                }
            }, 7000); // 1-second delay
        } else {
            isLoadingQueries=false;
            goHome();
            Toast.makeText(this, "All queries loaded or stopped.", Toast.LENGTH_SHORT).show();
        }


    }
    private void goHome(){
        isLoadingQueries=false;
        webView.loadUrl("https://www.bing.com");
    }
    private void signOut() {
        // Clear cookies
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        isLoadingQueries=false;
        // Clear cache and history
        webView.clearCache(true);
        webView.clearHistory();

        // Optionally clear form data
        webView.clearFormData();

        // Navigate to the sign-in page or a desired URL
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
            // Close the activity if there's no history
            super.onBackPressed();
        }
    }
}