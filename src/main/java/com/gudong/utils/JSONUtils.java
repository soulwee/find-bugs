package com.gudong.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * description
 *
 * @author maggie
 * @date 2021-07-21 15:34
 */
public class JSONUtils {
    public static String toJson(Object o) {
        try {
            return new Gson().toJson(o);
        } catch (Exception e) {
            LogUtils.error("Covert Object As String Error ! ", e);
        }
        return "";
    }

    public static <T> T parseJson(String json, Class<T> clazz) {
        try {
            return getGsonBuilder().create().fromJson(json, clazz);
        } catch (Exception e) {
            LogUtils.error("Parse Json String To Object Error ! ", e);
        }
        return null;
    }
    private static GsonBuilder getGsonBuilder() {
        return new GsonBuilder().disableHtmlEscaping().serializeNulls()
                .enableComplexMapKeySerialization().serializeSpecialFloatingPointValues()
                .setLenient();

    }
}
