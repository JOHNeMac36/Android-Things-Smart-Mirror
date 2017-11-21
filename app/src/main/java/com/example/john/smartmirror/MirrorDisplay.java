    package com.example.john.smartmirror;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.johnhiott.darkskyandroidlib.ForecastApi;
import com.johnhiott.darkskyandroidlib.RequestBuilder;
import com.johnhiott.darkskyandroidlib.models.Request;
import com.johnhiott.darkskyandroidlib.models.WeatherResponse;
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.content.ContentValues.TAG;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MirrorDisplay extends Activity {

    final Handler handler = new Handler();
    TextView time_text_view;
    TextView dow_text_view;
    TextView date_text_view;
    ImageView weather_icon_image_view;
    TextView news_label_text_view;
    TextView news_text_view;
    TextView current_weather_text_view,
            temperature_text_view,
            forecast_text_view,
            location_text_view;

    Timer timer;
    Runnable get_weather = new Runnable() {
        String location = "";

        @Override
        public void run() {
            final String ip = "150.250.125.232";
            String longitude = "", latitude = "";
            String url = "http://freegeoip.net/json/";
            InputStream is = null;
            try {
                is = new URL(url).openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String jsonText = "";
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                longitude = json.getString("longitude");
                latitude = json.getString("latitude");
                location = json.getString("city") + ", " + json.getString("region_code");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {

            } finally {
            }
            RequestBuilder weather = new RequestBuilder();

            Request request = new Request();
            Log.d(TAG, "long = " + longitude);
            Log.d(TAG, "lat = " + latitude);

            request.setLat(latitude);
            request.setLng(longitude);
            request.setUnits(Request.Units.US);
            request.setLanguage(Request.Language.ENGLISH);
            final String finalJsonText = jsonText;
            weather.getWeather(request, new Callback<WeatherResponse>() {
                @Override
                public void success(WeatherResponse weatherResponse, Response response) {
                    Log.d(TAG, "Weather Currently: " + weatherResponse.getCurrently().getApparentTemperature());
                    call_updateWeather(weatherResponse, response);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.d(TAG, "Error while calling: " + retrofitError.getUrl());
                }
            });

        }

        private void call_updateWeather(WeatherResponse weatherResponse, Response response) {
            updateWeather(weatherResponse, response, location);
        }
    };
    Runnable ui_thread = new Runnable() {
        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                synchronized (this) {
                    wait(500);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Date currentTime = Calendar.getInstance().getTime();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    updateTime(currentTime);
                                    updateDate(currentTime);
                                }
                            }, 100, 1000);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    Thread thr = new Thread(get_weather);
                                    thr.start();
                                }
                            }, 100, 3600000);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    updateNews();
                                }
                            }, 100, 60000);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(MirrorDisplay.this, CalendarDisplay.class);
                                    startActivity(intent);
                                    finish();                                }
                            }, 6000);
                        }
                    });

                }
            } catch (
                    InterruptedException e)

            {
                e.printStackTrace();
            }
        }
    };
    Runnable mTask = new Runnable() {
        public void run() {
            // just sleep for 30 seconds.
            try {
                Thread.sleep(100);

                runOnUiThread(ui_thread);

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ForecastApi.create("83afd8c71153c1996fb93ff670b33366");
        setContentView(R.layout.activity_mirror_display);

        time_text_view = (TextView) findViewById(R.id.time_text_view);
        dow_text_view = (TextView) findViewById(R.id.dow_text_view);
        date_text_view = (TextView) findViewById(R.id.date_text_view);
        weather_icon_image_view = (ImageView) findViewById(R.id.weather_icon_image_view);
        news_label_text_view = (TextView) findViewById(R.id.news_label_text_view);
        news_text_view = (TextView) findViewById(R.id.news_text_view);

        current_weather_text_view = (TextView) findViewById(R.id.current_weather_text_view);
        temperature_text_view = (TextView) findViewById(R.id.temperature_text_view);
        forecast_text_view = (TextView) findViewById(R.id.forecast_text_view);
        location_text_view = (TextView) findViewById(R.id.location_text_view);

        Typeface helvetica = Typeface.createFromAsset(getAssets(), "fonts/Helvetica-Regular.ttf");
        time_text_view.setTypeface(helvetica);
        dow_text_view.setTypeface(helvetica);
        date_text_view.setTypeface(helvetica);
        news_label_text_view.setTypeface(helvetica);
        news_text_view.setTypeface(helvetica);
        current_weather_text_view.setTypeface(helvetica);
        temperature_text_view.setTypeface(helvetica);
        forecast_text_view.setTypeface(helvetica);
        location_text_view.setTypeface(helvetica);

        timer = new Timer();
        Thread thr = new Thread(ui_thread);
        thr.start();
    }

    private void updateTime(final Date currentTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DateFormat format = new SimpleDateFormat("h:mm a");
                TextView time_text_view = (TextView) findViewById(R.id.time_text_view);
                time_text_view.setText(format.format(currentTime));
            }
        });
    }

    private void updateDate(final Date currentTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView date_text_view = (TextView) findViewById(R.id.date_text_view);
                TextView dow_text_view = (TextView) findViewById(R.id.dow_text_view);
                DateFormat format = new SimpleDateFormat("MMM d, YYYY");
                format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                date_text_view.setText(format.format(currentTime));
                format = new SimpleDateFormat("EEEE");
                dow_text_view.setText(format.format(currentTime));
            }
        });
    }

    private void updateNews() {
        String urlString = "https://news.google.com/news?ned=us&output=rss";
        final Parser parser = new Parser();
        parser.onFinish(new Parser.OnTaskCompleted() {
            @Override
            public void onTaskCompleted(ArrayList<Article> list) {
                Log.d(TAG, "News change called");
                String headlines = "", headline, publisher;
                for (int i = 0; i < 5; i++) {
                    headline = list.get(i + 1).getTitle();
                    publisher = headline.substring(headline.indexOf(" - "), headline.length());
                    headline = headline.substring(0, headline.indexOf(" - "));
                    final int MAX_NEWS_TITLE_LEN = 50, MAX_PUBLISHER_LEN = 20;
                    if (publisher.length() >= MAX_NEWS_TITLE_LEN) {
                        publisher = publisher.substring(0, MAX_PUBLISHER_LEN);
                    }
                    if (headline.length() >= MAX_NEWS_TITLE_LEN) {
                        headline = headline.substring(0, MAX_NEWS_TITLE_LEN - 3) + "...";
                    }
                    headlines += headline + publisher + "\n";
                }
                final String finalHeadlines = headlines;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Headlines = " + finalHeadlines);
                        TextView news_text_view = (TextView) findViewById(R.id.news_text_view);
                        news_text_view.setText(finalHeadlines);
                    }
                });
            }

            @Override
            public void onError() {
                Log.d(TAG, "onError*() Called");
            }
        });
        Log.d(TAG, "Parser to execute");
        parser.execute(urlString);
        Log.d(TAG, "Parser executed");

    }

    private void updateWeather(final WeatherResponse weatherResponse, Response response, final String location) {
        final int temp = (int) weatherResponse.getCurrently().getApparentTemperature();
        final String current_summary = weatherResponse.getCurrently().getSummary();
        final String forecast_sumary = weatherResponse.getHourly().getSummary();
        final String weather_icon = weatherResponse.getCurrently().getIcon();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView weather_icon_image_view = (ImageView) findViewById(R.id.weather_icon_image_view);
                TextView current_weather_text_view = (TextView) findViewById(R.id.current_weather_text_view);
                TextView forecast_text_view = (TextView) findViewById(R.id.forecast_text_view);
                TextView location_text_view = (TextView) findViewById(R.id.location_text_view);
                TextView temperature_text_view = (TextView) findViewById(R.id.temperature_text_view);

                switch (weather_icon) {
                    case "clear-day":
                        weather_icon_image_view.setImageResource(R.drawable.sun);
                        break;
                    case "wind":
                        weather_icon_image_view.setImageResource(R.drawable.wind);
                        break;
                    case "cloudy":
                        weather_icon_image_view.setImageResource(R.drawable.cloud);
                        break;
                    case "partly-cloudy-day":
                        weather_icon_image_view.setImageResource(R.drawable.partlysunny);
                        break;
                    case "rain":
                        weather_icon_image_view.setImageResource(R.drawable.rain);
                        break;
                    case "snow":
                        weather_icon_image_view.setImageResource(R.drawable.snow);
                        break;
                    case "snow-thin":
                        weather_icon_image_view.setImageResource(R.drawable.snow);
                        break;
                    case "fog":
                        weather_icon_image_view.setImageResource(R.drawable.haze);
                        break;
                    case "clear-night":
                        weather_icon_image_view.setImageResource(R.drawable.moon);
                        break;
                    case "partly-cloudy-night":
                        weather_icon_image_view.setImageResource(R.drawable.partlymoon);
                        break;
                    case "thunderstorm":
                        weather_icon_image_view.setImageResource(R.drawable.storm);
                        break;
                    case "tornado":
                        weather_icon_image_view.setImageResource(R.drawable.tornado);
                        break;
                    case "hail":
                        weather_icon_image_view.setImageResource(R.drawable.hail);
                        break;
                }

                current_weather_text_view.setText(current_summary);
                forecast_text_view.setText(forecast_sumary);
                location_text_view.setText(location);
                temperature_text_view.setText("" + temp + '°');
            }
        });
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static void rotateScreen(final RelativeLayout layout, final Activity activity) {

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int w = size.x;
        int h = size.y;

        layout.setRotation(90.0f);
        layout.setTranslationX((w - h) / 2);
        layout.setTranslationY((h - w) / 2);

        ViewGroup.LayoutParams lp = layout.getLayoutParams();
        lp.height = w;
        lp.width = h;
        layout.setLayoutParams(lp);
        layout.requestLayout();
    }
}

