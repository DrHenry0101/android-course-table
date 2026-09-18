package com.example.coursetable;

import android.content.Context;
import java.util.*;

public class CourseStore {
    private final android.content.SharedPreferences prefs;
    public CourseStore(Context c) { prefs=c.getSharedPreferences("courses", Context.MODE_PRIVATE); }
    public ArrayList<Course> load() {
        ArrayList<Course> out=new ArrayList<>(); String raw=prefs.getString("data", "");
        if (raw.isEmpty()) return out;
        for (String line: raw.split("\\n", -1)) { String[] a=line.split("\\t", -1); if(a.length<9) continue;
            try { out.add(new Course(a[0],Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]),a[6],a[7],a[8],a.length>=10?a[9]:a[4]+"-"+a[5])); } catch(Exception ignored){} }
        return out;
    }
    public void save(List<Course> courses) { StringBuilder b=new StringBuilder(); for(Course c:courses) b.append(clean(c.name)).append('\t').append(c.weekday).append('\t').append(c.startPeriod).append('\t').append(c.periods).append('\t').append(c.weekStart).append('\t').append(c.weekEnd).append('\t').append(clean(c.teacher)).append('\t').append(clean(c.room)).append('\t').append(clean(c.note)).append('\t').append(clean(c.weekRule)).append('\n'); prefs.edit().putString("data",b.toString()).apply(); }
    private String clean(String s) { return s==null?"":s.replace("\t"," ").replace("\n"," "); }
}
