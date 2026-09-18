package com.example.coursetable;

public class Course {
    public String name, teacher, room, note, weekRule; public int weekday, startPeriod, periods, weekStart, weekEnd;
    public Course(String name, int weekday, int startPeriod, int periods, int weekStart, int weekEnd, String teacher, String room, String note) {
        this(name, weekday, startPeriod, periods, weekStart, weekEnd, teacher, room, note, weekStart+"-"+weekEnd);
    }
    public Course(String name, int weekday, int startPeriod, int periods, int weekStart, int weekEnd, String teacher, String room, String note, String weekRule) {
        this.name=name; this.weekday=weekday; this.startPeriod=startPeriod; this.periods=periods; this.weekStart=weekStart; this.weekEnd=weekEnd; this.teacher=teacher; this.room=room; this.note=note; this.weekRule=weekRule==null||weekRule.trim().isEmpty()?weekStart+"-"+weekEnd:weekRule.trim();
    }
    public boolean activeAt(int week) { if(week<weekStart||week>weekEnd)return false; for(String raw:weekRule.replace('，','、').split("、|;|，")){String token=raw.trim();if(token.isEmpty())continue;boolean odd=token.endsWith("单"),even=token.endsWith("双");if(odd||even)token=token.substring(0,token.length()-1);try{String[] range=token.split("-",-1);int from=Integer.parseInt(range[0].trim()),to=range.length>1?Integer.parseInt(range[1].trim()):from;if(week>=from&&week<=to&&(!odd&&!even||(odd&&week%2==1)||(even&&week%2==0)))return true;}catch(Exception ignored){}}return false; }
}
