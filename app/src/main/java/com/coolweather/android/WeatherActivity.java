package com.coolweather.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;

import android.support.v4.widget.SwipeRefreshLayout;

import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.ActivityCollector;
import com.coolweather.android.util.AppHelper;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;
import com.coolweather.android.util.WeatherPagerAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends BaseActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private static final String TAG = "WeatherActivity";
    private ImageView bingPicImg;
    private List<View> viewList;
    private WeatherPagerAdapter weatherPagerAdapter;
    private ViewPager viewPager;
    private Button addButton;
    private Button delButton;

    private List<String> weatherIdList = new ArrayList<>();

    private static int ADD_CITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        initView();

        Intent intent = getIntent();
        String weatherId = intent.getStringExtra("weather_id");
        if (!"likexin".equals(weatherId)) {
            requestWeather(weatherId, true);
        } else {
            SharedPreferences preferences1 = PreferenceManager.getDefaultSharedPreferences(this);
            String data=preferences1.getString("weatherId",null);
            Gson gson=new Gson();
            Type listType=new TypeToken<List<String>>(){}.getType();
            weatherIdList=gson.fromJson(data,listType);

            if (weatherIdList == null) {
                Intent intent1 = new Intent(this, MainActivity.class);
                startActivityForResult(intent1, ADD_CITY);
            } else {
                for (int i = 0; i < weatherIdList.size(); i++) {
                    String weatherIdItem=weatherIdList.get(i);
                    if (weatherIdItem != null)
                        requestWeather(weatherIdItem, true);
                    else {
                        Intent intent2 = new Intent(this, MainActivity.class);
                        Toast.makeText(WeatherActivity.this, "获取天气缓存失败，请手动选择", Toast.LENGTH_SHORT).show();
                        weatherIdList.clear();
                        startActivityForResult(intent2, ADD_CITY);
                        break;
                    }
                }

                viewPager.setCurrentItem(viewList.size() - 1);
            }

        }

        //添加背景图
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = preferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

    }

    private void initView() {
        //初始化控件：
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        bingPicImg = findViewById(R.id.bing_pic_img);
        addButton = findViewById(R.id.add_bt);
        delButton=findViewById(R.id.del_bt);
        viewPager = findViewById(R.id.view_pager);
        viewList = new ArrayList<>();
        weatherPagerAdapter = new WeatherPagerAdapter(viewList);
        viewPager.setAdapter(weatherPagerAdapter);



        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this, MainActivity.class);
                startActivityForResult(intent, ADD_CITY);
            }
        });

        delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePage();
            }
        });

        swipeRefreshLayout.setDistanceToTriggerSync(700);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                int position = viewPager.getCurrentItem();
                requestWeather(weatherIdList.get(position),false);
                loadBingPic();

            }
        });
    }

    private void DeletePage() {
        AlertDialog.Builder builder=new AlertDialog.Builder(WeatherActivity.this);
        final int position=viewPager.getCurrentItem();
        View view=viewList.get(position);
        TextView tv=view.findViewById(R.id.title_city);
        builder.setTitle(tv.getText().toString());
        builder.setMessage("确定删除?");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewList.remove(position);
                weatherIdList.remove(position);
                weatherPagerAdapter.notifyDataSetChanged();
                if(viewList.size()==0){
                    Intent intent = new Intent(WeatherActivity.this, MainActivity.class);
                    startActivityForResult(intent, ADD_CITY);
                }
                else {
                    viewPager.setCurrentItem(position-1,true);
                }
            }
        });
        builder.setNegativeButton("点错了",null);
        builder.show();

    }


    /*处理返回的weatherID*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String weatherId = data.getStringExtra("weather_id");
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK)
                    requestWeather(weatherId, true);
                else if (resultCode == RESULT_CANCELED)
                    Toast.makeText(WeatherActivity.this, "未选择位置信息！", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void requestWeather(final String weatherId, final boolean isAdd) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (weather != null && "ok".equals(weather.status)) {

                            if (isAdd) {
                                weatherIdList.add(weatherId);
                                addCity(weather);
                                viewPager.setCurrentItem(viewList.size() - 1);
                            } else {
                                refreshCurrentWeather(weather);
                            }
                        } else {
                            if (weather == null)
                                Log.i(TAG, "run: ddd");
                            Toast.makeText(WeatherActivity.this, "获取天气数据失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气数据失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

            }
        });

    }

    private void addCity(Weather weather) {

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.weather_layout, null);


        TextView titleCity;
        TextView titleUpdateTime;
        TextView degreeText;
        TextView weatherInfoText;
        LinearLayout forecastLayout;
        TextView aqiText;
        TextView pm25Text;
        TextView comfortText;
        TextView carWashText;
        TextView sportText;

        titleCity = view.findViewById(R.id.title_city);
        titleUpdateTime = view.findViewById(R.id.title_update_time);
        degreeText = view.findViewById(R.id.degree_text);
        weatherInfoText = view.findViewById(R.id.weather_info_text);
        forecastLayout = view.findViewById(R.id.forecast_layout);
        aqiText = view.findViewById(R.id.aqi_text);
        pm25Text = view.findViewById(R.id.pm25_text);
        comfortText = view.findViewById(R.id.comfort_text);
        carWashText = view.findViewById(R.id.car_wash_text);
        sportText = view.findViewById(R.id.sport_text);

        /* *//*先设置控件不可见，等信息加载完成再显示出来*//*
        weatherLayout.setVisibility(View.INVISIBLE);*/

        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "摄氏度";
        String weatherInfo = weather.now.more.info;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);


        forecastLayout.removeAllViews();

        for (Forecast forecast : weather.forecastList) {

            View view1 = View.inflate(this, R.layout.forecast_item, null);


            TextView dateText = view1.findViewById(R.id.date_text);
            TextView infoText = view1.findViewById(R.id.info_text);
            TextView maxText = view1.findViewById(R.id.max_text);
            TextView minText = view1.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);

            forecastLayout.addView(view1, -1);
        }

        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        viewList.add(view);
        weatherPagerAdapter.notifyDataSetChanged();

    }

    private void refreshCurrentWeather(Weather weather) {

        if (weather == null) {
            Toast.makeText(WeatherActivity.this, "天气更新失败", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        View view = viewList.get(viewPager.getCurrentItem());
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "摄氏度";
        String weatherInfo = weather.now.more.info;

        TextView titleCity;
        TextView titleUpdateTime;
        TextView degreeText;
        TextView weatherInfoText;
        LinearLayout forecastLayout;
        TextView aqiText;
        TextView pm25Text;
        TextView comfortText;
        TextView carWashText;
        TextView sportText;

        titleCity = view.findViewById(R.id.title_city);
        titleUpdateTime = view.findViewById(R.id.title_update_time);
        degreeText = view.findViewById(R.id.degree_text);
        weatherInfoText = view.findViewById(R.id.weather_info_text);
        forecastLayout = view.findViewById(R.id.forecast_layout);
        aqiText = view.findViewById(R.id.aqi_text);
        pm25Text = view.findViewById(R.id.pm25_text);
        comfortText = view.findViewById(R.id.comfort_text);
        carWashText = view.findViewById(R.id.car_wash_text);
        sportText = view.findViewById(R.id.sport_text);

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);


        forecastLayout.removeAllViews();

        for (Forecast forecast : weather.forecastList) {

            View view1 = View.inflate(this, R.layout.forecast_item, null);


            TextView dateText = view1.findViewById(R.id.date_text);
            TextView infoText = view1.findViewById(R.id.info_text);
            TextView maxText = view1.findViewById(R.id.max_text);
            TextView minText = view1.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);

            forecastLayout.addView(view1, -1);
        }

        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        Toast.makeText(WeatherActivity.this, "天气更新成功~", Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);


    }

    /*加载必应每日一图*/
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "每日一图加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });

            }
        });
    }


    @Override
    public void onBackPressed() {

        Gson gson=new Gson();
        String data=gson.toJson(weatherIdList);
        SharedPreferences.Editor editor=PreferenceManager
                .getDefaultSharedPreferences(WeatherActivity.this).edit();
        editor.putString("weatherId",data);
        editor.putBoolean("Cached",true);
        editor.apply();
        AppHelper.isFirst=true;

        ActivityCollector.exit();
    }

}
