package com.gzp.m3u8;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MergeM3uText {
    public static void main(String[] args) {
        List<String> miaoTv = getTvList(getMiaoTv());
        List<String> hotel = getTvList(getGyFabu("雅思阁","http://103.45.68.47:666/gy/gy_JSVK/mkjd.php?ip=114.241.107.193%3A8014"));
        List<String> cq = getTvList(getGyFabu("重庆","http://103.45.68.47:666/gy/gy_JSVK/重庆.php"));
        List<String> yq = getTvList(getGyFabu("阳泉","http://103.45.68.47:666/gy/gy_JSVK/阳泉.php"));
        List<String> ak = getTvList(getGyFabu("爱看","http://103.45.68.47:666/gy/gy_JSVK/爱看mg.php"));
        List<String> gz = getTvList(getGyFabu("贵州","http://103.45.68.47:666/gy/gy_JSVK/贵州长id.php"));
        List<String> gd = getTvList(getGyFabu("广东","http://103.45.68.47:666/gy/gy_JSVK/触电.php"));
        List<String> yn = getTvList(getGyFabu("云南","http://103.45.68.47:666/gy/gy_JSVK/云南.php"));

        List<String> allList =new ArrayList<>();
        allList.addAll(miaoTv);
        allList.addAll(hotel);
        allList.addAll(cq);
        allList.addAll(yq);
        allList.addAll(ak);
        allList.addAll(gz);
        allList.addAll(gd);
        allList.addAll(yn);

        List<String> yangs = findTV(allList, "cctv");
        List<String> ws = findTV(allList, "卫视");
        List<String> jiaoyu = findTV(allList, "CETV");

        allList.removeAll(yangs);
        allList.removeAll(ws);


        List<String> hyqp = getTvList(getGyFabu("虎牙(切片)","http://103.45.68.47:666/gy/gy_JSVK/虎牙(切片).php"));
        List<String> dy = getTvList(getGyFabu("斗鱼","http://103.45.68.47:666/gy/gy_JSVK/斗鱼.php"));

        List<String> newTvList = new ArrayList<>();
        for (String str : allList) {
            if (!str.toLowerCase().contains("cctv")) {
                newTvList.add(str);
            }
            if (str.contains("genre")){
                if (str.contains("央视频道")){
                    newTvList.addAll(yangs);
                }
                if (str.contains("卫视频道")){
                    newTvList.addAll(ws);
                }
            }
        }

        newTvList.addAll(hyqp);
        newTvList.addAll(dy);

        StringBuilder builder=new StringBuilder();
        String fileHead="#EXTM3U\n";
        builder.append(fileHead);
        String title="";
        for (String str : newTvList) {
            if (!str.contains("http")){
                title=str.substring(0,str.indexOf(","));
                continue;
            }
            String[] content = str.split(",");
            String moban="#EXTINF:-1 group-title=\"%s\",%s\r\n%s";
            str=String.format(moban,title,content[0],content[1]);
            builder.append(str).append("\n");
        }
        System.out.println("builder = " + builder);



    }

    private static List<String> getTvList(String strs) {
        List<String> allList = new ArrayList<>();
        for (String str : strs.split("\n")) {
            if (str.contains("http") || str.contains("genre")) {
                allList.add(str);
            }
        }
        return allList;
    }

    private static List<String> findTV(List<String> allList, String find) {
        List<String> tvList = new ArrayList<>();
        for (String str : allList) {
            if (str.trim().isEmpty()) continue;
            String[] split = str.split(",");
            if (split.length == 2) {
                if (!split[1].contains("http")) continue;
                String name = split[0].toLowerCase();
                if (name.contains(find)) {
                    tvList.add(getTVName(name).toUpperCase() + "," + split[1]);
                }
            }
        }
        return tvList;
    }


    //喵影视
    private static String getMiaoTv() {
        String url = "http://111.67.196.181/mtvzb.txt";
        String str = HttpUtil.get(url);
//        System.out.println("str = " + str);
        return str;
    }

    //网站 http://103.45.68.47:666/gy/gy-fabu.php
    private static String getGyFabu(String name,String url) {
        String str = HttpUtil.get(url);
        str = processInput(str).replace("br/>", "");
//        System.out.println("str = " + str);
        str=name+",#genre#\n"+str;
        return str;
    }

    private static String processInput(String input) {
        Pattern pattern = Pattern.compile("([^,<]+),<a\\s+[^>]*?href=['\"](.*?)['\"][^>]*?>");
        Matcher matcher = pattern.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String extractedText = matcher.group(1).replace("br>", "");
            String url = matcher.group(2);
            builder.append(extractedText).append(",").append(url).append("\n");
        }
        return builder.toString();
    }

    //分好类的
    public static String change(String arg) {
        List<String> list = Arrays.asList(arg.split("\n"));
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        String name = "";
        for (String str : list) {
            if (ObjectUtil.isEmpty(str)) {
                continue;
            }
            JSONObject item = new JSONObject();
            String[] split = str.split(",");
            if (split.length > 1 && !split[1].contains("genre")) {
                item.putOpt(split[0].trim(), split[1].trim());
                array.add(item);
            } else {
                if (array.size() > 0) {
                    obj.putOpt(name, array);
                }
                name = split[0];
                array = new JSONArray();
            }
        }
        obj.putOpt(name, array);
        System.out.println("name = " + obj.toString());
        return obj.toString();
    }


    private static String getTVName(String name) {
        if (name.contains("cctv10") || name.contains("cctv-10")) return "CCTV10科教";
        else if (name.contains("cctv11") || name.contains("cctv-11")) return "CCTV11戏曲";
        else if (name.contains("cctv12") || name.contains("cctv-12")) return "CCTV12社会";
        else if (name.contains("cctv13") || name.contains("cctv-13")) return "CCTV13新闻";
        else if (name.contains("cctv14") || name.contains("cctv-14")) return "CCTV14少儿";
        else if (name.contains("cctv15") || name.contains("cctv-15")) return "CCTV15音乐";
        else if (name.contains("cctv16") || name.contains("cctv-16")) return "CCTV16奥林匹克";
        else if (name.contains("cctv17") || name.contains("cctv-17")) return "CCTV17农业";
        else if (name.contains("cctv5+") || name.contains("cctv-5+")) return "CCTV5+体育";
        else if (name.contains("cctv4k") || name.contains("cctv-4k")) return "CCTV4K高清";
        else if (name.contains("cctv1") || name.contains("cctv-1")) return "CCTV1综合";
        else if (name.contains("cctv2") || name.contains("cctv-2")) return "CCTV2财经";
        else if (name.contains("cctv3") || name.contains("cctv-3")) return "CCTV3综艺";
        else if (name.contains("cctv4") || name.contains("cctv-4")) return "CCTV4国际";
        else if (name.contains("cctv5") || name.contains("cctv-5")) return "CCTV5体育";
        else if (name.contains("cctv6") || name.contains("cctv-6")) return "CCTV6电影";
        else if (name.contains("cctv7") || name.contains("cctv-7")) return "CCTV7军事";
        else if (name.contains("cctv8") || name.contains("cctv-8")) return "CCTV8电视";
        else if (name.contains("cctv9") || name.contains("cctv-9")) return "CCTV9记录";
        else {
            return name;
        }
    }
}
