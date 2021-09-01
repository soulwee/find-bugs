package com.gudong.utils;

/**
 * description 日志
 *
 * @author maggie
 * @date 2021-07-21 14:52
 */
public class LogUtils {
    public static boolean debug = false;
    public static boolean info = true;
    public static boolean error = true;
    public static void debug(String msg) {
        if(debug){
            System.out.println("code checking : " + msg);
        }
    }
    public static void info(String msg) {
        if(info){
            System.out.println("code checking : " + msg);
        }
    }
    public static void error(String msg, Exception e) {
        if(error){
            System.err.println("checking error: " + msg);
            e.printStackTrace();
        }
    }
}
