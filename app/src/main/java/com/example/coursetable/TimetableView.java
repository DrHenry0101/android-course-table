package com.example.coursetable;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import java.util.*;

public class TimetableView extends LinearLayout {

    public interface OnCourseClick {
        void click(Course c);
    }

    public interface OnActionListener {
        void onAddCourse();
        void onImportCsv();
    }

    // Palette: 10 pastel color pairs matching WakeUp / Morandi style
    // [0]=background, [1]=border, [2]=primary text, [3]=secondary text
    private static final int[][] PALETTE = {
        {0xFFEBF1FF, 0xFFD4E1FD, 0xFF2A50C7, 0xFF4A6CD4}, // Blue
        {0xFFE7F7ED, 0xFFC6EED7, 0xFF1B7347, 0xFF358C5E}, // Green
        {0xFFFFF1E0, 0xFFFFDFBD, 0xFFAC5300, 0xFFBF6D1C}, // Amber
        {0xFFF3EBFF, 0xFFE2D1FE, 0xFF6532B8, 0xFF7E4ECB}, // Purple
        {0xFFFEECEE, 0xFFFACED3, 0xFFB3263B, 0xFFC34658}, // Coral
        {0xFFE3F6F5, 0xFFBCEAE7, 0xFF0E7370, 0xFF2B8C89}, // Teal
        {0xFFECEBFF, 0xFFD4D2FD, 0xFF4046C4, 0xFF5B60CE}, // Indigo
        {0xFFFEF7DC, 0xFFFCEEAA, 0xFF836006, 0xFF997519}, // Ochre
        {0xFFFFEFE9, 0xFFFFD7C7, 0xFFBA4218, 0xFFC95B33}, // Salmon
        {0xFFFCEBF4, 0xFFF9CEE3, 0xFF9E1F61, 0xFFB03D78}  // Berry
    };

    private static final String[] DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final String[] PERIOD_TIMES = {
        "08:00", "08:55", "10:00", "10:55",
        "14:00", "14:55", "16:00", "16:55",
        "19:00", "19:55", "20:50", "21:40"
    };

    private List<Course> allCourses = new ArrayList<>();
    private int displayedWeek = 1;
    private int currentRealWeek = 1;
    private long semesterStartMonday = 0L;

    private OnCourseClick onCourseClickListener;
    private OnActionListener onActionListener;

    private WeekdayHeaderView headerView;
    private ScrollView scrollView;
    private CourseGridView gridView;
    private LinearLayout emptyLayout;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private final float density;

    public TimetableView(Context context) {
        this(context, null);
    }

    public TimetableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        initViews(context);
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    private void initViews(Context context) {
        // 1. Pinned Weekday Header View (Height: 46dp)
        headerView = new WeekdayHeaderView(context);
        addView(headerView, new LayoutParams(LayoutParams.MATCH_PARENT, dp(46)));

        // 2. Container for ScrollView and Empty State
        FrameLayout contentContainer = new FrameLayout(context);
        LayoutParams containerParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f);
        addView(contentContainer, containerParams);

