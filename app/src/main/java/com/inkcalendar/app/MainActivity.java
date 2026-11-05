package com.inkcalendar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public final class MainActivity extends Activity {
    private EventStore store;
    private SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String today, selected, displayedMonth;
    private LinearLayout root, monthGrid, agenda;
    private TextView monthTitle, dayNumber, dateTitle, dayTitle;
    private ImageButton previous, next;
    private static Typeface serif;
    private Locale displayLocale;
    private boolean registered;
    private AlertDialog editor;
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
        SimpleDateFormat format = new SimpleDateFormat(pattern, displayLocale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(Dates.parse(date).getTime());
    }
    private String monthLabel(String date) { return formatDate(date, displayLocale.equals(Locale.US) ? "MMMM yyyy" : "yyyy 年 M 月"); }
    private String shortDate(String date) { return formatDate(date, displayLocale.equals(Locale.US) ? "MMM d" : "M月d日"); }

    private void buildScreen() {
        textScale = preferences.getBoolean("large", false) ? 1.18f : 1f;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false); scroll.setBackgroundColor(Color.WHITE);
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
        top.addView(new View(this), weight());
        top.addView(icon(LineIcon.TODAY, R.string.go_today, () -> {
            selected = Dates.today(); today = selected; displayedMonth = Dates.month(selected); render();
        }));
        top.addView(icon(LineIcon.SETTINGS, R.string.settings, this::showSettings));
        root.addView(top);

        boolean wide = getResources().getConfiguration().screenWidthDp >= 640 && getResources().getConfiguration().orientation == 2;
        LinearLayout layout = wide ? row() : column();
        layout.setGravity(Gravity.TOP);
        root.addView(layout, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout identity = column();
        dayNumber = text("", wide ? 104 : 84, false);
        if (Build.VERSION.SDK_INT >= 21) dayNumber.setFontFeatureSettings("tnum");
        // Date text remains black; the selected calendar cell is outlined, today is filled.
        dateTitle = text("", 16, false); dayTitle = text("", 21, false);
        if (wide) {
            identity.setPadding(0, dp(16), dp(24), 0);
            identity.addView(dateTitle); identity.addView(dayNumber); identity.addView(dayTitle);
            layout.addView(identity, new LinearLayout.LayoutParams(0, -2, 0.36f));
        } else {
            LinearLayout hero = row(); hero.setPadding(0, dp(8), 0, dp(8));
            hero.addView(dayNumber);
            LinearLayout words = column(); words.setPadding(dp(18), 0, 0, 0);
            words.addView(dateTitle); words.addView(dayTitle);
            hero.addView(words, weight()); identity.addView(hero);
            layout.addView(identity);
        }
        LinearLayout calendarPanel = column();
        if (wide) layout.addView(calendarPanel, new LinearLayout.LayoutParams(0, -2, 0.64f));
        else layout.addView(calendarPanel);
        LinearLayout nav = row();
        previous = icon(LineIcon.PREVIOUS, R.string.previous_month, () -> moveMonth(-1));
        next = icon(LineIcon.NEXT, R.string.next_month, () -> moveMonth(1));
        monthTitle = text("", 20, false); monthTitle.setGravity(Gravity.CENTER);
        nav.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        nav.addView(monthTitle, weight());
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        calendarPanel.addView(nav);
        monthGrid = column(); calendarPanel.addView(monthGrid);
        agenda = column();
        agenda.setPadding(0, dp(10), 0, 0);
        if (wide) { identity.addView(rule(), new LinearLayout.LayoutParams(-1, dp(1))); identity.addView(agenda); }
        else { root.addView(rule(), new LinearLayout.LayoutParams(-1, dp(1))); root.addView(agenda); }
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
        dayTitle.setText(formatDate(today, "EEEE"));
        monthTitle.setText(monthLabel(displayedMonth));
        previous.setEnabled(!displayedMonth.equals("1900-01-01")); next.setEnabled(!displayedMonth.equals("2100-12-01"));
        monthGrid.removeAllViews(); agenda.removeAllViews();
        try {
            String[] days = Dates.grid(displayedMonth);
            Set<String> marks = store.markedDays(days[0], days[41]);
            LinearLayout header = row();
            String[] weekLabels = displayLocale.equals(Locale.US) ? new String[]{"M", "T", "W", "T", "F", "S", "S"} : new String[]{"一", "二", "三", "四", "五", "六", "日"};
            for (String label : weekLabels) {
                TextView t = text(label, 14, false); t.setGravity(Gravity.CENTER);
                header.addView(t, new LinearLayout.LayoutParams(0, dp(30), 1));
            }
            monthGrid.addView(header);
            for (int r = 0; r < 6; r++) {
                LinearLayout week = row();
                for (int col = 0; col < 7; col++) {
                    final String date = days[r * 7 + col];
                    TextView cell = text(Integer.parseInt(date.substring(8)) + (marks.contains(date) ? "\n•" : "\n"), 19, date.equals(today));
                    cell.setGravity(Gravity.CENTER); cell.setIncludeFontPadding(false);
                    boolean inMonth = Dates.month(date).equals(displayedMonth);
                    boolean valid = date.compareTo("1900-01-01") >= 0 && date.compareTo("2100-12-31") <= 0;
                    cell.setTextColor(date.equals(today) ? Color.WHITE : inMonth ? Color.BLACK : Color.rgb(100, 100, 100));
                    cell.setBackground(background(date.equals(today) ? Color.BLACK : Color.WHITE, date.equals(selected) ? 2 : 0));
                    cell.setContentDescription(date + (date.equals(today) ? ", " + getString(R.string.today) : "") + (date.equals(selected) ? ", " + getString(R.string.selected) : "") + (marks.contains(date) ? ", " + getString(R.string.has_events) : ""));
                    cell.setFocusable(valid); cell.setClickable(valid);
                    if (valid) cell.setOnClickListener(v -> {
                        if (selected.equals(date)) return;
                        selected = date; displayedMonth = Dates.month(date); render();
                    });
                    float systemScale = Math.max(1f, getResources().getConfiguration().fontScale);
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp((textScale > 1 ? 62 : 50) * systemScale), 1);
                    cp.setMargins(dp(1), dp(1), dp(1), dp(1)); week.addView(cell, cp);
                }
                monthGrid.addView(week);
            }
            boolean upcoming = selected.equals(today);
            LinearLayout agendaHeader = row();
            TextView title = text(upcoming ? getString(R.string.upcoming) : shortDate(selected), 18, false);
            agendaHeader.addView(title, weight());
            agendaHeader.addView(icon(LineIcon.ADD, R.string.add_event, () -> showEditor(0, selected, "", "")));
            agenda.addView(agendaHeader);
            List<Event> events = store.between(selected, upcoming ? Dates.addDays(selected, 6) : selected);
            if (events.isEmpty()) {
                TextView empty = text(getString(upcoming ? R.string.empty_upcoming : R.string.empty_day), 16, false);
                empty.setLineSpacing(dp(6), 1); agenda.addView(empty);
            }
            for (Event event : events) {
                String when = (upcoming ? (event.date.equals(today) ? getString(R.string.today) : shortDate(event.date)) + "  " : "") + (event.time.isEmpty() ? getString(R.string.all_day) : event.time);
                LinearLayout item = column(); item.setPadding(0, dp(10), 0, dp(10));
                item.addView(text(when, 14, false)); item.addView(text(event.title, 18, false));
                item.setBackground(background(Color.WHITE, 0)); item.setMinimumHeight(dp(56)); item.setFocusable(true);
                item.setContentDescription(when + ", " + event.title + ", " + getString(R.string.edit_event));
                item.setOnClickListener(v -> showEditor(event.id, event.date, event.title, event.time));
                agenda.addView(item);
            }
        } catch (RuntimeException e) {
            agenda.removeAllViews(); agenda.addView(text(getString(R.string.read_failed), 16, false));
        }
    }

    private void showEditor(long id, String date, String title, String time) {
        editingId = id; editingDate = date;
        LinearLayout form = column(); form.setPadding(dp(20), dp(8), dp(20), dp(8));
        Button dateButton = button(getString(R.string.date) + "  " + date, () -> {
            Calendar c = Dates.parse(editingDate);
            DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
                editingDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                ((Button) form.getChildAt(0)).setText(getString(R.string.date) + "  " + editingDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            Calendar min = Calendar.getInstance(); min.clear(); min.set(1900, 0, 1);
            Calendar max = Calendar.getInstance(); max.clear(); max.set(2100, 11, 31, 23, 59);
            picker.getDatePicker().setMinDate(min.getTimeInMillis()); picker.getDatePicker().setMaxDate(max.getTimeInMillis());
            picker.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.save), picker);
            picker.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (android.content.DialogInterface.OnClickListener) null);
            picker.show(); styleDialog(picker);
        });
        form.addView(dateButton);
        titleInput = new EditText(this); titleInput.setText(title); titleInput.setHint(R.string.event_hint);
        titleInput.setTextColor(Color.BLACK); titleInput.setTextSize(20); titleInput.setMinLines(2);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        titleInput.setContentDescription(getString(R.string.event_title)); form.addView(titleInput);
        TextView help = text(getString(R.string.time_optional), 14, false); form.addView(help);
        timeInput = new EditText(this); timeInput.setText(time); timeInput.setHint("09:30"); timeInput.setSingleLine(true);
        timeInput.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        timeInput.setTextColor(Color.BLACK); timeInput.setContentDescription(getString(R.string.time_description)); form.addView(timeInput);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(id == 0 ? R.string.add_event : R.string.edit_event)
            .setView(scroll).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null);
        if (id != 0) builder.setNeutralButton(R.string.delete, null);
        editor = builder.create();
        editor.setOnShowListener(d -> {
            editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                Event event;
                try { event = new Event(editingId, editingDate, titleInput.getText().toString(), timeInput.getText().toString()); }
                catch (IllegalArgumentException error) { toast(validationMessage(error)); return; }
                try { store.save(event); }
                catch (RuntimeException error) { toast(getString(R.string.save_failed)); return; }
                selected = event.date; displayedMonth = Dates.month(selected); editor.dismiss(); render();
            });
            if (id != 0) editor.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                AlertDialog confirm = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm).setMessage(title).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try { store.delete(id); editor.dismiss(); render(); }
                    catch (RuntimeException error) { toast(getString(R.string.delete_failed)); }
                }).create();
                confirm.show(); styleDialog(confirm);
            });
        });
        editor.show(); styleDialog(editor);
    }
    private void showSettings() {
        LinearLayout form = column(); form.setPadding(dp(20), dp(8), dp(20), dp(8));
        CheckBox awake = check(getString(R.string.keep_awake), preferences.getBoolean("awake", true));
        CheckBox full = check(getString(R.string.full_screen), preferences.getBoolean("full", true));
        CheckBox large = check(getString(R.string.large_text), preferences.getBoolean("large", false));
        form.addView(awake); form.addView(full); form.addView(large);
        TextView languageTitle = text(getString(R.string.language), 16, false);
        languageTitle.setPadding(0, dp(16), 0, dp(8)); form.addView(languageTitle);
        RadioGroup languages = new RadioGroup(this);
        RadioButton chinese = new RadioButton(this); chinese.setId(View.generateViewId()); chinese.setText("简体中文"); chinese.setMinHeight(dp(48));
        RadioButton english = new RadioButton(this); english.setId(View.generateViewId()); english.setText("English"); english.setMinHeight(dp(48));
        languages.addView(chinese); languages.addView(english);
        languages.check(displayLocale.equals(Locale.US) ? english.getId() : chinese.getId()); form.addView(languages);
        TextView note = text(getString(R.string.settings_note), 14, false);
        note.setPadding(0, dp(12), 0, 0); note.setLineSpacing(dp(5), 1); form.addView(note);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog settings = new AlertDialog.Builder(this).setTitle(R.string.settings).setView(scroll).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                preferences.edit().putBoolean("awake", awake.isChecked()).putBoolean("full", full.isChecked()).putBoolean("large", large.isChecked())
                    .putString("language", languages.getCheckedRadioButtonId() == english.getId() ? "en" : "zh").apply();
                applyLanguage(); buildScreen(); applyDisplay();
            }).create();
        settings.show(); styleDialog(settings);
    }
    private CheckBox check(String title, boolean checked) {
        CheckBox box = new CheckBox(this); box.setText(title); box.setTextColor(Color.BLACK); box.setTextSize(18); box.setMinHeight(dp(48)); box.setChecked(checked); return box;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextColor(Color.BLACK); view.setTextSize(size * textScale);
        view.setTypeface(serif, bold ? Typeface.BOLD : Typeface.NORMAL); return view;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setTextColor(Color.BLACK); b.setTextSize(16 * textScale);
        b.setTypeface(serif);
        b.setAllCaps(false); b.setMinimumWidth(dp(48)); b.setMinWidth(dp(48)); b.setMinimumHeight(dp(48)); b.setMinHeight(dp(48));
        b.setPadding(dp(10), dp(4), dp(10), dp(4)); b.setBackground(background(Color.WHITE, 1));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private ImageButton icon(int kind, int label, Runnable action) {
        ImageButton button = new ImageButton(this);
        button.setImageDrawable(new LineIcon(kind, getResources().getDisplayMetrics().density));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        button.setBackground(background(Color.WHITE, 0));
        button.setContentDescription(getString(label));
        button.setOnClickListener(v -> action.run());
        button.setOnLongClickListener(v -> { toast(getString(label)); return true; });
        return button;
    }
    private void styleDialog(AlertDialog dialog) {
        if (dialog.getWindow() == null) return;
        dialog.getWindow().setWindowAnimations(0);
        applyTypeface(dialog.getWindow().getDecorView());
    }
    private void applyTypeface(View view) {
        if (view instanceof TextView) ((TextView) view).setTypeface(serif);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyTypeface(group.getChildAt(i));
        }
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
        states.addState(new int[]{android.R.attr.state_pressed}, shape(color == Color.BLACK ? Color.BLACK : Color.LTGRAY, 2));
        states.addState(new int[]{android.R.attr.state_focused}, shape(color, 3));
        states.addState(new int[]{}, shape(color, border)); return states;
    }
    private GradientDrawable shape(int color, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color);
        if (border > 0) d.setStroke(dp(border), Color.BLACK); return d;
    }
    private View rule() { View v = new View(this); v.setBackgroundColor(Color.BLACK); return v; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
