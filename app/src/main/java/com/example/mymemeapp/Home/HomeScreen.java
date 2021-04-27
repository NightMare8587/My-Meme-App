package com.example.mymemeapp.Home;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mymemeapp.MainActivity;
import com.example.mymemeapp.R;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class HomeScreen extends AppCompatActivity {
    RequestQueue requestQueue;
    ProgressBar progressBar;
    String my;
    Button share,next;
    URL URL;
    String uuid;
    LinearLayout linearLayout;
    ImageView imageView;
    AsyncTask myTask;
    String url = "https://meme-api.herokuapp.com/gimme";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        initialise();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    progressBar.setVisibility(View.INVISIBLE);
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray array = jsonObject.getJSONArray("preview");
                    my = (String) array.get(array.length()-1);

                    Glide.with(HomeScreen.this).load(my).into(imageView);
//                    Picasso.get().load(my).memoryPolicy(MemoryPolicy.NO_CACHE,MemoryPolicy.NO_STORE)
//                            .networkPolicy(NetworkPolicy.NO_CACHE,NetworkPolicy.NO_STORE).into(imageView);
                    linearLayout.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                Log.i("resp",response.toString()+"");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                loadMeme();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                uuid = UUID.randomUUID().toString();
                Log.d("id", uuid+"");
                View content = findViewById(R.id.loadMemeImage);
                content.setDrawingCacheEnabled(true);

                Bitmap bitmap = content.getDrawingCache();
                File root = Environment.getExternalStorageDirectory();
                File cachePath = new File(root.getAbsolutePath() + "/DCIM/Camera/" + uuid + ".jpg");
                try {
                    cachePath.createNewFile();
                    FileOutputStream ostream = new FileOutputStream(cachePath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                    ostream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cachePath));
                startActivity(Intent.createChooser(share,"Share via"));

            }

        });

        requestQueue.add(jsonObjectRequest);
    }



    private void loadMeme(){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    progressBar.setVisibility(View.INVISIBLE);
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray array = jsonObject.getJSONArray("preview");
                    my = (String) array.get(array.length()-1);
                    Glide.with(HomeScreen.this).load(my).into(imageView);
//                    Picasso.get().load(my).memoryPolicy(MemoryPolicy.NO_CACHE)
//                            .networkPolicy(NetworkPolicy.NO_CACHE).into(imageView);
                    linearLayout.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                Log.i("resp",response.toString()+"");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void initialise() {
        requestQueue = Volley.newRequestQueue(this);
        progressBar = findViewById(R.id.progressBar);
        imageView = findViewById(R.id.loadMemeImage);
        linearLayout  = findViewById(R.id.myLinearLatyout);
        share = findViewById(R.id.shareThisMeme);
        next = findViewById(R.id.showNextMeme);
    }
}