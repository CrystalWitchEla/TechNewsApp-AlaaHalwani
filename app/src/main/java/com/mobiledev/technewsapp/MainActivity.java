package com.mobiledev.technewsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    ArrayList<String> titles;
    ArrayList<String> contents;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titles = new ArrayList<String>();
        contents = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        ListView headlines = (ListView) findViewById(R.id.headlines);
        headlines.setAdapter(arrayAdapter);

        headlines.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ArticleView.class);
                intent.putExtra("content", contents.get(position));
                startActivity(intent);

            }
        });

        db = this.openOrCreateDatabase("mobiledb", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS news_articles(article_id INTEGER PRIMARY KEY, " +
                "article_title VARCHAR, article_content VARCHAR)");

        LoadHTML loadHTML = new LoadHTML();
        try
        {
           // loadHTML.execute("https://hacker-news.firebaseio.com/v0/topstories.json");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        addToList();
    }

    public class LoadHTML extends AsyncTask<String, Void, String>
    {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection connection;

            try
            {
                url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();

                InputStream is = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(is);

                int data = reader.read();

                while(data != -1)
                {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray ids = new JSONArray(result);

                db.execSQL("DELETE FROM news_articles");
                for (int i = 0; i < 20; i++)
                {
                    String artID = ids.getString(i);
                    String artInfo = "";

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + artID +
                            ".json?print=pretty");
                    connection = (HttpURLConnection) url.openConnection();

                    is = connection.getInputStream();
                    reader = new InputStreamReader(is);

                    data = reader.read();

                    while(data != -1)
                    {
                        char current = (char) data;
                        artInfo += current;
                        data = reader.read();
                    }

                    JSONObject article = new JSONObject(artInfo);

                    String artTitle = article.getString("title");
                    String artURL = article.getString("url");

                    Log.i("title", artTitle);
                    Log.i("url", artURL);
                    url = new URL(artURL);

                    StringBuilder sb = new StringBuilder();

                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

                    String inputLine;

                    while ((inputLine = br.readLine()) != null) {
                        sb.append(inputLine);
                    }

                    br.close();

                    String artContent = sb.toString();

                    Log.i("content", artContent);

                    String insert = "INSERT INTO news_articles(article_id, article_title, article_content) VALUES(?,?,?)";
                    SQLiteStatement q = db.compileStatement(insert);

                    q.bindString(1, artID);
                    q.bindString(2, artTitle);
                    q.bindString(3, artContent);

                    q.execute();

                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            addToList();
        }
    }

    public void addToList()
    {
        Cursor c = db.rawQuery("SELECT * FROM news_articles", null);
        int titleIndex = c.getColumnIndex("article_title");
        int contentIndex = c.getColumnIndex("article_content");
        if(c.moveToFirst())
        {
            titles.clear();
            contents.clear();

            do
            {
                titles.add(c.getString(titleIndex));
                contents.add(c.getString(contentIndex));
//                Log.i("title",c.getString(titleIndex));
//                Log.i("content",c.getString(contentIndex));
            } while (c.moveToNext());
        }

        arrayAdapter.notifyDataSetChanged();

    }
}