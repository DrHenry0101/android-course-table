package com.example.coursetable;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF_CONFIG = "courses_config";
    private static final String KEY_START_MONDAY = "start_monday_ms";
    private static final String[] WEEKDAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};

    private final ArrayList<Course> courses = new ArrayList<>();
    private CourseStore store;
    private TimetableView table;
    private TextView weekText;
    private Button returnWeekBtn;
    private TextView dateSubText;

    private int week = 1;              // Currently selected/viewed week
    private int currentRealWeek = 1;   // The real week matching today's date
    private long startMondayMs = 0L;   // Timestamp of semester start Monday
    private SharedPreferences configPrefs;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        configPrefs = getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE);
        store = new CourseStore(this);
        courses.addAll(store.load());

        calculateCurrentDateAndWeek();
        buildUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recalculate date & week on resume so it stays in sync if day passed
        calculateCurrentDateAndWeek();
        if (table != null) {
            refresh();
        }
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    /**
     * Calculates the real week number based on today's date and the stored semester start date.
     * If not previously configured, defaults current week to 3 (matching initial course data)
     * and derives the semester start Monday.
     */
    private void calculateCurrentDateAndWeek() {
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        int todayDayOfWeek = (todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1; // Mon=1..Sun=7
        long todayMondayMs = todayCal.getTimeInMillis() - (todayDayOfWeek - 1) * 86400000L;

        startMondayMs = configPrefs.getLong(KEY_START_MONDAY, 0L);
        if (startMondayMs == 0L) {
            // Default initial state: current week is Week 3
            // Monday of Week 1 was 2 weeks before today's Monday
            startMondayMs = todayMondayMs - 2 * 7L * 86400000L;
            configPrefs.edit().putLong(KEY_START_MONDAY, startMondayMs).apply();
        }

        long diffDays = (todayMondayMs - startMondayMs) / 86400000L;
        int realWeek = (int) (diffDays / 7) + 1;
        if (realWeek < 1) realWeek = 1;
        if (realWeek > 30) realWeek = 30;

        this.currentRealWeek = realWeek;
        // On app open/resume, always stop at the week matching the current date!
        this.week = realWeek;
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F8FC);

        // Respect system window insets (status bar & navigation bar) to avoid any overlapping
        root.setFitsSystemWindows(true);

        // 1. Header Bar: Title + Dynamic Date + Action Buttons
        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(dp(16), dp(10), dp(16), dp(4));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText("我的课表");
        titleText.setTextSize(20);
        titleText.setTextColor(0xFF1F2937);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);

        dateSubText = new TextView(this);
        dateSubText.setText(getFormattedTodayDate());
        dateSubText.setTextSize(12);
        dateSubText.setTextColor(0xFF64748B);
        dateSubText.setPadding(0, dp(2), 0, 0);

        titleCol.addView(titleText);
        titleCol.addView(dateSubText);
        headerBar.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // Action Buttons: "＋ 添加" and "📥 导入"
        Button addBtn = createTopActionButton("＋ 添加");
        Button importBtn = createTopActionButton("📥 导入");
        addBtn.setOnClickListener(v -> showEditor(null));
        importBtn.setOnClickListener(v -> pickCsv());

        headerBar.addView(addBtn);
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        importLp.leftMargin = dp(8);
        headerBar.addView(importBtn, importLp);
        root.addView(headerBar);

        // 2. Week Switcher Bar: ‹ | 第 X 周 ▾ | › | 回到本周
        LinearLayout weekBar = new LinearLayout(this);
        weekBar.setOrientation(LinearLayout.HORIZONTAL);
        weekBar.setGravity(Gravity.CENTER_VERTICAL);
        weekBar.setPadding(dp(16), dp(6), dp(16), dp(8));

        Button prevBtn = createNavButton("‹");
        prevBtn.setOnClickListener(v -> {
            if (week > 1) {
                week--;
                refresh();
            }
        });

        // Center Week Indicator (Clickable to jump to specific week or calibrate)
        weekText = new TextView(this);
        weekText.setText("第 " + week + " 周 ▾");
        weekText.setTextSize(14.5f);
        weekText.setTextColor(0xFF1F2937);
        weekText.setTypeface(Typeface.DEFAULT_BOLD);
        weekText.setGravity(Gravity.CENTER);
        weekText.setBackgroundResource(R.drawable.bg_pill_white);
        weekText.setMinHeight(dp(38));
        weekText.setPadding(dp(14), dp(6), dp(14), dp(6));
        weekText.setOnClickListener(v -> showWeekPickerDialog());

        Button nextBtn = createNavButton("›");
        nextBtn.setOnClickListener(v -> {
            if (week < 30) {
                week++;
                refresh();
            }
        });

        // "回到本周" Entrance
        returnWeekBtn = new Button(this);
        returnWeekBtn.setText("回到本周");
        returnWeekBtn.setTextSize(12);
        returnWeekBtn.setAllCaps(false);
        returnWeekBtn.setMinHeight(dp(36));
        returnWeekBtn.setMinWidth(dp(72));
        returnWeekBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        returnWeekBtn.setOnClickListener(v -> {
            week = currentRealWeek;
            refresh();
            Toast.makeText(this, "已回到第 " + currentRealWeek + " 周（本周）", Toast.LENGTH_SHORT).show();
        });

        weekBar.addView(prevBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout.LayoutParams weekLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        weekLp.leftMargin = dp(4);
        weekLp.rightMargin = dp(4);
        weekBar.addView(weekText, weekLp);
        weekBar.addView(nextBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));

        View spacer = new View(this);
        weekBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));
        weekBar.addView(returnWeekBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)));

        root.addView(weekBar);

        // 3. Timetable Core View
        table = new TimetableView(this);
        table.setCourses(courses);
        table.setWeek(week);
        table.setCurrentWeek(currentRealWeek);
        table.setSemesterStartMonday(startMondayMs);
        table.setOnCourseClick(this::showCourseDetail);
        table.setOnActionListener(new TimetableView.OnActionListener() {
            @Override
            public void onAddCourse() {
                showEditor(null);
            }

            @Override
            public void onImportCsv() {
                pickCsv();
            }
        });

        root.addView(table, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

        updateReturnWeekBtnStyle();
        setContentView(root);
    }

    private Button createTopActionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12.5f);
        b.setTextColor(0xFF4F6EF7);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundResource(R.drawable.bg_pill_subtle);
        b.setAllCaps(false);
        b.setMinHeight(dp(38));
        b.setMinWidth(dp(68));
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        return b;
    }

    private Button createNavButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(20);
        b.setTextColor(0xFF1F2937);
        b.setBackgroundResource(R.drawable.bg_pill_subtle);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(44));
        b.setMinWidth(dp(44));
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private String getFormattedTodayDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("M月d日 · E", Locale.CHINESE);
        return sdf.format(cal.getTime());
    }

    private void updateReturnWeekBtnStyle() {
        if (returnWeekBtn == null) return;
        if (week == currentRealWeek) {
            returnWeekBtn.setText("本周");
            returnWeekBtn.setTextColor(0xFF94A3B8);
            returnWeekBtn.setBackgroundResource(R.drawable.bg_pill_white);
        } else {
            returnWeekBtn.setText("回到本周");
            returnWeekBtn.setTextColor(0xFF4F6EF7);
            returnWeekBtn.setBackgroundResource(R.drawable.bg_pill_subtle);
        }
    }

    private void refresh() {
        weekText.setText("第 " + week + " 周 ▾");
        dateSubText.setText(getFormattedTodayDate());
        updateReturnWeekBtnStyle();
        if (table != null) {
            table.setSemesterStartMonday(startMondayMs);
            table.setCurrentWeek(currentRealWeek);
            table.setCourses(courses);
            table.setWeek(week);
        }
    }

    /**
     * Week Picker Dialog allowing fast navigation to any week (1..25)
     * and setting the chosen week as current week.
     */
    private void showWeekPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_dialog_rounded);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("选择周次");
        title.setTextSize(17);
        title.setTextColor(0xFF1F2937);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        TextView sub = new TextView(this);
        sub.setText("当前自然周为第 " + currentRealWeek + " 周");
        sub.setTextSize(12.5f);
        sub.setTextColor(0xFF64748B);
        sub.setPadding(0, dp(4), 0, dp(14));
        layout.addView(sub);

        // Grid of Weeks 1 to 25
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);

        for (int w = 1; w <= 25; w++) {
            final int targetWeek = w;
            Button wb = new Button(this);
            wb.setText(String.valueOf(w));
            wb.setTextSize(13);
            wb.setAllCaps(false);
            wb.setMinHeight(dp(44));
            wb.setMinWidth(dp(44));

            if (w == week) {
                wb.setTextColor(0xFFFFFFFF);
                wb.setBackgroundResource(R.drawable.bg_pill_primary);
                wb.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (w == currentRealWeek) {
                wb.setTextColor(0xFF4F6EF7);
                wb.setBackgroundResource(R.drawable.bg_pill_subtle);
                wb.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                wb.setTextColor(0xFF1F2937);
                wb.setBackgroundResource(R.drawable.bg_pill_white);
            }

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = dp(46);
            glp.height = dp(44);
            glp.setMargins(dp(3), dp(3), dp(3), dp(3));
            wb.setLayoutParams(glp);

            wb.setOnClickListener(v -> {
                week = targetWeek;
                refresh();
                dialog.dismiss();
            });
            grid.addView(wb);
        }
        layout.addView(grid);

        // Bottom Action: Set current selected week as real current week
        Button setAsCurrentBtn = new Button(this);
        setAsCurrentBtn.setText("设置第 " + week + " 周为当前周");
        setAsCurrentBtn.setTextSize(13);
        setAsCurrentBtn.setTextColor(0xFF4F6EF7);
        setAsCurrentBtn.setBackgroundResource(R.drawable.bg_pill_subtle);
        setAsCurrentBtn.setMinHeight(dp(44));
        setAsCurrentBtn.setAllCaps(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        btnLp.topMargin = dp(16);
        setAsCurrentBtn.setOnClickListener(v -> {
            Calendar todayCal = Calendar.getInstance();
            int todayDayOfWeek = (todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
            long todayMondayMs = todayCal.getTimeInMillis() - (todayDayOfWeek - 1) * 86400000L;
            startMondayMs = todayMondayMs - (week - 1) * 7L * 86400000L;
            configPrefs.edit().putLong(KEY_START_MONDAY, startMondayMs).apply();
            currentRealWeek = week;
            refresh();
            dialog.dismiss();
            Toast.makeText(this, "已将第 " + week + " 周设置为当前周，后续周数将随日期自动递增", Toast.LENGTH_LONG).show();
        });
        layout.addView(setAsCurrentBtn, btnLp);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    /**
     * Displays a clean, elegant Course Detail Dialog matching WakeUp curriculum specs.
     */
    private void showCourseDetail(Course c) {
        if (c == null) return;
        Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_dialog_rounded);
        box.setPadding(dp(20), dp(20), dp(20), dp(18));

        // Header: Course Name + Badge
        TextView nameText = new TextView(this);
        nameText.setText(c.name);
        nameText.setTextSize(18);
        nameText.setTextColor(0xFF1F2937);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setPadding(0, 0, 0, dp(14));
        box.addView(nameText);

        // Detail Rows
        int endPeriod = c.startPeriod + c.periods - 1;
        String dayName = (c.weekday >= 1 && c.weekday <= 7) ? WEEKDAY_NAMES[c.weekday - 1] : String.valueOf(c.weekday);
        String periodDesc = "星期" + dayName + " · 第 " + c.startPeriod + "-" + endPeriod + " 节 (共 " + c.periods + " 节)";
        String weekDesc = "第 " + c.weekRule + " 周";

        box.addView(createDetailRow("📅 节次时间", periodDesc));
        box.addView(createDetailRow("⏱ 周数规则", weekDesc));
        box.addView(createDetailRow("📍 上课教室", TextUtils.isEmpty(c.room) ? "未填写" : c.room));
        box.addView(createDetailRow("👨‍🏫 任课教师", TextUtils.isEmpty(c.teacher) ? "未填写" : c.teacher));
        if (!TextUtils.isEmpty(c.note)) {
            box.addView(createDetailRow("📝 课程备注", c.note));
        }

        // Action Buttons: 删除课程 | 编辑课程
        LinearLayout btnBar = new LinearLayout(this);
        btnBar.setOrientation(LinearLayout.HORIZONTAL);
        btnBar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = dp(18);

        Button deleteBtn = new Button(this);
        deleteBtn.setText("删除");
        deleteBtn.setTextSize(13);
        deleteBtn.setTextColor(0xFFEF4444);
        deleteBtn.setBackgroundResource(R.drawable.bg_pill_white);
        deleteBtn.setMinHeight(dp(44));
        deleteBtn.setMinWidth(dp(72));
        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除课程《" + c.name + "》吗？")
                .setPositiveButton("删除", (d, which) -> {
                    courses.remove(c);
                    store.save(courses);
                    refresh();
                    dlg.dismiss();
                    Toast.makeText(this, "已删除课程", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
        });

        Button editBtn = new Button(this);
        editBtn.setText("编辑课程");
        editBtn.setTextSize(13);
        editBtn.setTextColor(0xFFFFFFFF);
        editBtn.setBackgroundResource(R.drawable.bg_button_primary);
        editBtn.setMinHeight(dp(44));
        editBtn.setMinWidth(dp(96));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
        editLp.leftMargin = dp(10);
        editBtn.setOnClickListener(v -> {
            dlg.dismiss();
            showEditor(c);
        });

        btnBar.addView(deleteBtn);
        btnBar.addView(editBtn, editLp);
        box.addView(btnBar, barLp);

        dlg.setContentView(box);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dlg.show();
    }

    private LinearLayout createDetailRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(13);
        lbl.setTextColor(0xFF64748B);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(dp(95), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextSize(13);
        val.setTextColor(0xFF1F2937);
        val.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        row.addView(lbl);
        row.addView(val);
        return row;
    }

    /**
     * Editor Dialog for creating or modifying a Course.
     * Preserves exact weekRule (e.g. 4-16双, 3-5、7-16) when editing existing course.
     */
    private void showEditor(Course old) {
        Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_dialog_rounded);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        scroll.addView(box);

        TextView title = new TextView(this);
        title.setText(old == null ? "添加课程" : "编辑课程");
        title.setTextSize(17);
        title.setTextColor(0xFF1F2937);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(10));
        box.addView(title);

        EditText name = input("课程名称 (如: 高等数学)");
        EditText day = input("星期 1-7 (如: 1 代表星期一)");
        EditText start = input("开始节次 1-12 (如: 1)");
        EditText periods = input("连续节数 (如: 2)");
        EditText weekRuleInput = input("周数规则 (如: 3-7, 4-16双, 3-5、7-16)");
        EditText teacher = input("教师 (可选)");
        EditText room = input("教室 (可选)");
        EditText note = input("备注 (可选)");

        if (old != null) {
            name.setText(old.name);
            day.setText(String.valueOf(old.weekday));
            start.setText(String.valueOf(old.startPeriod));
            periods.setText(String.valueOf(old.periods));
            // Keep and prefill original weekRule faithfully!
            weekRuleInput.setText(old.weekRule);
            teacher.setText(old.teacher);
            room.setText(old.room);
            note.setText(old.note);
        }

        for (EditText e : new EditText[]{name, day, start, periods, weekRuleInput, teacher, room, note}) {
            box.addView(e);
        }

        // Action Buttons: 取消 | 保存
        LinearLayout btnBox = new LinearLayout(this);
        btnBox.setOrientation(LinearLayout.HORIZONTAL);
        btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams btnBoxLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnBoxLp.topMargin = dp(14);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("取消");
        cancelBtn.setTextSize(13);
        cancelBtn.setTextColor(0xFF64748B);
        cancelBtn.setBackgroundResource(R.drawable.bg_pill_white);
        cancelBtn.setMinHeight(dp(44));
        cancelBtn.setMinWidth(dp(72));
        cancelBtn.setOnClickListener(v -> dlg.dismiss());

        Button saveBtn = new Button(this);
        saveBtn.setText(old == null ? "保存" : "保存修改");
        saveBtn.setTextSize(13);
        saveBtn.setTextColor(0xFFFFFFFF);
        saveBtn.setBackgroundResource(R.drawable.bg_button_primary);
        saveBtn.setMinHeight(dp(44));
        saveBtn.setMinWidth(dp(96));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
        saveLp.leftMargin = dp(10);

        saveBtn.setOnClickListener(v -> {
            try {
                String cName = name.getText().toString().trim();
                String dayStr = day.getText().toString().trim();
                String startStr = start.getText().toString().trim();
                String periodsStr = periods.getText().toString().trim();
                String ruleStr = weekRuleInput.getText().toString().trim();

                if (cName.isEmpty()) {
                    Toast.makeText(this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                int weekday = Integer.parseInt(dayStr);
                int startPeriod = Integer.parseInt(startStr);
                int numPeriods = Integer.parseInt(periodsStr);

                if (weekday < 1 || weekday > 7) {
                    Toast.makeText(this, "星期必须在 1 到 7 之间", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startPeriod < 1 || startPeriod > 12 || numPeriods < 1 || startPeriod + numPeriods - 1 > 12) {
                    Toast.makeText(this, "节次范围必须在 1 到 12 节之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If user didn't specify a rule, default to 1-16
                if (ruleStr.isEmpty()) {
                    ruleStr = "1-16";
                }

                if (!validWeekRule(ruleStr)) {
                    Toast.makeText(this, "周数规则格式不正确，如: 3-7, 4-16双, 3-5、7-16", Toast.LENGTH_LONG).show();
                    return;
                }

                int[] bounds = weekBounds(ruleStr);

                Course updatedCourse = new Course(
                    cName,
                    weekday,
                    startPeriod,
                    numPeriods,
                    bounds[0],
                    bounds[1],
                    teacher.getText().toString().trim(),
                    room.getText().toString().trim(),
                    note.getText().toString().trim(),
                    ruleStr
                );

                if (old != null) {
                    int idx = courses.indexOf(old);
                    if (idx >= 0) {
                        courses.set(idx, updatedCourse);
                    } else {
                        courses.add(updatedCourse);
                    }
                } else {
                    courses.add(updatedCourse);
                }

                store.save(courses);
                refresh();
                dlg.dismiss();
                Toast.makeText(this, old == null ? "添加课程成功" : "保存修改成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "请检查输入的内容格式", Toast.LENGTH_SHORT).show();
            }
        });

        btnBox.addView(cancelBtn);
        btnBox.addView(saveBtn, saveLp);
        box.addView(btnBox, btnBoxLp);

        dlg.setContentView(scroll);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dlg.show();
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(13);
        e.setTextColor(0xFF1F2937);
        e.setHintTextColor(0xFF94A3B8);
        e.setSingleLine();
        e.setMinHeight(dp(44));
        e.setPadding(dp(8), dp(10), dp(8), dp(10));
        return e;
    }

    // =========================================================================
    // CSV Import Logic (Strictly preserved 7-column & 9-column parsing)
    // =========================================================================
    private void pickCsv() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != 10 || result != RESULT_OK || data == null || data.getData() == null) return;

        int count = 0, skipped = 0;
        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = r.readLine();
            if (line != null) line = stripBom(line);
            String[] header = parseCsv(line);
            int format = headerFormat(header);

            if (format == 0) {
                Toast.makeText(this, "CSV 表头不符合约定", Toast.LENGTH_LONG).show();
                r.close();
                return;
            }

            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] a = parseCsv(line);
                try {
                    Course c;
                    if (format == 7) {
                        if (a.length < 7) throw new Exception();
                        int start = Integer.parseInt(a[2].trim());
                        int end = Integer.parseInt(a[3].trim());
                        String rule = a[6].trim();
                        int[] bounds = weekBounds(rule);
                        if (!validRange(a[0], a[1], start, end - start + 1, bounds[0], bounds[1]) || !validWeekRule(rule)) {
                            throw new Exception();
                        }
                        c = new Course(a[0].trim(), Integer.parseInt(a[1].trim()), start, end - start + 1, bounds[0], bounds[1], a[4].trim(), a[5].trim(), "", rule);
                    } else {
                        if (a.length < 9 || !validNumbers(a)) throw new Exception();
                        c = new Course(a[0].trim(), Integer.parseInt(a[1].trim()), Integer.parseInt(a[2].trim()), Integer.parseInt(a[3].trim()), Integer.parseInt(a[4].trim()), Integer.parseInt(a[5].trim()), a[6].trim(), a[7].trim(), a[8].trim());
                    }
                    courses.add(c);
                    count++;
                } catch (Exception ignored) {
                    skipped++;
                }
            }
            r.close();

            store.save(courses);
            refresh();
            Toast.makeText(this, "已导入 " + count + " 门课程，跳过 " + skipped + " 行", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "CSV 读取失败，请使用 UTF-8 格式", Toast.LENGTH_LONG).show();
        }
    }

    private String stripBom(String s) {
        return (s != null && s.length() > 0 && s.charAt(0) == '\ufeff') ? s.substring(1) : s;
    }

    private String[] parseCsv(String line) {
        if (line == null) return new String[0];
        ArrayList<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    b.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                out.add(b.toString());
                b.setLength(0);
            } else {
                b.append(ch);
            }
        }
        out.add(b.toString());
        return out.toArray(new String[0]);
    }

    private int headerFormat(String[] h) {
        if (h.length >= 9) {
            String[] w = {"name", "weekday", "start_period", "periods", "week_start", "week_end", "teacher", "room", "note"};
            boolean ok = true;
            for (int i = 0; i < w.length; i++) {
                if (!w[i].equals(h[i].trim().toLowerCase(Locale.ROOT))) ok = false;
            }
            if (ok) return 9;
        }
        if (h.length >= 7) {
            String[] w = {"课程名称", "星期", "开始节数", "结束节数", "老师", "地点", "周数"};
            boolean ok = true;
            for (int i = 0; i < w.length; i++) {
                if (!w[i].equals(h[i].trim())) ok = false;
            }
            if (ok) return 7;
        }
        return 0;
    }

    private boolean validNumbers(String[] a) {
        try {
            int day = Integer.parseInt(a[1].trim());
            int start = Integer.parseInt(a[2].trim());
            int periods = Integer.parseInt(a[3].trim());
            int from = Integer.parseInt(a[4].trim());
            int to = Integer.parseInt(a[5].trim());
            return !a[0].trim().isEmpty() && day >= 1 && day <= 7 && start >= 1 && start <= 12 && periods >= 1 && periods <= 12 && start + periods - 1 <= 12 && from >= 1 && to >= from;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validRange(String name, String day, String start, String periods, String from, String to) {
        return validNumbers(new String[]{name, day, start, periods, from, to});
    }

    private boolean validRange(String name, String day, int start, int periods, int from, int to) {
        return validRange(name, day, String.valueOf(start), String.valueOf(periods), String.valueOf(from), String.valueOf(to));
    }

    private int[] weekBounds(String rule) {
        int min = Integer.MAX_VALUE, max = 0;
        for (String raw : rule.replace('，', '、').split("、|;|，")) {
            String t = raw.trim();
            if (t.endsWith("单") || t.endsWith("双")) t = t.substring(0, t.length() - 1);
            String[] p = t.split("-", -1);
            int a = Integer.parseInt(p[0].trim());
            int b = p.length > 1 ? Integer.parseInt(p[1].trim()) : a;
            min = Math.min(min, a);
            max = Math.max(max, b);
        }
        if (min == Integer.MAX_VALUE) throw new IllegalArgumentException();
        return new int[]{min, max};
    }

    private boolean validWeekRule(String rule) {
        try {
            for (String raw : rule.replace('，', '、').split("、|;|，")) {
                String t = raw.trim();
                if (t.isEmpty()) return false;
                if (t.endsWith("单") || t.endsWith("双")) t = t.substring(0, t.length() - 1);
                String[] p = t.split("-", -1);
                if (p.length > 2) return false;
                int a = Integer.parseInt(p[0].trim());
                int b = p.length > 1 ? Integer.parseInt(p[1].trim()) : a;
                if (a < 1 || b < a) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