        // ScrollView for Course Grid
        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);

        gridView = new CourseGridView(context);
        scrollView.addView(gridView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        contentContainer.addView(scrollView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Empty State View
        emptyLayout = createEmptyView(context);
        contentContainer.addView(emptyLayout, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout createEmptyView(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(24), dp(32), dp(24), dp(32));
        layout.setVisibility(GONE);

        // Vector Icon
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_empty_calendar);
        layout.addView(icon, new LayoutParams(dp(72), dp(72)));

        // Title
        emptyTitle = new TextView(context);
        emptyTitle.setText("本周暂无课程");
        emptyTitle.setTextSize(16);
        emptyTitle.setTextColor(0xFF1F2937);
        emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        emptyTitle.setGravity(Gravity.CENTER);
        LayoutParams titleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(14);
        layout.addView(emptyTitle, titleParams);

        // Subtitle
        emptySubtitle = new TextView(context);
        emptySubtitle.setText("点击下方按钮添加课程或导入 CSV");
        emptySubtitle.setTextSize(13);
        emptySubtitle.setTextColor(0xFF64748B);
        emptySubtitle.setGravity(Gravity.CENTER);
        LayoutParams subParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(6);
        layout.addView(emptySubtitle, subParams);

        // Button Container
        LinearLayout btnBox = new LinearLayout(context);
        btnBox.setOrientation(HORIZONTAL);
        btnBox.setGravity(Gravity.CENTER);
        LayoutParams boxParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        boxParams.topMargin = dp(22);

        Button importBtn = new Button(context);
        importBtn.setText("导入 CSV");
        importBtn.setTextSize(14);
        importBtn.setTextColor(0xFFFFFFFF);
        importBtn.setBackgroundResource(R.drawable.bg_button_primary);
        importBtn.setAllCaps(false);
        importBtn.setMinHeight(dp(44));
        importBtn.setMinWidth(dp(110));
        importBtn.setOnClickListener(v -> {
            if (onActionListener != null) onActionListener.onImportCsv();
        });

        Button addBtn = new Button(context);
        addBtn.setText("添加课程");
        addBtn.setTextSize(14);
        addBtn.setTextColor(0xFF4F6EF7);
        addBtn.setBackgroundResource(R.drawable.bg_button_secondary);
        addBtn.setAllCaps(false);
        addBtn.setMinHeight(dp(44));
        addBtn.setMinWidth(dp(110));
        LayoutParams addParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addParams.leftMargin = dp(14);
        addBtn.setOnClickListener(v -> {
            if (onActionListener != null) onActionListener.onAddCourse();
        });

        btnBox.addView(importBtn);
        btnBox.addView(addBtn, addParams);
        layout.addView(btnBox, boxParams);

        return layout;
    }

    public void setCourses(List<Course> courses) {
        this.allCourses = courses != null ? courses : new ArrayList<>();
        updateState();
    }

    public void setWeek(int week) {
        this.displayedWeek = week;
        updateState();
    }

    public void setCurrentWeek(int currentWeek) {
        this.currentRealWeek = currentWeek;
        headerView.invalidate();
        gridView.invalidate();
    }

    public void setSemesterStartMonday(long ms) {
        this.semesterStartMonday = ms;
        headerView.invalidate();
    }

    public void setOnCourseClick(OnCourseClick listener) {
        this.onCourseClickListener = listener;
    }

    public void setOnActionListener(OnActionListener listener) {
        this.onActionListener = listener;
    }

    private void updateState() {
        boolean hasActive = false;
        for (Course c : allCourses) {
            if (c.activeAt(displayedWeek)) {
                hasActive = true;
                break;
            }
        }

        if (hasActive) {
            emptyLayout.setVisibility(GONE);
            scrollView.setVisibility(VISIBLE);
        } else {
            emptyLayout.setVisibility(VISIBLE);
            scrollView.setVisibility(GONE);
            if (allCourses.isEmpty()) {
                emptyTitle.setText("课表空空如也");
                emptySubtitle.setText("导入课表 CSV 或手动添加课程，开启日程");
            } else {
                emptyTitle.setText("本周暂无课程");
                emptySubtitle.setText("好好享受轻松的一周吧，或切换其他周次");
            }
        }

        headerView.invalidate();
        gridView.invalidate();
    }

    // =========================================================================
    // 1. WeekdayHeaderView (Sticky top bar displaying Month, Days, Dates)
    // =========================================================================
    private class WeekdayHeaderView extends View {
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public WeekdayHeaderView(Context context) {
            super(context);
            setBackgroundColor(0xFFF6F8FC);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float leftColW = dp(32);
            float dayW = (width - leftColW) / 7.0f;

            // Calendar dates for the displayed week
            Calendar cal = Calendar.getInstance();
            int todayDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1; // Mon=1..Sun=7
            boolean isThisWeek = (displayedWeek == currentRealWeek);

            // Compute dates for displayed week
            long baseMonday = semesterStartMonday;
            if (baseMonday == 0L) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.DAY_OF_YEAR, -(todayDayOfWeek - 1));
                baseMonday = cal.getTimeInMillis() - (currentRealWeek - 1) * 7L * 86400000L;
            }
            long weekMondayMs = baseMonday + (displayedWeek - 1) * 7L * 86400000L;

            cal.setTimeInMillis(weekMondayMs);
            int month = cal.get(Calendar.MONTH) + 1;

            // Draw Month in Left Column
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(0xFF1F2937);
            textPaint.setTextSize(dp(11.5f));
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(month + "月", leftColW / 2.0f, dp(26), textPaint);

            // Draw 7 Days
            for (int i = 0; i < 7; i++) {
                int dayNum = i + 1;
                long dayMs = weekMondayMs + i * 86400000L;
                cal.setTimeInMillis(dayMs);
                int dateOfMonth = cal.get(Calendar.DAY_OF_MONTH);

                float cx = leftColW + i * dayW + dayW / 2.0f;
                boolean isToday = isThisWeek && (dayNum == todayDayOfWeek);

                if (isToday) {
                    // Active blue pill for Today
                    pillPaint.setColor(0xFF4F6EF7);
                    pillPaint.setStyle(Paint.Style.FILL);
                    float pillW = Math.min(dayW - dp(4), dp(38));
                    float pillH = height - dp(8);
                    float px0 = cx - pillW / 2.0f;
                    float py0 = dp(4);
                    canvas.drawRoundRect(px0, py0, px0 + pillW, py0 + pillH, dp(10), dp(10), pillPaint);

                    // White text for Today
                    textPaint.setColor(0xFFFFFFFF);
                    textPaint.setTextSize(dp(11.5f));
                    textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    canvas.drawText(DAYS[i], cx, dp(18), textPaint);

                    textPaint.setTextSize(dp(10.5f));
                    textPaint.setTypeface(Typeface.DEFAULT);
                    canvas.drawText(String.valueOf(dateOfMonth), cx, dp(34), textPaint);
                } else {
                    // Normal text
                    textPaint.setColor(i >= 5 ? 0xFF64748B : 0xFF1F2937);
                    textPaint.setTextSize(dp(11.5f));
                    textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    canvas.drawText(DAYS[i], cx, dp(18), textPaint);

                    textPaint.setColor(0xFF94A3B8);
                    textPaint.setTextSize(dp(10.5f));
                    textPaint.setTypeface(Typeface.DEFAULT);
                    canvas.drawText(String.valueOf(dateOfMonth), cx, dp(34), textPaint);
                }
            }

            // Bottom Divider Line
            linePaint.setColor(0xFFE5EAF2);
            linePaint.setStrokeWidth(dp(1));
            canvas.drawLine(0, height - dp(0.5f), width, height - dp(0.5f), linePaint);
        }
    }

    // =========================================================================
    // 2. CourseGridView (1-12 Periods, Scrollable, High density cards)
    // =========================================================================
    private class CourseGridView extends View {
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint todayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rectF = new RectF();

        private float downX, downY;
        private final int touchSlop;

        public CourseGridView(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setBackgroundColor(0xFFFFFFFF);
            cardBorderPaint.setStyle(Paint.Style.STROKE);
            cardBorderPaint.setStrokeWidth(dp(0.8f));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int rowH = dp(62);
            int totalHeight = rowH * 12 + dp(16);
            setMeasuredDimension(width, totalHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float leftColW = dp(32);
            float dayW = (width - leftColW) / 7.0f;
            float rowH = dp(62);

            Calendar cal = Calendar.getInstance();
            int todayDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1; // Mon=1..Sun=7
            boolean isThisWeek = (displayedWeek == currentRealWeek);

            // 1. Highlight Today Column Background
            if (isThisWeek && todayDayOfWeek >= 1 && todayDayOfWeek <= 7) {
                todayBgPaint.setColor(0xFFF0F4FE);
                float tx0 = leftColW + (todayDayOfWeek - 1) * dayW;
                canvas.drawRect(tx0, 0, tx0 + dayW, rowH * 12, todayBgPaint);
            }

            // 2. Draw Horizontal Grid Lines & Period Indicators
            gridLinePaint.setColor(0xFFE5EAF2);
            gridLinePaint.setStrokeWidth(dp(0.8f));

            textPaint.setTextAlign(Paint.Align.CENTER);
            for (int r = 1; r <= 12; r++) {
                float y = (r - 1) * rowH;

                // Period Number
                textPaint.setColor(0xFF475569);
                textPaint.setTextSize(dp(11.5f));
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText(String.valueOf(r), leftColW / 2.0f, y + dp(22), textPaint);

                // Period Start Time
                subTextPaint.setTextAlign(Paint.Align.CENTER);
                subTextPaint.setColor(0xFF94A3B8);
                subTextPaint.setTextSize(dp(8.0f));
                subTextPaint.setTypeface(Typeface.DEFAULT);
                canvas.drawText(PERIOD_TIMES[r - 1], leftColW / 2.0f, y + dp(35), subTextPaint);

                // Horizontal Line
                canvas.drawLine(leftColW, y, width, y, gridLinePaint);
            }
            canvas.drawLine(leftColW, 12 * rowH, width, 12 * rowH, gridLinePaint);

            // 3. Draw Vertical Column Divider
            canvas.drawLine(leftColW, 0, leftColW, 12 * rowH, gridLinePaint);
            gridLinePaint.setColor(0xFFF1F5F9);
            for (int d = 1; d < 7; d++) {
                float vx = leftColW + d * dayW;
                canvas.drawLine(vx, 0, vx, 12 * rowH, gridLinePaint);
            }

            // 4. Draw Course Cards
            float margin = dp(1.5f);
            float padX = dp(3.0f);
            float padY = dp(4.5f);
            float radius = dp(8.0f);

            for (Course c : allCourses) {
                if (!c.activeAt(displayedWeek)) continue;
                if (c.weekday < 1 || c.weekday > 7 || c.startPeriod < 1 || c.startPeriod > 12) continue;

                float x0 = leftColW + (c.weekday - 1) * dayW + margin;
                float y0 = (c.startPeriod - 1) * rowH + margin;
                float cardW = dayW - margin * 2;
                float cardH = c.periods * rowH - margin * 2;

                // Stable Hash Color Palette based strictly on Course Name
                int colorIdx = Math.abs((c.name != null ? c.name : "").hashCode()) % PALETTE.length;
                int[] theme = PALETTE[colorIdx];

                // Draw Card Background
                cardBgPaint.setColor(theme[0]);
                rectF.set(x0, y0, x0 + cardW, y0 + cardH);
                canvas.drawRoundRect(rectF, radius, radius, cardBgPaint);

                // Draw Card Border
                cardBorderPaint.setColor(theme[1]);
                canvas.drawRoundRect(rectF, radius, radius, cardBorderPaint);

                // Draw Text with Strict Clipping
                canvas.save();
                canvas.clipRect(x0, y0, x0 + cardW, y0 + cardH);

                float textW = cardW - padX * 2;
                float curY = y0 + padY;

                // Title TextPaint
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(theme[2]);
                textPaint.setTextSize(dp(10.2f));
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);

                // Secondary TextPaint
                subTextPaint.setTextAlign(Paint.Align.LEFT);
                subTextPaint.setColor(theme[3]);
                subTextPaint.setTextSize(dp(8.8f));
                subTextPaint.setTypeface(Typeface.DEFAULT);

                // Course Name (Intelligent Line Breaking)
                String name = c.name != null ? c.name : "";
                float titleLineH = dp(12.5f);

                int count = textPaint.breakText(name, 0, name.length(), true, textW, null);
                if (count >= name.length()) {
                    // 1 Line
                    canvas.drawText(name, x0 + padX, curY + dp(9.0f), textPaint);
                    curY += titleLineH + dp(2.0f);
                } else {
                    // 2 Lines
                    String line1 = name.substring(0, count);
                    String remaining = name.substring(count);

                    if (cardH >= dp(50)) {
                        String line2 = TextUtils.ellipsize(remaining, new android.text.TextPaint(textPaint), textW, TextUtils.TruncateAt.END).toString();
                        canvas.drawText(line1, x0 + padX, curY + dp(9.0f), textPaint);
                        canvas.drawText(line2, x0 + padX, curY + dp(9.0f) + titleLineH, textPaint);
                        curY += titleLineH * 2 + dp(2.5f);
                    } else {
                        String singleLine = TextUtils.ellipsize(name, new android.text.TextPaint(textPaint), textW, TextUtils.TruncateAt.END).toString();
                        canvas.drawText(singleLine, x0 + padX, curY + dp(9.0f), textPaint);
                        curY += titleLineH + dp(2.0f);
                    }
                }

                // Room / Classroom
                float subLineH = dp(11.5f);
                if (c.room != null && !c.room.trim().isEmpty()) {
                    if (curY + subLineH <= y0 + cardH - dp(2.0f)) {
                        String r = c.room.trim();
                        // Smart abbreviation for cards: if room has space (e.g. "校区 K-602"), show "@K-602"
                        String roomCode = r;
                        if (r.contains(" ")) {
                            String lastToken = r.substring(r.lastIndexOf(' ') + 1).trim();
                            if (!lastToken.isEmpty() && lastToken.length() < r.length()) {
                                roomCode = lastToken;
                            }
                        }
                        String roomStr = (roomCode.startsWith("@") ? "" : "@") + roomCode;
                        String roomText = TextUtils.ellipsize(roomStr, new android.text.TextPaint(subTextPaint), textW, TextUtils.TruncateAt.END).toString();
                        canvas.drawText(roomText, x0 + padX, curY + dp(8.0f), subTextPaint);
                        curY += subLineH;
                    }
                }

                // Teacher
                if (c.teacher != null && !c.teacher.trim().isEmpty()) {
                    if (curY + subLineH <= y0 + cardH - dp(2.0f)) {
                        String teacherText = TextUtils.ellipsize(c.teacher.trim(), new android.text.TextPaint(subTextPaint), textW, TextUtils.TruncateAt.END).toString();
                        canvas.drawText(teacherText, x0 + padX, curY + dp(8.0f), subTextPaint);
                    }
                }

                canvas.restore();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (Math.hypot(dx, dy) <= touchSlop) {
                        float leftColW = dp(32);
                        if (event.getX() >= leftColW) {
                            float dayW = (getWidth() - leftColW) / 7.0f;
                            float rowH = dp(62);
                            int day = (int) ((event.getX() - leftColW) / dayW) + 1;
                            int period = (int) (event.getY() / rowH) + 1;

                            for (Course c : allCourses) {
                                if (c.activeAt(displayedWeek) && c.weekday == day && period >= c.startPeriod && period < c.startPeriod + c.periods) {
                                    playSoundEffect(SoundEffectConstants.CLICK);
                                    if (onCourseClickListener != null) {
                                        onCourseClickListener.click(c);
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                    break;
            }
            return super.onTouchEvent(event);
        }
    }
}
