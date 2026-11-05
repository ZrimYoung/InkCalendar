package com.inkcalendar.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Offline China-calendar dates. No runtime astronomical calculation or device time-zone dependency. */
final class Almanac {
    private static final String[] TERMS="小寒 大寒 立春 雨水 惊蛰 春分 清明 谷雨 立夏 小满 芒种 夏至 小暑 大暑 立秋 处暑 白露 秋分 寒露 霜降 立冬 小雪 大雪 冬至".split(" ");
    private static final String[] MONTHS="正 二 三 四 五 六 七 八 九 十 冬 腊".split(" ");
    private static final long EPOCH=Dates.parse("1900-01-01").getTimeInMillis();
    private final List<int[]> months=new ArrayList<>();
    private final Map<String,Integer> terms=new HashMap<>();
    Almanac(Reader monthInput, Reader termInput) throws IOException {
        try(BufferedReader reader=new BufferedReader(monthInput)) {
            String line; while((line=reader.readLine())!=null) {
                String[] p=line.split(","); months.add(new int[]{Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3])});
            }
        }
        try(BufferedReader reader=new BufferedReader(termInput)) {
            String line; while((line=reader.readLine())!=null) {
                String[] p=line.split(","); terms.put(p[0],Integer.parseInt(p[1]));
            }
        }
    }
    static final class LunarDate {
        final int year, month, day, length;
        LunarDate(int year,int month,int day,int length) { this.year=year;this.month=month;this.day=day;this.length=length; }
        String monthName() { return (month<0?"闰":"")+MONTHS[Math.abs(month)-1]+"月"; }
        String dayName() {
            if(day==10)return "初十";if(day==20)return "二十";if(day==30)return "三十";
            return new String[]{"初","十","廿"}[(day-1)/10]+"一二三四五六七八九".charAt((day-1)%10);
        }
        String shortName() { return day==1?monthName():dayName(); }
        String fullName() { return monthName()+dayName(); }
    }
    LunarDate lunar(String date) {
        if(date.compareTo("1900-01-01")<0 || date.compareTo("2100-12-31")>0)return null;
        int day=(int)((Dates.parse(date).getTimeInMillis()-EPOCH)/86400000L);
        int low=0,high=months.size()-1;
        while(low<=high) { int mid=(low+high)>>>1; if(months.get(mid)[0]<=day)low=mid+1;else high=mid-1; }
        if(high<0)return null;
        int[] m=months.get(high);return new LunarDate(m[1],m[2],day-m[0]+1,m[3]);
    }
    String term(String date) { Integer term=terms.get(date);return term==null?"":TERMS[term]; }
    String festival(String date,boolean english) {
        String md=date.substring(5);
        if(md.equals("01-01"))return english?"New Year":"元旦";
        if(md.equals("05-01"))return english?"Labour Day":"劳动节";
        if(md.equals("10-01"))return english?"National Day":"国庆节";
        if("清明".equals(term(date)))return english?"Qingming":"清明节";
        LunarDate l=lunar(date);if(l==null || l.month<0)return "";
        if(l.month==1 && l.day==1)return english?"Spring Festival":"春节";
        if(l.month==1 && l.day==15)return english?"Lantern Festival":"元宵节";
        if(l.month==5 && l.day==5)return english?"Dragon Boat Festival":"端午节";
        if(l.month==8 && l.day==15)return english?"Mid-Autumn Festival":"中秋节";
        if(l.month==9 && l.day==9)return english?"Double Ninth":"重阳节";
        if(l.month==12 && l.day==l.length)return english?"Spring Festival Eve":"除夕";
        return "";
    }
    String nextAnniversary(String anchor,String today) {
        LunarDate original=lunar(anchor), current=lunar(today);
        if(original==null || current==null)return null;
        for(int year=current.year;year<=2100;year++) {
            int[] chosen=null;
            for(int[] m:months)if(m[1]==year && m[2]==original.month) { chosen=m;break; }
            // A leap-month birthday falls in the regular month in years without that leap month.
            if(chosen==null && original.month<0)for(int[] m:months)if(m[1]==year && m[2]==-original.month) {chosen=m;break;}
            if(chosen==null)continue;
            String candidate=Dates.addDays("1900-01-01",chosen[0]+Math.min(original.day,chosen[3])-1);
            if(candidate.compareTo(today)>=0 && candidate.compareTo("2100-12-31")<=0)return candidate;
        }
        return null;
    }
}
