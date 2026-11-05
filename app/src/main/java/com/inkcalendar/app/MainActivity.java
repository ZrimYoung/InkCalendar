package com.inkcalendar.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public final class MainActivity extends Activity {
    private static final int PAPER = Color.WHITE, INK = Color.BLACK,
        SECONDARY = Color.BLACK, RULE = Color.BLACK, ACCENT = Color.BLACK;
    private EventStore store;
    private SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String today, selected, displayedMonth;
    private LinearLayout root, monthGrid, agenda;
    private TextView monthTitle, dayNumber, dateTitle, dayTitle;
    private ImageButton previous, next;
    private static Typeface serif;
    private static Typeface display, headline, labelFont, reading;
    private Locale displayLocale;
    private boolean registered;
    private Dialog editor, settingsDialog, dateDialog, deleteDialog;
    private EditText titleInput, timeInput;
    private String editingDate;
    private long editingId;
    private float textScale;

    private final Runnable midnight = () -> { refreshDate(); render(); scheduleDayChange(); };
    private final BroadcastReceiver clockChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refreshDate(); render(); scheduleDayChange(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new EventStore(this);
        preferences = getSharedPreferences("display", MODE_PRIVATE);
        applyLanguage();
        if (serif == null) serif = Typeface.createFromAsset(getAssets(), "fonts/NotoSerifSC-Regular.otf");
        if (display == null) {
            display = Typeface.createFromAsset(getAssets(), "fonts/NewsprintDisplay-Regular.ttf");
            headline = Typeface.createFromAsset(getAssets(), "fonts/NewsprintDisplay-Black.ttf");
            labelFont = Typeface.createFromAsset(getAssets(), "fonts/NewsprintDisplay-Semibold.ttf");
            reading = Typeface.createFromAsset(getAssets(), "fonts/NewsprintReading-Regular.ttf");
        }
        today = state == null ? Dates.today() : state.getString("today", Dates.today());
        selected = state == null ? today : state.getString("selected", today);
        displayedMonth = state == null ? Dates.month(today) : state.getString("month", Dates.month(today));
        refreshDate();
        buildScreen();
        if (state != null && state.getBoolean("editing")) {
            showEditor(state.getLong("eventId"), state.getString("eventDate"), state.getString("eventTitle"), state.getString("eventTime"));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshDate(); render(); applyDisplay(); scheduleDayChange();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(clockChanged, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(clockChanged, filter);
        registered = true;
    }
    @Override protected void onPause() {
        handler.removeCallbacks(midnight);
        if (registered) { unregisterReceiver(clockChanged); registered = false; }
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (editor != null) editor.dismiss();
        if (settingsDialog != null) settingsDialog.dismiss();
        if (dateDialog != null) dateDialog.dismiss();
        if (deleteDialog != null) deleteDialog.dismiss();
        store.close(); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("today", today); out.putString("selected", selected); out.putString("month", displayedMonth);
        if (editor != null && editor.isShowing()) {
            out.putBoolean("editing", true); out.putLong("eventId", editingId);
            out.putString("eventDate", editingDate);
            out.putString("eventTitle", titleInput.getText().toString());
            out.putString("eventTime", timeInput.getText().toString());
        }
        super.onSaveInstanceState(out);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyDisplay();
    }

    private void refreshDate() {
        String updated = Dates.today();
        if (!updated.equals(today)) {
            boolean followsToday = selected.equals(today);
            today = updated;
            if (followsToday) { selected = today; displayedMonth = Dates.month(today); }
        }
    }
    private void scheduleDayChange() {
        handler.removeCallbacks(midnight);
        handler.postDelayed(midnight, Dates.untilNextDay(System.currentTimeMillis(), TimeZone.getDefault()));
    }
    private void applyDisplay() {
        if (preferences.getBoolean("awake", true)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        boolean full = preferences.getBoolean("full", true);
        if (full) getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(full ? View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN : 0);
    }

    @SuppressWarnings("deprecation")
    private void applyLanguage() {
        displayLocale = "en".equals(preferences.getString("language", "zh")) ? Locale.US : Locale.SIMPLIFIED_CHINESE;
        Locale.setDefault(displayLocale);
        Configuration configuration = new Configuration(getResources().getConfiguration());
        configuration.setLocale(displayLocale);
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
    }
    private String formatDate(String date, String pattern) {
        return formatDate(date, pattern, displayLocale);
    }
    private String formatDate(String date, String pattern, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(Dates.parse(date).getTime());
    }
    private String monthLabel(String date) { return formatDate(date, displayLocale.equals(Locale.US) ? "MMMM yyyy" : "yyyy 年 M 月"); }
    private Locale alternateLocale() { return displayLocale.equals(Locale.US) ? Locale.SIMPLIFIED_CHINESE : Locale.US; }
    private String alternateString(int id) {
        Configuration config = new Configuration(getResources().getConfiguration()); config.setLocale(alternateLocale());
        return createConfigurationContext(config).getString(id);
    }
    private String alternateMonth(String date) {
        Locale locale = alternateLocale();
        return formatDate(date, locale.equals(Locale.US) ? "MMMM yyyy" : "yyyy 年 M 月", locale);
    }
    private String shortDate(String date) { return formatDate(date, displayLocale.equals(Locale.US) ? "MMM d" : "M月d日"); }

    private void buildScreen() {
        textScale = preferences.getBoolean("large", false) ? 1.18f : 1f;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false); scroll.setBackgroundColor(PAPER);
        root = column(); root.setPadding(dp(18), dp(12), dp(18), dp(18));
        scroll.addView(root);
        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 35) {
            scroll.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            scroll.requestApplyInsets();
        }
        LinearLayout top = row();
        TextView brand = label(R.string.app_name, 30, true);
        brand.setTypeface(fontFor(brand.getText(), headline), Typeface.BOLD);
        ((StableTextView) brand).reserveFor(alternateString(R.string.app_name), fontFor(alternateString(R.string.app_name), headline));
        top.addView(brand, weight());
        top.addView(icon(LineIcon.TODAY, R.string.go_today, () -> {
            if (selected.equals(Dates.today()) && displayedMonth.equals(Dates.month(Dates.today()))) {
                Toast.makeText(this, R.string.already_today, Toast.LENGTH_SHORT).show();
                return;
            }
            selected = Dates.today(); today = selected; displayedMonth = Dates.month(selected); render();
        }));
        top.addView(icon(LineIcon.SETTINGS, R.string.settings, this::showSettings));
        root.addView(top);
        LinearLayout.LayoutParams mastheadRule = new LinearLayout.LayoutParams(-1, dp(4));
        mastheadRule.setMargins(0, dp(12), 0, dp(12));
        root.addView(rule(INK), mastheadRule);

        boolean wide = getResources().getConfiguration().screenWidthDp >= 640 && getResources().getConfiguration().orientation == 2;
        LinearLayout layout = wide ? row() : column();
        layout.setGravity(Gravity.TOP);
        root.addView(layout, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout identity = column();
        dayNumber = text("", wide ? 104 : 84, false);
        dayNumber.setTypeface(headline);
        dayNumber.setIncludeFontPadding(false);
        dateTitle = stableText("", 16, false); dayTitle = stableText("", 21, false);
        dateTitle.setTextColor(SECONDARY);
        if (wide) {
            identity.setPadding(0, dp(8), dp(12), 0);
            identity.addView(dateTitle); identity.addView(dayNumber); identity.addView(dayTitle);
            layout.addView(identity, new LinearLayout.LayoutParams(0, -2, 0.32f));
            layout.addView(rule(RULE), new LinearLayout.LayoutParams(dp(1), -1));
        } else {
            LinearLayout hero = row(); hero.setPadding(0, dp(8), 0, dp(8));
            hero.addView(dayNumber);
            LinearLayout words = column(); words.setPadding(dp(18), 0, 0, 0);
            words.addView(dateTitle); words.addView(dayTitle);
            hero.addView(words, weight()); identity.addView(hero);
            layout.addView(identity);
        }
        LinearLayout calendarPanel = new MonthSwipeLayout(this, this::moveMonth);
        if (wide) {
            calendarPanel.setPadding(dp(12), 0, 0, 0);
            layout.addView(calendarPanel, new LinearLayout.LayoutParams(0, -2, 0.68f));
        }
        else layout.addView(calendarPanel);
        LinearLayout nav = row();
        previous = icon(LineIcon.PREVIOUS, R.string.previous_month, () -> moveMonth(-1));
        next = icon(LineIcon.NEXT, R.string.next_month, () -> moveMonth(1));
        monthTitle = stableText("", 20, true); monthTitle.setGravity(Gravity.CENTER);
        nav.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        nav.addView(monthTitle, weight());
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        calendarPanel.addView(nav);
        monthGrid = column(); calendarPanel.addView(monthGrid);
        agenda = column();
        agenda.setPadding(0, dp(10), 0, 0);
        LinearLayout.LayoutParams agendaRule = new LinearLayout.LayoutParams(-1, dp(4));
        agendaRule.setMargins(0, dp(16), 0, 0);
        if (wide) { identity.addView(rule(INK), agendaRule); identity.addView(agenda); }
        else { root.addView(rule(INK), agendaRule); root.addView(agenda); }
        render();
    }
    private void moveMonth(int by) {
        String target = Dates.addMonths(displayedMonth, by);
        if (target.compareTo("1900-01-01") < 0 || target.compareTo("2100-12-01") > 0) return;
        displayedMonth = target;
        selected = Dates.month(today).equals(target) ? today : target;
        render();
    }
    private void render() {
        if (monthGrid == null) return;
        dayNumber.setText(String.format(Locale.US, "%02d", Integer.parseInt(today.substring(8))));
        dateTitle.setText(monthLabel(today));
        ((StableTextView) dateTitle).reserveFor(alternateMonth(today), fontFor(alternateMonth(today), display));
        dayTitle.setText(formatDate(today, "EEEE"));
        String alternateDay = formatDate(today, "EEEE", alternateLocale());
        ((StableTextView) dayTitle).reserveFor(alternateDay, fontFor(alternateDay, display));
        monthTitle.setText(monthLabel(displayedMonth));
        ((StableTextView) monthTitle).reserveFor(alternateMonth(displayedMonth), fontFor(alternateMonth(displayedMonth), labelFont));
        previous.setEnabled(!displayedMonth.equals("1900-01-01")); next.setEnabled(!displayedMonth.equals("2100-12-01"));
        monthGrid.removeAllViews(); agenda.removeAllViews();
        try {
            String[] days = Dates.grid(displayedMonth);
            Set<String> marks = store.markedDays(days[0], days[41]);
            LinearLayout header = row();
            String[] weekLabels = displayLocale.equals(Locale.US) ? new String[]{"M", "T", "W", "T", "F", "S", "S"} : new String[]{"一", "二", "三", "四", "五", "六", "日"};
            for (String label : weekLabels) {
                TextView t = text(label, 12, true); t.setGravity(Gravity.CENTER); t.setTextColor(SECONDARY);
                header.addView(t, new LinearLayout.LayoutParams(0, dp(36), 1));
            }
            monthGrid.addView(header);
            for (int r = 0; r < 6; r++) {
                LinearLayout week = row();
                for (int col = 0; col < 7; col++) {
                    final String date = days[r * 7 + col];
                    String day = String.valueOf(Integer.parseInt(date.substring(8)));
                    TextView cell = text(day, 22, date.equals(today));
                    SpannableString contents = new SpannableString(day + (marks.contains(date) ? "\n•" : "\n\u00a0"));
                    contents.setSpan(new RelativeSizeSpan(0.55f), day.length() + 1, contents.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    cell.setText(contents);
                    cell.setGravity(Gravity.CENTER); cell.setIncludeFontPadding(false);
                    boolean inMonth = Dates.month(date).equals(displayedMonth);
                    if (!inMonth) cell.setTextSize(18 * textScale);
                    boolean valid = date.compareTo("1900-01-01") >= 0 && date.compareTo("2100-12-31") <= 0;
                    int base = date.equals(selected) ? INK : PAPER;
                    cell.setTextColor(interactiveText(base, inMonth ? INK : SECONDARY));
                    cell.setBackground(background(base, date.equals(today) ? 2 : 0));
                    if (date.equals(today)) cell.setTypeface(headline);
                    cell.setSelected(date.equals(selected));
                    cell.setContentDescription(date + (date.equals(today) ? ", " + getString(R.string.today) : "") + (date.equals(selected) ? ", " + getString(R.string.selected) : "") + (marks.contains(date) ? ", " + getString(R.string.has_events) : ""));
                    cell.setFocusable(valid); cell.setClickable(valid);
                    cell.setTag(date);
                    if (valid) cell.setOnClickListener(v -> {
                        if (selected.equals(date)) return;
                        boolean restoreFocus = v.isFocused();
                        selected = date; displayedMonth = Dates.month(date); render();
                        if (restoreFocus) {
                            View focusedDate = monthGrid.findViewWithTag(date);
                            if (focusedDate != null) focusedDate.requestFocus();
                        }
                    });
                    float systemScale = Math.max(1f, getResources().getConfiguration().fontScale);
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp((textScale > 1 ? 66 : 56) * systemScale), 1);
                    cp.setMargins(dp(1), dp(1), dp(1), dp(1)); week.addView(cell, cp);
                }
                monthGrid.addView(week);
                monthGrid.addView(rule(RULE), new LinearLayout.LayoutParams(-1, dp(1)));
            }
            boolean upcoming = selected.equals(today);
            LinearLayout agendaHeader = row();
            TextView title = stableText(upcoming ? getString(R.string.upcoming).toUpperCase(displayLocale) : shortDate(selected), 14, true);
            String alternateHeading = upcoming ? alternateString(R.string.upcoming).toUpperCase(alternateLocale())
                : formatDate(selected, alternateLocale().equals(Locale.US) ? "MMM d" : "M月d日", alternateLocale());
            ((StableTextView) title).reserveFor(alternateHeading, fontFor(alternateHeading, labelFont));
            title.setTextColor(ACCENT);
            if (Build.VERSION.SDK_INT >= 21) title.setLetterSpacing(0.09f);
            agendaHeader.addView(title, weight());
            agendaHeader.addView(icon(LineIcon.ADD, R.string.add_event, () -> showEditor(0, selected, "", "")));
            agenda.addView(agendaHeader);
            List<Event> events = store.between(selected, upcoming ? Dates.addDays(selected, 6) : selected);
            if (events.isEmpty()) {
                TextView empty = label(upcoming ? R.string.empty_upcoming : R.string.empty_day, 16, false);
                empty.setLineSpacing(dp(6), 1); agenda.addView(empty);
            }
            for (Event event : events) {
                String when = (upcoming ? (event.date.equals(today) ? getString(R.string.today) : shortDate(event.date)) + "  " : "") + (event.time.isEmpty() ? getString(R.string.all_day) : event.time);
                LinearLayout item = column(); item.setPadding(dp(4), dp(12), dp(4), dp(12));
                TextView meta = text(when, 13, false); meta.setTextColor(interactiveText(PAPER, SECONDARY));
                TextView content = text(event.title, 20, false);
                content.setTypeface(fontFor(event.title, reading));
                content.setLineSpacing(0, 1.3f); content.setTextColor(interactiveText(PAPER, INK));
                meta.setDuplicateParentStateEnabled(true); content.setDuplicateParentStateEnabled(true);
                item.addView(meta); item.addView(content);
                item.setBackground(background(PAPER, 0)); item.setMinimumHeight(dp(64)); item.setFocusable(true);
                item.setContentDescription(when + ", " + event.title + ", " + getString(R.string.edit_event));
                item.setOnClickListener(v -> showEditor(event.id, event.date, event.title, event.time));
                agenda.addView(item);
                agenda.addView(rule(RULE), new LinearLayout.LayoutParams(-1, dp(1)));
            }
        } catch (RuntimeException e) {
            agenda.removeAllViews(); agenda.addView(text(getString(R.string.read_failed), 16, false));
        }
    }

    private void showEditor(long id, String date, String title, String time) {
        editingId = id; editingDate = date;
        editor = new Dialog(this); editor.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = pageForm(id == 0 ? R.string.add_event : R.string.edit_event);
        if (id != 0) ((LinearLayout) form.getChildAt(0)).addView(icon(LineIcon.DELETE, R.string.delete, this::confirmDelete));
        fieldLabel(form, R.string.date);
        Button dateButton = button(date, () -> {});
        dateButton.setContentDescription(getString(R.string.choose_date));
        dateButton.setOnClickListener(v -> { hideKeyboard(titleInput); showDatePage(dateButton); });
        form.addView(dateButton);
        fieldLabel(form, R.string.event_title);
        titleInput = input(title, R.string.event_hint);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setTypeface(serif); titleInput.setMinLines(3); titleInput.setGravity(Gravity.TOP | Gravity.LEFT);
        titleInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        titleInput.setContentDescription(getString(R.string.event_title)); form.addView(titleInput);
        fieldLabel(form, R.string.time_optional);
        timeInput = input(time, 0); timeInput.setHint("09:30"); timeInput.setSingleLine(true);
        timeInput.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        timeInput.setTypeface(display); timeInput.setContentDescription(getString(R.string.time_description)); form.addView(timeInput);
        TextView error = errorLabel(form);
        form.addView(actions(getString(R.string.cancel), () -> editor.dismiss(), getString(R.string.save), () -> {
            hideKeyboard(timeInput);
            Event event;
            try { event = new Event(editingId, editingDate, titleInput.getText().toString(), timeInput.getText().toString()); }
            catch (IllegalArgumentException e) { showError(error, validationMessage(e)); return; }
            try { store.save(event); }
            catch (RuntimeException e) { showError(error, getString(R.string.save_failed)); return; }
            selected = event.date; displayedMonth = Dates.month(selected); editor.dismiss(); render();
        }));
        editor.setOnDismissListener(d -> {
            hideKeyboard(titleInput);
            if (dateDialog != null) dateDialog.dismiss();
            if (deleteDialog != null) deleteDialog.dismiss();
        });
        showPage(editor, form);
    }

    private void confirmDelete() {
        hideKeyboard(titleInput);
        deleteDialog = new Dialog(this); deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = pageForm(R.string.delete_confirm);
        TextView summary = text(titleInput.getText().toString(), 20, false);
        summary.setIncludeFontPadding(false); summary.setPadding(0, dp(12), 0, dp(12)); form.addView(summary);
        TextView error = errorLabel(form);
        form.addView(actions(getString(R.string.cancel), () -> deleteDialog.dismiss(), getString(R.string.delete), () -> {
            try { store.delete(editingId); deleteDialog.dismiss(); editor.dismiss(); render(); }
            catch (RuntimeException e) { showError(error, getString(R.string.delete_failed)); }
        }));
        showPage(deleteDialog, form);
    }

    private void showDatePage(Button dateButton) {
        dateDialog = new Dialog(this); dateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = pageForm(R.string.choose_date);
        fieldLabel(form, R.string.date_format);
        EditText value = input(editingDate, 0); value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        value.setTypeface(display); value.setContentDescription(getString(R.string.date_format)); form.addView(value);
        final String[] month = { Dates.month(editingDate) };
        final Runnable[] draw = new Runnable[1];
        MonthSwipeLayout calendar = new MonthSwipeLayout(this, by -> {
            String target = Dates.addMonths(month[0], by);
            if (target.compareTo("1900-01-01") >= 0 && target.compareTo("2100-12-01") <= 0) { month[0] = target; draw[0].run(); }
        });
        LinearLayout.LayoutParams calendarBounds = new LinearLayout.LayoutParams(-1, -2); calendarBounds.topMargin = dp(16); form.addView(calendar, calendarBounds);
        draw[0] = () -> {
            calendar.removeAllViews();
            LinearLayout nav = row();
            ImageButton prev = icon(LineIcon.PREVIOUS, R.string.previous_month, () -> { month[0] = Dates.addMonths(month[0], -1); draw[0].run(); });
            ImageButton nextMonth = icon(LineIcon.NEXT, R.string.next_month, () -> { month[0] = Dates.addMonths(month[0], 1); draw[0].run(); });
            prev.setEnabled(!month[0].equals("1900-01-01")); nextMonth.setEnabled(!month[0].equals("2100-12-01"));
            StableTextView heading = stableText(monthLabel(month[0]), 20, true); heading.setGravity(Gravity.CENTER);
            heading.reserveFor(alternateMonth(month[0]), fontFor(alternateMonth(month[0]), labelFont));
            nav.addView(prev); nav.addView(heading, weight()); nav.addView(nextMonth); calendar.addView(nav);
            LinearLayout weekdays = row();
            String[] labels = displayLocale.equals(Locale.US) ? new String[]{"M","T","W","T","F","S","S"} : new String[]{"一","二","三","四","五","六","日"};
            for (String name : labels) {
                TextView day = stableText(name, 12, true); day.setGravity(Gravity.CENTER); weekdays.addView(day, new LinearLayout.LayoutParams(0, dp(36), 1));
            }
            calendar.addView(weekdays);
            String[] days = Dates.grid(month[0]);
            for (int r = 0; r < 6; r++) {
                LinearLayout week = row();
                for (int c = 0; c < 7; c++) {
                    String key = days[r * 7 + c]; boolean chosen = key.equals(value.getText().toString());
                    TextView cell = text(String.valueOf(Integer.parseInt(key.substring(8))), Dates.month(key).equals(month[0]) ? 20 : 16, chosen);
                    cell.setIncludeFontPadding(false); cell.setGravity(Gravity.CENTER);
                    cell.setTextColor(interactiveText(chosen ? INK : PAPER, INK)); cell.setBackground(background(chosen ? INK : PAPER, key.equals(today) ? 1 : 0));
                    cell.setContentDescription(key + (chosen ? ", " + getString(R.string.selected) : "")); cell.setSelected(chosen);
                    boolean valid = key.compareTo("1900-01-01") >= 0 && key.compareTo("2100-12-31") <= 0;
                    cell.setEnabled(valid); cell.setFocusable(valid);
                    if (valid) cell.setOnClickListener(v -> { hideKeyboard(value); value.setText(key); });
                    week.addView(cell, new LinearLayout.LayoutParams(0, dp(48 * Math.max(textScale, getResources().getConfiguration().fontScale)), 1));
                }
                calendar.addView(week); calendar.addView(rule(INK), new LinearLayout.LayoutParams(-1, dp(1)));
            }
        };
        value.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { Dates.parse(s.toString()); month[0] = Dates.month(s.toString()); draw[0].run(); }
                catch (IllegalArgumentException ignored) { /* Keep the current month while a date is incomplete. */ }
            }
            public void afterTextChanged(Editable s) {}
        });
        draw[0].run();
        TextView error = errorLabel(form);
        form.addView(actions(getString(R.string.cancel), () -> dateDialog.dismiss(), getString(R.string.save), () -> {
            String chosen = value.getText().toString().trim();
            try { Dates.parse(chosen); }
            catch (IllegalArgumentException e) { showError(error, getString(R.string.date_invalid)); return; }
            editingDate = chosen; dateButton.setText(StableTextView.uniformLines(chosen, dateButton)); hideKeyboard(value); dateDialog.dismiss();
        }));
        dateDialog.setOnDismissListener(d -> hideKeyboard(value));
        showPage(dateDialog, form);
    }

    private LinearLayout pageForm(int title) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(20), dp(20), dp(20)); form.setBackgroundColor(PAPER);
        form.setFocusableInTouchMode(true);
        LinearLayout header = row(); header.addView(label(title, 26, true), weight()); form.addView(header);
        LinearLayout.LayoutParams divider = new LinearLayout.LayoutParams(-1, dp(4)); divider.setMargins(0, dp(12), 0, dp(8)); form.addView(rule(INK), divider);
        return form;
    }
    private void fieldLabel(LinearLayout form, int id) {
        TextView field = label(id, 15, false); field.setPadding(0, dp(16), 0, dp(8)); form.addView(field);
    }
    private EditText input(String value, int hint) {
        EditText field = new EditText(this); field.setTextSize(20 * textScale); field.setTypeface(serif);
        field.setTextColor(INK); field.setHintTextColor(INK); field.setIncludeFontPadding(false);
        field.setBackgroundResource(R.drawable.ink_input); field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setMinimumHeight(dp(48)); field.setText(value); if (hint != 0) field.setHint(hint);
        return field;
    }
    private TextView errorLabel(LinearLayout form) {
        TextView error = stableText("", 15, true); error.setPadding(0, dp(12), 0, 0); error.setVisibility(View.GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); form.addView(error); return error;
    }
    private void showError(TextView error, String message) {
        error.setText(message); error.setVisibility(View.VISIBLE);
        error.post(() -> error.requestRectangleOnScreen(new Rect(0, 0, error.getWidth(), error.getHeight()), true));
    }
    private LinearLayout actions(String cancel, Runnable cancelAction, String accept, Runnable acceptAction) {
        LinearLayout actions = row(); actions.setPadding(0, dp(20), 0, 0);
        actions.addView(button(cancel, cancelAction), weight());
        LinearLayout.LayoutParams bounds = weight(); bounds.leftMargin = dp(12);
        actions.addView(button(accept, acceptAction), bounds); return actions;
    }
    private void hideKeyboard(View input) {
        if (input != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
    }
    private void showPage(Dialog page, LinearLayout form) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(PAPER);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER); scroll.addView(form); page.setContentView(scroll);
        Window window = page.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(shape(PAPER, 0)); window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            if (preferences.getBoolean("awake", true)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (preferences.getBoolean("full", true)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
        page.show(); if (window != null) window.setLayout(-1, -1);
        form.requestFocus();
    }
    private void showSettings() {
        settingsDialog = new Dialog(this);
        settingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = column(); form.setPadding(dp(20), dp(20), dp(20), dp(20));
        form.setBackgroundColor(PAPER);
        TextView heading = label(R.string.settings, 26, true);
        form.addView(heading);
        LinearLayout.LayoutParams divider = new LinearLayout.LayoutParams(-1, dp(4));
        divider.setMargins(0, dp(12), 0, dp(12)); form.addView(rule(INK), divider);
        CheckBox awake = check(getString(R.string.keep_awake), preferences.getBoolean("awake", true));
        CheckBox full = check(getString(R.string.full_screen), preferences.getBoolean("full", true));
        CheckBox large = check(getString(R.string.large_text), preferences.getBoolean("large", false));
        form.addView(awake); form.addView(full); form.addView(large);
        TextView languageTitle = label(R.string.language, 16, false);
        languageTitle.setPadding(0, dp(16), 0, dp(8)); form.addView(languageTitle);
        RadioGroup languages = new RadioGroup(this);
        RadioButton chinese = new RadioButton(this); chinese.setId(View.generateViewId()); chinese.setText("简体中文"); chinese.setMinHeight(dp(48));
        RadioButton english = new RadioButton(this); english.setId(View.generateViewId()); english.setText("English"); english.setMinHeight(dp(48));
        styleChoice(chinese, true); styleChoice(english, true);
        languages.addView(chinese); languages.addView(english);
        languages.check(displayLocale.equals(Locale.US) ? english.getId() : chinese.getId()); form.addView(languages);
        TextView note = label(R.string.settings_note, 14, false);
        note.setPadding(0, dp(12), 0, 0); note.setLineSpacing(dp(5), 1); form.addView(note);
        LinearLayout actions = row(); actions.setPadding(0, dp(20), 0, 0);
        actions.addView(button(getString(R.string.cancel), () -> settingsDialog.dismiss()), weight());
        LinearLayout.LayoutParams saveBounds = weight(); saveBounds.leftMargin = dp(12);
        actions.addView(button(getString(R.string.save), () -> {
                preferences.edit().putBoolean("awake", awake.isChecked()).putBoolean("full", full.isChecked()).putBoolean("large", large.isChecked())
                    .putString("language", languages.getCheckedRadioButtonId() == english.getId() ? "en" : "zh").apply();
                settingsDialog.dismiss();
                applyLanguage(); buildScreen(); applyDisplay();
            }), saveBounds);
        form.addView(actions);
        ScrollView scroll = new ScrollView(this); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER); scroll.addView(form);
        settingsDialog.setContentView(scroll);
        Window window = settingsDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(shape(PAPER, 0));
            window.setWindowAnimations(0); window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if (preferences.getBoolean("full", true)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
        settingsDialog.show();
        if (window != null) window.setLayout(-1, -1);
    }
    private CheckBox check(String title, boolean checked) {
        CheckBox box = new CheckBox(this); box.setText(title); box.setMinHeight(dp(48));
        styleChoice(box, false); box.setChecked(checked); return box;
    }
    private void styleChoice(android.widget.CompoundButton choice, boolean radio) {
        choice.setButtonDrawable(new ChoiceMark(getResources().getDisplayMetrics().density, radio));
        choice.setTextColor(INK); choice.setTextSize(18 * textScale);
        choice.setTypeface(fontFor(choice.getText(), display));
        choice.setIncludeFontPadding(false);
        choice.setText(StableTextView.uniformLines(choice.getText(), choice));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, shape(PAPER, 2));
        states.addState(new int[]{}, shape(PAPER, 0));
        choice.setBackground(states);
        choice.setMinHeight(dp(48)); choice.setMinimumHeight(dp(48));
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextColor(INK); view.setTextSize(size * textScale);
        Typeface face = fontFor(value.isEmpty() ? (displayLocale.equals(Locale.US) ? "A" : "中") : value, bold ? labelFont : display);
        view.setTypeface(face, face == serif && bold ? Typeface.BOLD : Typeface.NORMAL); return view;
    }
    private StableTextView stableText(String value, int size, boolean bold) {
        StableTextView view = new StableTextView(this);
        view.setTextSize(size * textScale); view.setTextColor(INK); view.setGravity(Gravity.CENTER_VERTICAL);
        Typeface face = fontFor(value.isEmpty() ? (displayLocale.equals(Locale.US) ? "A" : "中") : value, bold ? labelFont : display);
        view.setTypeface(face, face == serif && bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setText(value); return view;
    }
    private StableTextView label(int id, int size, boolean bold) {
        StableTextView view = stableText(getString(id), size, bold);
        String alternate = alternateString(id);
        view.reserveFor(alternate, fontFor(alternate, bold ? labelFont : display));
        return view;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setTextColor(Color.BLACK); b.setTextSize(16 * textScale);
        b.setTypeface(fontFor(value, display));
        b.setIncludeFontPadding(false); b.setText(StableTextView.uniformLines(value, b));
        b.setTextColor(interactiveText(PAPER, INK));
        b.setAllCaps(false); b.setMinimumWidth(dp(48)); b.setMinWidth(dp(48)); b.setMinimumHeight(dp(48)); b.setMinHeight(dp(48));
        b.setPadding(dp(10), dp(4), dp(10), dp(4)); b.setBackground(background(PAPER, 1));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private ImageButton icon(int kind, int label, Runnable action) {
        ImageButton button = new ImageButton(this);
        button.setImageDrawable(new LineIcon(kind, getResources().getDisplayMetrics().density));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(dp(48), dp(48));
        bounds.leftMargin = dp(8); button.setLayoutParams(bounds);
        button.setBackground(background(PAPER, 1));
        button.setContentDescription(getString(label));
        button.setOnClickListener(v -> action.run());
        button.setOnLongClickListener(v -> { toast(getString(label)); return true; });
        return button;
    }
    private String validationMessage(IllegalArgumentException error) {
        String key = error.getMessage();
        if ("title_required".equals(key)) return getString(R.string.title_required);
        if ("title_too_long".equals(key)) return getString(R.string.title_too_long);
        if ("time_invalid".equals(key)) return getString(R.string.time_invalid);
        return getString(R.string.date_invalid);
    }
    private StateListDrawable background(int color, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, shape(INK, 2));
        GradientDrawable focus = shape(color, 3);
        if (color == INK) focus.setStroke(dp(3), PAPER);
        states.addState(new int[]{android.R.attr.state_focused}, focus);
        states.addState(new int[]{}, shape(color, border)); return states;
    }
    private ColorStateList interactiveText(int background, int normal) {
        return new ColorStateList(new int[][]{new int[]{android.R.attr.state_pressed}, new int[]{}},
            new int[]{PAPER, background == INK ? PAPER : normal});
    }
    private Typeface fontFor(CharSequence value, Typeface latin) {
        // The Latin families do not include CJK. Keep Chinese text legible on API 19.
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) >= 0x2e80) return serif;
        return latin;
    }
    private GradientDrawable shape(int color, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color);
        if (border > 0) d.setStroke(dp(border), INK); return d;
    }
    private View rule(int color) { View v = new View(this); v.setBackgroundColor(color); return v; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
