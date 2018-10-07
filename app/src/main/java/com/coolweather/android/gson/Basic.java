package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {

    /*JSON 中的字段不太适合直接作为Java字段来命名，
    所以使用Serialized注解的方式来让JSON字段和java字段建立映射关系*/

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
