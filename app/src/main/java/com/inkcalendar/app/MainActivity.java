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
import android.graphics.Paint;
import android.text.TextUtils;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
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
    private Almanac almanac;
    private LinearLayout milestonePanel;
    private TextView calendarDetails, deskDetails;
    private Dialog futureDialog, milestonesDialog, milestoneEditor;
    private Milestone milestoneDraft;
    private EditText milestoneTitle;
    private CheckBox milestoneLunar, milestonePinned;
    private RadioGroup milestoneKinds;
    private String milestoneDate;
    private boolean editingCompleted;
    private interface DateSelected { void accept(String date); }

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

    private final Runnable midnight = () -> { refreshDate(); render(); refreshOpenLists(); scheduleDayChange(); };
    private final BroadcastReceiver clockChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refreshDate(); render(); refreshOpenLists(); scheduleDayChange(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new EventStore(this);
        try { almanac = new Almanac(new InputStreamReader(getAssets().open("calendar/months.csv"), "UTF-8"),
            new InputStreamReader(getAssets().open("calendar/terms.csv"), "UTF-8")); }
        catch (IOException e) { throw new IllegalStateException("Missing packaged calendar data", e); }
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
        if (state != null && state.getBoolean("futureOpen")) showFuture();
        if (state != null && state.getBoolean("milestonesOpen")) showMilestones();
        if (state != null && state.getBoolean("editing")) {
            showEditor(state.getLong("eventId"), state.getString("eventDate"), state.getString("eventTitle"), state.getString("eventTime"));
            editingCompleted = state.getBoolean("eventCompleted");
        }
        if (state != null && state.getBoolean("milestoneEditing")) {
            showMilestoneEditor(new Milestone(state.getLong("milestoneId"),state.getString("milestoneDate"),
                state.getString("milestoneTitle").trim().isEmpty()?"draft":state.getString("milestoneTitle"),state.getInt("milestoneKind"),state.getBoolean("milestoneLunar"),state.getBoolean("milestonePinned")));
            milestoneTitle.setText(state.getString("milestoneTitle"));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshDate(); render(); refreshOpenLists(); applyDisplay(); scheduleDayChange();
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
        if (futureDialog != null) futureDialog.dismiss();
        if (milestonesDialog != null) milestonesDialog.dismiss();
        if (milestoneEditor != null) milestoneEditor.dismiss();
        store.close(); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("today", today); out.putString("selected", selected); out.putString("month", displayedMonth);
        if (editor != null && editor.isShowing()) {
            out.putBoolean("editing", true); out.putLong("eventId", editingId);
            out.putString("eventDate", editingDate);
            out.putString("eventTitle", titleInput.getText().toString());
            out.putString("eventTime", timeInput.getText().toString());
            out.putBoolean("eventCompleted", editingCompleted);
        }
        out.putBoolean("futureOpen",futureDialog != null && futureDialog.isShowing());
        out.putBoolean("milestonesOpen",milestonesDialog != null && milestonesDialog.isShowing());
        if(milestoneEditor != null && milestoneEditor.isShowing()) {
            out.putBoolean("milestoneEditing",true);out.putLong("milestoneId",milestoneDraft.id);
            out.putString("milestoneDate",milestoneDate);out.putString("milestoneTitle",milestoneTitle.getText().toString());
            out.putInt("milestoneKind",selectedMilestoneKind());
            out.putBoolean("milestoneLunar",milestoneLunar.isChecked());out.putBoolean("milestonePinned",milestonePinned.isChecked());
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

        boolean wide = !preferences.getBoolean("desk",false) && getResources().getConfiguration().screenWidthDp >= 640 && getResources().getConfiguration().orientation == 2;
        LinearLayout layout = wide ? row() : column();
        layout.setGravity(Gravity.TOP);
        root.addView(layout, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout identity = column();
        dayNumber = text("", preferences.getBoolean("desk",false) ? 120 : wide ? 104 : 84, false);
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
        deskDetails=stableText("",16,false);
        if(preferences.getBoolean("desk",false))identity.addView(deskDetails);
        LinearLayout calendarPanel = new MonthSwipeLayout(this, this::moveMonth);
        if (wide) {
            calendarPanel.setPadding(dp(12), 0, 0, 0);
            layout.addView(calendarPanel, new LinearLayout.LayoutParams(0, -2, 0.68f));
        }
        else layout.addView(calendarPanel);
        calendarPanel.setVisibility(preferences.getBoolean("desk",false) ? View.GONE : View.VISIBLE);
        LinearLayout nav = row();
        previous = icon(LineIcon.PREVIOUS, R.string.previous_month, () -> moveMonth(-1));
        next = icon(LineIcon.NEXT, R.string.next_month, () -> moveMonth(1));
        monthTitle = stableText("", 20, true); monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setMinimumHeight(dp(48)); monthTitle.setFocusable(true);
        monthTitle.setContentDescription(getString(R.string.jump_date));
        monthTitle.setBackground(background(PAPER,0));
        monthTitle.setTextColor(interactiveText(PAPER,INK));
        monthTitle.setOnClickListener(v -> showDatePage(selected, chosen -> {selected=chosen;displayedMonth=Dates.month(chosen);render();}));
        nav.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        nav.addView(monthTitle, weight());
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        calendarPanel.addView(nav);
        monthGrid = column(); calendarPanel.addView(monthGrid);
        calendarDetails=stableText("",12,false);calendarDetails.setMinLines(2);calendarDetails.setMaxLines(2);
        calendarDetails.setEllipsize(TextUtils.TruncateAt.END); calendarPanel.addView(calendarDetails);
        milestonePanel=column();
        (wide ? identity : root).addView(milestonePanel);
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
        renderPinned();
        calendarDetails.setText(dateDetails(selected).isEmpty()?"\u00a0":dateDetails(selected));
        deskDetails.setText(dateDetails(today));
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
                    boolean inMonth = Dates.month(date).equals(displayedMonth);
                    LinearLayout cell = column(); cell.setGravity(Gravity.CENTER);
                    TextView number = stableText(day + (marks.contains(date) ? "·" : ""), inMonth ? 22 : 18, date.equals(today));
                    number.setGravity(Gravity.CENTER);
                    TextView detail = stableText(dayAnnotation(date), 11, true); detail.setGravity(Gravity.CENTER);
                    detail.setMaxLines(1);detail.setTag("almanac:"+date);
                    int base = date.equals(selected) ? INK : PAPER;
                    number.setTextColor(interactiveText(base,INK));detail.setTextColor(interactiveText(base,INK));
                    number.setDuplicateParentStateEnabled(true);detail.setDuplicateParentStateEnabled(true);
                    float systemScale = Math.max(1f, getResources().getConfiguration().fontScale);
                    cell.addView(number,new LinearLayout.LayoutParams(-1,-2));cell.addView(detail,new LinearLayout.LayoutParams(-1,dp(20 * textScale * systemScale)));
                    boolean valid = date.compareTo("1900-01-01") >= 0 && date.compareTo("2100-12-31") <= 0;
                    cell.setBackground(background(base, date.equals(today) ? 2 : 0));
                    cell.setSelected(date.equals(selected));
                    cell.setContentDescription(date + ", " + dateDetails(date) + (date.equals(today) ? ", " + getString(R.string.today) : "") + (date.equals(selected) ? ", " + getString(R.string.selected) : "") + (marks.contains(date) ? ", " + getString(R.string.has_events) : ""));
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
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp((textScale > 1 ? 66 : 56) * systemScale), 1);
                    cp.setMargins(dp(1), dp(1), dp(1), dp(1)); week.addView(cell, cp);
                }
                monthGrid.addView(week);
                monthGrid.addView(rule(RULE), new LinearLayout.LayoutParams(-1, dp(1)));
            }
            boolean desk=preferences.getBoolean("desk",false);
            boolean upcoming = desk || selected.equals(today);
            LinearLayout agendaHeader = row();
            TextView title = stableText(upcoming ? getString(R.string.upcoming).toUpperCase(displayLocale) : shortDate(selected), 14, true);
            String alternateHeading = upcoming ? alternateString(R.string.upcoming).toUpperCase(alternateLocale())
                : formatDate(selected, alternateLocale().equals(Locale.US) ? "MMM d" : "M月d日", alternateLocale());
            ((StableTextView) title).reserveFor(alternateHeading, fontFor(alternateHeading, labelFont));
            title.setTextColor(ACCENT);
            if (Build.VERSION.SDK_INT >= 21) title.setLetterSpacing(0.09f);
            agendaHeader.addView(title, weight());
            agendaHeader.addView(icon(LineIcon.LIST, R.string.future_events, this::showFuture));
            agendaHeader.addView(icon(LineIcon.ADD, R.string.add_event, () -> showEditor(0, selected, "", "")));
            agenda.addView(agendaHeader);
            String start=desk?today:selected;
            List<Event> events = visibleEvents(start, upcoming ? boundedEnd(start, 6) : start, desk || preferences.getBoolean("hide_done",false));
            if (events.isEmpty()) {
                TextView empty = label(upcoming ? R.string.empty_upcoming : R.string.empty_day, 16, false);
                empty.setLineSpacing(dp(6), 1); agenda.addView(empty);
            }
            int shown=0;
            for (Event event : events) {
                if(desk && shown++>=3)break;
                addEventRow(agenda,event,upcoming,this::render);
            }
        } catch (RuntimeException e) {
            agenda.removeAllViews(); agenda.addView(text(getString(R.string.read_failed), 16, false));
        }
    }

    private void showEditor(long id, String date, String title, String time) {
        editingId = id; editingDate = date;
        Event existing=id==0?null:store.get(id);editingCompleted=existing!=null && existing.completed;
        editor = new Dialog(this); editor.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = pageForm(id == 0 ? R.string.add_event : R.string.edit_event);
        if (id != 0) ((LinearLayout) form.getChildAt(0)).addView(icon(LineIcon.DELETE, R.string.delete, this::confirmDelete));
        fieldLabel(form, R.string.date);
        Button dateButton = button(date, () -> {});
        dateButton.setContentDescription(getString(R.string.choose_date));
        dateButton.setOnClickListener(v -> { hideKeyboard(titleInput); showDatePage(editingDate, chosen -> {editingDate=chosen;dateButton.setText(StableTextView.uniformLines(chosen,dateButton));}); });
        form.addView(dateButton);
        fieldLabel(form, R.string.event_title);
        titleInput = input(title, R.string.event_hint);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setTypeface(serif); titleInput.setMinLines(3); titleInput.setGravity(Gravity.TOP | Gravity.START);
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
            try { event = new Event(editingId, editingDate, titleInput.getText().toString(), timeInput.getText().toString(),editingCompleted); }
            catch (IllegalArgumentException e) { showError(error, validationMessage(e)); return; }
            try { store.save(event); }
            catch (RuntimeException e) { showError(error, getString(R.string.save_failed)); return; }
            selected = event.date; displayedMonth = Dates.month(selected); editor.dismiss(); render(); refreshOpenLists();
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
            try { store.delete(editingId); deleteDialog.dismiss(); editor.dismiss(); render(); refreshOpenLists(); }
            catch (RuntimeException e) { showError(error, getString(R.string.delete_failed)); }
        }));
        showPage(deleteDialog, form);
    }

    private void showDatePage(String initialDate, DateSelected onChosen) {
        dateDialog = new Dialog(this); dateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form = pageForm(R.string.choose_date);
        fieldLabel(form, R.string.date_format);
        EditText value = input(initialDate, 0); value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        value.setTypeface(display); value.setContentDescription(getString(R.string.date_format)); form.addView(value);
        final String[] month = { Dates.month(initialDate) };
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
            onChosen.accept(chosen); hideKeyboard(value); dateDialog.dismiss();
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
        CheckBox awake = check(R.string.keep_awake, preferences.getBoolean("awake", true));
        CheckBox full = check(R.string.full_screen, preferences.getBoolean("full", true));
        CheckBox large = check(R.string.large_text, preferences.getBoolean("large", false));
        form.addView(awake); form.addView(full); form.addView(large);
        CheckBox lunar=check(R.string.show_lunar,preferences.getBoolean("lunar",true));
        CheckBox terms=check(R.string.show_terms,preferences.getBoolean("terms",true));
        CheckBox holidays=check(R.string.show_holidays,preferences.getBoolean("holidays",true));
        CheckBox hideDone=check(R.string.hide_completed,preferences.getBoolean("hide_done",false));
        CheckBox desk=check(R.string.display_mode,preferences.getBoolean("desk",false));
        form.addView(lunar);form.addView(terms);form.addView(holidays);form.addView(hideDone);form.addView(desk);
        TextView deskNote=label(R.string.display_note,13,false);deskNote.setPadding(0,dp(8),0,dp(8));form.addView(deskNote);
        TextView holidayNote=label(R.string.holiday_note,13,false);form.addView(holidayNote);
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
                    .putBoolean("lunar",lunar.isChecked()).putBoolean("terms",terms.isChecked()).putBoolean("holidays",holidays.isChecked())
                    .putBoolean("hide_done",hideDone.isChecked()).putBoolean("desk",desk.isChecked())
                    .putString("language", languages.getCheckedRadioButtonId() == english.getId() ? "en" : "zh").apply();
                settingsDialog.dismiss();
                applyLanguage(); buildScreen(); applyDisplay();
            }), saveBounds);
        form.addView(actions);
        showPage(settingsDialog,form);
    }
    private Runnable refreshFuture, refreshMilestones;
    private boolean english() { return displayLocale.equals(Locale.US); }
    private String boundedEnd(String start,int days) {
        String end=Dates.addDays(start,days);return end.compareTo("2100-12-31")>0?"2100-12-31":end;
    }
    private String dayAnnotation(String date) {
        boolean holidays=preferences.getBoolean("holidays",true);
        if(holidays && !Holidays.label(date,english()).isEmpty()) return Holidays.label(date,english());
        if(!english() && preferences.getBoolean("terms",true) && !almanac.term(date).isEmpty())return almanac.term(date);
        if(holidays) {
            String name=almanac.festival(date,english());
            if(!name.isEmpty()) {
                if(!english())return name;
                String[] full={"New Year","Labour Day","National Day","Qingming","Spring Festival","Lantern Festival","Dragon Boat Festival","Mid-Autumn Festival","Double Ninth","Spring Festival Eve"};
                String[] shortNames={"New Yr","May Day","Natl","Qingm","Spring","Lant.","Boat","Moon","Ninth","Eve"};
                for(int i=0;i<full.length;i++)if(full[i].equals(name))return shortNames[i];
            }
        }
        Almanac.LunarDate lunar=almanac.lunar(date);
        return !english() && preferences.getBoolean("lunar",true) && lunar!=null?lunar.shortName():"";
    }
    private String dateDetails(String date) {
        List<String> details=new ArrayList<>();
        Almanac.LunarDate lunar=almanac.lunar(date);
        if(!english() && preferences.getBoolean("lunar",true) && lunar!=null)details.add(lunar.fullName());
        if(!english() && preferences.getBoolean("terms",true) && !almanac.term(date).isEmpty())details.add(almanac.term(date));
        if(preferences.getBoolean("holidays",true)) {
            String festival=almanac.festival(date,english()),status=Holidays.label(date,english());
            if(!festival.isEmpty())details.add(festival);
            if(!status.isEmpty())details.add(status);
            if(!Holidays.supported(date))details.add(getString(R.string.holiday_unknown));
        }
        return TextUtils.join(" · ",details);
    }
    private List<Event> visibleEvents(String start,String end,boolean hideCompleted) {
        List<Event> result=new ArrayList<>();
        for(Event event:store.between(start,end))if(!hideCompleted || !event.completed)result.add(event);
        return result;
    }
    private void addEventRow(LinearLayout parent,Event event,boolean dateShown,Runnable changed) {
        LinearLayout item=row();
        CheckBox done=check("",event.completed);
        done.setContentDescription(getString(event.completed?R.string.mark_incomplete:R.string.mark_complete)+": "+event.title);
        done.setOnCheckedChangeListener((button,checked) -> {
            try {store.complete(event.id,checked);changed.run();}
            catch(RuntimeException e) {done.setOnCheckedChangeListener(null);done.setChecked(event.completed);toast(getString(R.string.save_failed));}
        });
        item.addView(done,new LinearLayout.LayoutParams(dp(48),dp(48)));
        String when=(dateShown?(event.date.equals(today)?getString(R.string.today):shortDate(event.date))+"  ":"")+(event.time.isEmpty()?getString(R.string.all_day):event.time);
        LinearLayout body=column();body.setPadding(dp(4),dp(10),dp(4),dp(10));
        TextView meta=text(when+(event.completed?" · "+getString(R.string.completed):""),13,false);
        TextView content=text(event.title,20,false);content.setTypeface(fontFor(event.title,reading));content.setLineSpacing(0,1.2f);
        if(event.completed)content.setPaintFlags(content.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
        meta.setTextColor(interactiveText(PAPER,INK));content.setTextColor(interactiveText(PAPER,INK));
        meta.setDuplicateParentStateEnabled(true);content.setDuplicateParentStateEnabled(true);
        body.addView(meta);body.addView(content);body.setBackground(background(PAPER,0));body.setMinimumHeight(dp(64));body.setFocusable(true);
        body.setContentDescription(when+", "+event.title+", "+getString(R.string.edit_event));
        body.setOnClickListener(v -> showEditor(event.id,event.date,event.title,event.time));
        item.addView(body,weight());parent.addView(item);parent.addView(rule(INK),new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void refreshOpenLists() {
        if(futureDialog!=null && futureDialog.isShowing() && refreshFuture!=null)refreshFuture.run();
        if(milestonesDialog!=null && milestonesDialog.isShowing() && refreshMilestones!=null)refreshMilestones.run();
    }
    private void showFuture() {
        futureDialog=new Dialog(this);futureDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form=pageForm(R.string.future_events),list=column();
        RadioGroup ranges=new RadioGroup(this);ranges.setOrientation(LinearLayout.HORIZONTAL);
        for(int days:new int[]{7,30}) {
            RadioButton choice=new RadioButton(this);choice.setId(View.generateViewId());choice.setTag(days);choice.setText(getString(days==7?R.string.next_7:R.string.next_30));styleChoice(choice,true);
            ranges.addView(choice,new RadioGroup.LayoutParams(0,-2,1));
        }
        ranges.check(ranges.findViewWithTag(preferences.getInt("future_days",7)).getId());form.addView(ranges);
        CheckBox hide=check(R.string.hide_completed,preferences.getBoolean("hide_done",false));form.addView(hide);
        TextView range=stableText("",13,false);form.addView(range);form.addView(list);
        refreshFuture=() -> {
            list.removeAllViews();String end=boundedEnd(today,((Integer)ranges.findViewById(ranges.getCheckedRadioButtonId()).getTag())-1);
            range.setText(getString(R.string.date_range,today,end));
            List<Event> events=visibleEvents(today,end,hide.isChecked());
            if(events.isEmpty())list.addView(label(R.string.empty_range,18,false));
            for(Event event:events)addEventRow(list,event,true,() -> {render();refreshOpenLists();});
        };
        ranges.setOnCheckedChangeListener((group,id) -> {preferences.edit().putInt("future_days",(Integer)ranges.findViewById(id).getTag()).apply();refreshFuture.run();});
        hide.setOnCheckedChangeListener((view,checked) -> {preferences.edit().putBoolean("hide_done",checked).apply();render();refreshFuture.run();});
        form.addView(button(getString(R.string.close),() -> futureDialog.dismiss()));refreshFuture.run();showPage(futureDialog,form);
    }
    private String dayCount(int resource,long days) { return getResources().getQuantityString(resource,(int)days,days); }
    private String milestoneSummary(Milestone item) {
        String nextDate=item.nextDate(today,almanac);
        if(nextDate==null)return getString(R.string.outside_range);
        long days=Milestone.distance(today,nextDate);
        String remaining=days==0?getString(R.string.today):dayCount(days>0?R.plurals.days_until:R.plurals.days_since,Math.abs(days));
        if(item.kind==Milestone.ANNIVERSARY) {
            int years=item.yearsAt(nextDate,almanac);
            if(years>0)remaining=getString(R.string.anniversary_years,years,remaining);
            if(item.date.compareTo(today)<=0) remaining+="\n"+dayCount(R.plurals.days_since,Milestone.distance(item.date,today));
        }
        return remaining;
    }
    private void addMilestoneRow(LinearLayout parent,Milestone item,boolean compact) {
        LinearLayout card=column();card.setPadding(dp(4),dp(10),dp(4),dp(10));
        TextView title=text(item.title,compact?18:22,true),detail=text(milestoneSummary(item),16,false);
        title.setTextColor(interactiveText(PAPER,INK));detail.setTextColor(interactiveText(PAPER,INK));
        title.setDuplicateParentStateEnabled(true);detail.setDuplicateParentStateEnabled(true);
        card.addView(title);card.addView(detail);
        if(!compact) {
            String anchor=getString(R.string.anchor_date,item.date);
            if(item.lunar && !english())anchor+=" · "+almanac.lunar(item.date).fullName();
            TextView date=text(anchor,13,false);date.setTextColor(interactiveText(PAPER,INK));date.setDuplicateParentStateEnabled(true);card.addView(date);
        }
        card.setBackground(background(PAPER,0));card.setFocusable(true);card.setMinimumHeight(dp(48));
        card.setContentDescription(item.title+", "+milestoneSummary(item)+", "+getString(R.string.edit_milestone));
        card.setOnClickListener(v -> showMilestoneEditor(item));parent.addView(card);
        parent.addView(rule(INK),new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void renderPinned() {
        if(milestonePanel==null)return;
        milestonePanel.removeAllViews();
        LinearLayout header=row();header.addView(label(R.string.milestones,14,true),weight());
        header.addView(icon(LineIcon.MILESTONE,R.string.milestones,this::showMilestones));milestonePanel.addView(header);
        for(Milestone item:store.milestones())if(item.pinned)addMilestoneRow(milestonePanel,item,true);
    }
    private void showMilestones() {
        milestonesDialog=new Dialog(this);milestonesDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form=pageForm(R.string.milestones),list=column();
        ((LinearLayout)form.getChildAt(0)).addView(icon(LineIcon.ADD,R.string.add_milestone,() -> showMilestoneEditor(null)));
        form.addView(list);
        refreshMilestones=() -> {
            list.removeAllViews();List<Milestone> items=store.milestones();
            if(items.isEmpty())list.addView(label(R.string.empty_milestones,18,false));
            for(Milestone item:items)addMilestoneRow(list,item,false);
        };
        form.addView(button(getString(R.string.close),() -> milestonesDialog.dismiss()));refreshMilestones.run();showPage(milestonesDialog,form);
    }
    private int pinnedCount() {int count=0;for(Milestone item:store.milestones())if(item.pinned)count++;return count;}
    private int selectedMilestoneKind() { return (Integer)milestoneKinds.findViewById(milestoneKinds.getCheckedRadioButtonId()).getTag(); }
    private void showMilestoneEditor(Milestone existing) {
        boolean isNew=existing==null || existing.id==0;
        milestoneDraft=existing==null?new Milestone(0,today,"draft",Milestone.ANNIVERSARY,false,pinnedCount()<2):existing;
        milestoneDate=milestoneDraft.date;
        milestoneEditor=new Dialog(this);milestoneEditor.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form=pageForm(isNew?R.string.add_milestone:R.string.edit_milestone);
        if(!isNew)((LinearLayout)form.getChildAt(0)).addView(icon(LineIcon.DELETE,R.string.delete,() -> {
            Dialog confirm=new Dialog(this);confirm.requestWindowFeature(Window.FEATURE_NO_TITLE);deleteDialog=confirm;
            LinearLayout page=pageForm(R.string.delete_milestone);page.addView(text(milestoneTitle.getText().toString(),20,false));TextView error=errorLabel(page);
            page.addView(actions(getString(R.string.cancel),confirm::dismiss,getString(R.string.delete),() -> {
                try {store.deleteMilestone(milestoneDraft.id);confirm.dismiss();milestoneEditor.dismiss();render();refreshOpenLists();}
                catch(RuntimeException e) {showError(error,getString(R.string.delete_failed));}
            }));showPage(confirm,page);
        }));
        fieldLabel(form,R.string.milestone_name);milestoneTitle=input(existing==null?"":existing.title,R.string.event_hint);
        milestoneTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});milestoneTitle.setSingleLine(true);
        milestoneTitle.setContentDescription(getString(R.string.milestone_name));form.addView(milestoneTitle);
        fieldLabel(form,R.string.milestone_type);milestoneKinds=new RadioGroup(this);
        for(int kind:new int[]{Milestone.ANNIVERSARY,Milestone.COUNTDOWN}) {
            RadioButton radio=new RadioButton(this);radio.setId(View.generateViewId());radio.setTag(kind);radio.setText(getString(kind==0?R.string.anniversary:R.string.countdown));styleChoice(radio,true);milestoneKinds.addView(radio);
        }
        milestoneKinds.check(milestoneKinds.findViewWithTag(milestoneDraft.kind).getId());form.addView(milestoneKinds);
        fieldLabel(form,R.string.date);Button date=button(milestoneDate,() -> {});date.setContentDescription(getString(R.string.choose_date));
        date.setOnClickListener(v -> {hideKeyboard(milestoneTitle);showDatePage(milestoneDate,chosen -> {milestoneDate=chosen;date.setText(StableTextView.uniformLines(chosen,date));});});form.addView(date);
        milestoneLunar=check(R.string.lunar_anniversary,milestoneDraft.lunar);
        // Existing lunar anniversaries remain editable in English; new ones are selected in Chinese.
        milestoneLunar.setVisibility(!english() || milestoneDraft.lunar?View.VISIBLE:View.GONE);
        milestoneLunar.setEnabled(milestoneDraft.kind==Milestone.ANNIVERSARY);
        milestoneKinds.setOnCheckedChangeListener((group,id) -> {milestoneLunar.setEnabled(selectedMilestoneKind()==Milestone.ANNIVERSARY);if(selectedMilestoneKind()==Milestone.COUNTDOWN)milestoneLunar.setChecked(false);});form.addView(milestoneLunar);
        milestonePinned=check(R.string.pin_home,milestoneDraft.pinned);form.addView(milestonePinned);
        TextView note=label(R.string.milestone_note,13,false);note.setPadding(0,dp(12),0,0);form.addView(note);
        TextView error=errorLabel(form);
        form.addView(actions(getString(R.string.cancel),() -> milestoneEditor.dismiss(),getString(R.string.save),() -> {
            try {
                Milestone item=new Milestone(milestoneDraft.id,milestoneDate,milestoneTitle.getText().toString(),selectedMilestoneKind(),milestoneLunar.isChecked(),milestonePinned.isChecked());
                store.saveMilestone(item);hideKeyboard(milestoneTitle);milestoneEditor.dismiss();render();refreshOpenLists();
            }catch(IllegalArgumentException e){showError(error,validationMessage(e));}
            catch(RuntimeException e){showError(error,getString(R.string.save_failed));}
        }));
        milestoneEditor.setOnDismissListener(d -> {hideKeyboard(milestoneTitle);if(dateDialog!=null)dateDialog.dismiss();if(deleteDialog!=null)deleteDialog.dismiss();});
        showPage(milestoneEditor,form);
    }

    private CheckBox check(int title, boolean checked) {
        StableCheckBox box=new StableCheckBox(this);box.setText(getString(title));styleChoice(box,false);box.setChecked(checked);
        String alternate=alternateString(title);box.reserveFor(alternate,fontFor(alternate,display));return box;
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
        if ("pin_limit".equals(key)) return getString(R.string.pin_limit);
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
