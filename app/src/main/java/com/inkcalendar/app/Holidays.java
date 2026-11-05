package com.inkcalendar.app;

/** China mainland official 2026 schedule, Guoban Famingdian [2025] No.7.
 * Unknown years return no work/rest classification; festival names are independent.
 */
final class Holidays {
    private static final String[][] REST={
        {"2026-01-01","2026-01-03"},{"2026-02-15","2026-02-23"},
        {"2026-04-04","2026-04-06"},{"2026-05-01","2026-05-05"},
        {"2026-06-19","2026-06-21"},{"2026-09-25","2026-09-27"},
        {"2026-10-01","2026-10-07"}};
    private static final String[] WORK={"2026-01-04","2026-02-14","2026-02-28","2026-05-09","2026-09-20","2026-10-10"};
    static boolean supported(String date) {return date.startsWith("2026-");}
    static int status(String date) {
        for(String work:WORK)if(work.equals(date))return -1;
        for(String[] range:REST)if(date.compareTo(range[0])>=0 && date.compareTo(range[1])<=0)return 1;
        return 0;
    }
    static String label(String date,boolean english) {
        int status=status(date);return status==1?(english?"Off":"休"):status==-1?(english?"Work":"班"):"";
    }
}
