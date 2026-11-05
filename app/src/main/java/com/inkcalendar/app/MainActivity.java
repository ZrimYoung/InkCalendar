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
    private Typeface serif, display, headline, labelFont, reading;
    private ThemeConfig theme;
    private ThemeFonts fonts;
    private TextView firstDigit,secondDigit;
    private Dialog appearanceDialog,componentDialog;
    private ThemeConfig settingsThemeDraft,appearanceThemeDraft,componentThemeDraft;
    private Bundle restoredSettings;
    private Runnable settingsThemeChanged,appearanceChanged;
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
        fonts=new ThemeFonts(getAssets());loadTheme();
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
        if(state!=null&&state.getBoolean("settingsOpen")){
            restoredSettings=state;showSettings();
            if(state.getBoolean("appearanceOpen"))showAppearance(settingsThemeDraft,settingsThemeChanged);
            if(state.getBoolean("componentsOpen"))showComponents(appearanceThemeDraft,appearanceChanged);
            restoredSettings=null;
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
        if (appearanceDialog != null) appearanceDialog.dismiss();
        if (componentDialog != null) componentDialog.dismiss();
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
        if(settingsDialog!=null&&settingsDialog.isShowing()){
            out.putBoolean("settingsOpen",true);settingsThemeDraft.bundle(out,"settingsTheme.");
            View decor=settingsDialog.getWindow().getDecorView();
            for(String key:new String[]{"awake","full","large","lunar","terms","holidays","hide_done","desk"})out.putBoolean("settings."+key,((CheckBox)decor.findViewWithTag("setting:"+key)).isChecked());
            RadioGroup languages=decor.findViewWithTag("setting:language");out.putString("settings.language",(String)languages.findViewById(languages.getCheckedRadioButtonId()).getTag());
            if(appearanceDialog!=null&&appearanceDialog.isShowing()){out.putBoolean("appearanceOpen",true);appearanceThemeDraft.bundle(out,"appearanceTheme.");}
            if(componentDialog!=null&&componentDialog.isShowing()){out.putBoolean("componentsOpen",true);componentThemeDraft.bundle(out,"componentTheme.");}
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

    private void loadTheme(){theme=ThemeConfig.read(preferences);serif=fonts.chinese(theme.font);display=fonts.latin(theme.font,false);headline=fonts.latin(theme.font,true);labelFont=fonts.label(theme.font);reading=fonts.reading(theme.font);}
    private String heading(String value){return theme.is("caps","on")?value.toUpperCase(displayLocale):value;}
    private void headingStyle(TextView view){float spacing=theme.is("tracking","spaced")?.09f:theme.is("tracking","tight")?-.035f:0;if(Build.VERSION.SDK_INT>=21)view.setLetterSpacing(spacing);else if(view instanceof StableTextView)((StableTextView)view).tracking(spacing);}

    private void buildScreen() {
        loadTheme();textScale=preferences.getBoolean("large",false)?1.18f:1f;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);scroll.setBackgroundColor(PAPER);
        root=column();root.setPadding(dp(18),dp(12),dp(18),dp(18));scroll.addView(root);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=35){scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});scroll.requestApplyInsets();}
        LinearLayout top=row();StableTextView brand=label(R.string.app_name,theme.is("date","masthead")?28:26,true);
        brand.setTypeface(headline);brand.setText(heading(getString(R.string.app_name)));brand.reserveFor(heading(alternateString(R.string.app_name)),headline);headingStyle(brand);top.addView(brand,weight());
        top.addView(icon(LineIcon.TODAY,R.string.go_today,()->{
            if(selected.equals(Dates.today())&&displayedMonth.equals(Dates.month(Dates.today()))){Toast.makeText(this,R.string.already_today,Toast.LENGTH_SHORT).show();return;}
            selected=Dates.today();today=selected;displayedMonth=Dates.month(selected);render();
        }));top.addView(icon(LineIcon.SETTINGS,R.string.settings,this::showSettings));root.addView(top);
        addDivider(root,theme.ruleWidth(),12);
        boolean desk=preferences.getBoolean("desk",false);
        boolean wide=!desk&&getResources().getConfiguration().screenWidthDp>=640&&getResources().getConfiguration().orientation==2;
        LinearLayout identity=buildIdentity(wide,desk);
        deskDetails=stableText("",16,false);if(desk)identity.addView(deskDetails);
        LinearLayout calendarPanel=new MonthSwipeLayout(this,this::moveMonth);calendarPanel.setVisibility(desk?View.GONE:View.VISIBLE);
        LinearLayout nav=row();previous=icon(LineIcon.PREVIOUS,R.string.previous_month,()->moveMonth(-1));next=icon(LineIcon.NEXT,R.string.next_month,()->moveMonth(1));
        monthTitle=stableText("",20,true);monthTitle.setGravity(Gravity.CENTER);monthTitle.setMinimumHeight(dp(48));monthTitle.setFocusable(true);monthTitle.setContentDescription(getString(R.string.jump_date));
        monthTitle.setBackground(background(PAPER,0));monthTitle.setTextColor(interactiveText(PAPER,INK));
        monthTitle.setOnClickListener(v->showDatePage(selected,chosen->{selected=chosen;displayedMonth=Dates.month(chosen);render();}));
        nav.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));nav.addView(monthTitle,weight());nav.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));calendarPanel.addView(nav);
        monthGrid=column();calendarPanel.addView(monthGrid);calendarDetails=stableText("",12,false);calendarDetails.setMinLines(2);calendarDetails.setMaxLines(2);calendarDetails.setEllipsize(TextUtils.TruncateAt.END);calendarPanel.addView(calendarDetails);
        milestonePanel=column();agenda=column();agenda.setPadding(0,dp(10),0,0);
        LinearLayout agendaArea=column();addDivider(agendaArea,theme.ruleWidth(),12);agendaArea.addView(agenda);
        if(wide&&!theme.is("flow","agenda-first")){
            LinearLayout body=row();body.setGravity(Gravity.TOP);LinearLayout left=column();left.setPadding(0,0,dp(12),0);left.addView(identity);left.addView(milestonePanel);left.addView(agendaArea);
            body.addView(left,new LinearLayout.LayoutParams(0,-2,.36f));calendarPanel.setPadding(dp(12),0,0,0);body.addView(calendarPanel,new LinearLayout.LayoutParams(0,-2,.64f));root.addView(body);
        }else if(wide){
            LinearLayout upper=row();upper.setGravity(Gravity.TOP);LinearLayout left=column();left.addView(identity);left.addView(milestonePanel);upper.addView(left,new LinearLayout.LayoutParams(0,-2,.4f));
            LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,.6f);right.leftMargin=dp(18);upper.addView(agendaArea,right);root.addView(upper);root.addView(calendarPanel);
        }else{
            root.addView(identity);if(theme.is("flow","agenda-first")){root.addView(milestonePanel);root.addView(agendaArea);root.addView(calendarPanel);}else{root.addView(calendarPanel);root.addView(milestonePanel);root.addView(agendaArea);}
        }
        render();
    }
    private void addDivider(LinearLayout parent,int width,int margin){LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,dp(width));bounds.setMargins(0,dp(margin),0,dp(margin));parent.addView(rule(INK),bounds);}
    private FitDateView bigDate(int size){FitDateView view=new FitDateView(this);view.setTypeface(headline);view.setTextColor(INK);view.maximum(size*textScale);return view;}
    private LinearLayout buildIdentity(boolean wide,boolean desk){
        LinearLayout identity=column();identity.setPadding(0,dp(8),0,dp(16));
        dayNumber=bigDate(desk?120:theme.is("date","masthead")?112:theme.is("date","center")?112:84);
        dayNumber.setText(String.format(Locale.US,"%02d",Integer.parseInt(today.substring(8))));
        dateTitle=stableText("",16,false);dayTitle=stableText("",21,false);headingStyle(dayTitle);
        LinearLayout words=column();words.addView(dateTitle);words.addView(dayTitle);firstDigit=null;secondDigit=null;
        if(theme.is("date","tiles")){
            LinearLayout tiles=row();firstDigit=bigDate(82);secondDigit=bigDate(82);
            for(TextView digit:new TextView[]{firstDigit,secondDigit}){digit.setGravity(Gravity.CENTER);digit.setBackground(shape(PAPER,2));LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(0,dp(116*textScale),1);bounds.setMargins(dp(3),0,dp(3),0);tiles.addView(digit,bounds);}
            identity.addView(tiles);words.setPadding(0,dp(10),0,0);identity.addView(words);
        }else if(theme.is("date","center")){
            dayNumber.setGravity(Gravity.CENTER);dateTitle.setGravity(Gravity.CENTER);dayTitle.setGravity(Gravity.CENTER);identity.addView(dayNumber,new LinearLayout.LayoutParams(-1,-2));identity.addView(words);
        }else if(wide&&!theme.is("date","reverse")){
            words.removeView(dateTitle);identity.addView(dateTitle);identity.addView(dayNumber,new LinearLayout.LayoutParams(-1,-2));identity.addView(words);
        }else{
            LinearLayout hero=row();LinearLayout.LayoutParams digits=new LinearLayout.LayoutParams(0,-2,theme.is("date","masthead")?.54f:.46f),copy=new LinearLayout.LayoutParams(0,-2,theme.is("date","masthead")?.46f:.54f);
            if(theme.is("date","reverse")){dayNumber.setGravity(Gravity.END);hero.addView(words,copy);hero.addView(dayNumber,digits);}else{copy.leftMargin=dp(12);hero.addView(dayNumber,digits);if(theme.is("date","masthead"))words.setBackground(bottomRule());hero.addView(words,copy);}identity.addView(hero);
        }
        return identity;
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
        if(firstDigit!=null){String value=String.format(Locale.US,"%02d",Integer.parseInt(today.substring(8)));firstDigit.setText(value.substring(0,1));secondDigit.setText(value.substring(1));}
        dateTitle.setText(monthLabel(today));
        ((StableTextView) dateTitle).reserveFor(alternateMonth(today), fontFor(alternateMonth(today), display));
        dayTitle.setText(heading(formatDate(today, "EEEE")));
        String alternateDay = heading(formatDate(today, "EEEE", alternateLocale()));
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
                    float systemScale = Math.max(1f, getResources().getConfiguration().fontScale);
                    CalendarDayView cell=new CalendarDayView(this,theme.component("calendar"),day,dayAnnotation(date),marks.contains(date),date.equals(today),date.equals(selected),false,textScale,display,labelFont,serif);
                    if(!inMonth)cell.number.setTextSize(18*textScale);
                    cell.detail.setTag("almanac:"+date);cell.marker.setTag("marker:"+date);
                    boolean valid = date.compareTo("1900-01-01") >= 0 && date.compareTo("2100-12-31") <= 0;
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
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(70*textScale*systemScale), 1);
                    cp.setMargins(dp(1), dp(1), dp(1), dp(1)); week.addView(cell, cp);
                }
                monthGrid.addView(week);
                if(theme.is("calendar","ruled"))monthGrid.addView(rule(RULE),new LinearLayout.LayoutParams(-1,dp(1)));
            }
            boolean desk=preferences.getBoolean("desk",false);
            boolean upcoming = desk || selected.equals(today);
            LinearLayout agendaHeader = row();
            TextView title = stableText(upcoming ? getString(R.string.upcoming).toUpperCase(displayLocale) : shortDate(selected), 14, true);
            String alternateHeading = upcoming ? alternateString(R.string.upcoming).toUpperCase(alternateLocale())
                : formatDate(selected, alternateLocale().equals(Locale.US) ? "MMM d" : "M月d日", alternateLocale());
            ((StableTextView) title).reserveFor(alternateHeading, fontFor(alternateHeading, labelFont));
            title.setTextColor(ACCENT);
            title.setText(heading(title.getText().toString()));headingStyle(title);
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
        titleInput.setTypeface(reading); titleInput.setMinLines(3); titleInput.setGravity(Gravity.TOP | Gravity.START);
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
                    CalendarDayView cell=new CalendarDayView(this,theme.component("calendar"),String.valueOf(Integer.parseInt(key.substring(8))),"",false,key.equals(today),chosen,true,textScale,display,labelFont,serif);
                    cell.setContentDescription(key + (chosen ? ", " + getString(R.string.selected) : "")); cell.setSelected(chosen);
                    boolean valid = key.compareTo("1900-01-01") >= 0 && key.compareTo("2100-12-31") <= 0;
                    cell.setEnabled(valid); cell.setFocusable(valid);
                    if (valid) cell.setOnClickListener(v -> { hideKeyboard(value); value.setText(key); });
                    week.addView(cell, new LinearLayout.LayoutParams(0, dp(54*textScale*Math.max(1f,getResources().getConfiguration().fontScale)), 1));
                }
                calendar.addView(week);if(theme.is("calendar","ruled"))calendar.addView(rule(INK),new LinearLayout.LayoutParams(-1,dp(1)));
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
        LinearLayout header = row();StableTextView caption=label(title,24,true);caption.setText(heading(caption.getText().toString()));headingStyle(caption);header.addView(caption,weight());form.addView(header);
        LinearLayout.LayoutParams divider = new LinearLayout.LayoutParams(-1, dp(theme.ruleWidth())); divider.setMargins(0, dp(12), 0, dp(8)); form.addView(rule(INK), divider);
        return form;
    }
    private void fieldLabel(LinearLayout form, int id) {
        TextView field = label(id, 15, false); field.setPadding(0, dp(16), 0, dp(8)); form.addView(field);
    }
    private EditText input(String value, int hint) {
        EditText field = new ThemeEditText(this,serif); field.setTextSize(20 * textScale); field.setTypeface(display);
        field.setTextColor(INK); field.setHintTextColor(INK); field.setIncludeFontPadding(false);
        if("minimal".equals(theme.style)||"editorial".equals(theme.style))field.setBackground(bottomRule());else field.setBackgroundResource(R.drawable.ink_input);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
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
        LinearLayout settingsHeader=row();settingsHeader.addView(label(R.string.settings,24,true),weight());form.addView(settingsHeader);
        ThemeConfig pendingTheme=restoredSettings==null?new ThemeConfig(theme):ThemeConfig.fromBundle(restoredSettings,"settingsTheme.");settingsThemeDraft=pendingTheme;
        Button appearance=button(getString(R.string.appearance)+" · "+getString(pendingTheme.label()),()->{});
        appearance.setContentDescription(getString(R.string.appearance));
        settingsThemeChanged=()->appearance.setText(ThemeFonts.mixed(getString(R.string.appearance)+" · "+getString(pendingTheme.label()),serif,false));
        appearance.setOnClickListener(v->showAppearance(pendingTheme,settingsThemeChanged));
        LinearLayout.LayoutParams divider = new LinearLayout.LayoutParams(-1, dp(4));
        divider.setMargins(0, dp(12), 0, dp(12)); form.addView(rule(INK), divider);
        LinearLayout.LayoutParams appearanceBounds=new LinearLayout.LayoutParams(-1,-2);appearanceBounds.bottomMargin=dp(12);form.addView(appearance,appearanceBounds);
        CheckBox awake = check(R.string.keep_awake, preferences.getBoolean("awake", true));
        CheckBox full = check(R.string.full_screen, preferences.getBoolean("full", true));
        CheckBox large = check(R.string.large_text, preferences.getBoolean("large", false));
        form.addView(awake); form.addView(full); form.addView(large);
        CheckBox lunar=check(R.string.show_lunar,preferences.getBoolean("lunar",true));
        CheckBox terms=check(R.string.show_terms,preferences.getBoolean("terms",true));
        CheckBox holidays=check(R.string.show_holidays,preferences.getBoolean("holidays",true));
        CheckBox hideDone=check(R.string.hide_completed,preferences.getBoolean("hide_done",false));
        CheckBox desk=check(R.string.display_mode,preferences.getBoolean("desk",false));
        String[] settingKeys={"awake","full","large","lunar","terms","holidays","hide_done","desk"};CheckBox[] settingViews={awake,full,large,lunar,terms,holidays,hideDone,desk};
        for(int i=0;i<settingKeys.length;i++){settingViews[i].setTag("setting:"+settingKeys[i]);if(restoredSettings!=null)settingViews[i].setChecked(restoredSettings.getBoolean("settings."+settingKeys[i],settingViews[i].isChecked()));}
        form.addView(lunar);form.addView(terms);form.addView(holidays);form.addView(hideDone);form.addView(desk);
        TextView deskNote=label(R.string.display_note,13,false);deskNote.setPadding(0,dp(8),0,dp(8));form.addView(deskNote);
        TextView holidayNote=label(R.string.holiday_note,13,false);form.addView(holidayNote);
        TextView languageTitle = label(R.string.language, 16, false);
        languageTitle.setPadding(0, dp(16), 0, dp(8)); form.addView(languageTitle);
        RadioGroup languages = new RadioGroup(this);
        RadioButton chinese = new RadioButton(this); chinese.setId(View.generateViewId()); chinese.setText("简体中文"); chinese.setMinHeight(dp(48));
        RadioButton english = new RadioButton(this); english.setId(View.generateViewId()); english.setText("English"); english.setMinHeight(dp(48));
        styleChoice(chinese, true); styleChoice(english, true);
        languages.setTag("setting:language");chinese.setTag("zh");english.setTag("en");languages.addView(chinese); languages.addView(english);
        languages.check((restoredSettings==null?displayLocale.equals(Locale.US):"en".equals(restoredSettings.getString("settings.language"))) ? english.getId() : chinese.getId());form.addView(languages);
        TextView note = label(R.string.settings_note, 14, false);
        note.setPadding(0, dp(12), 0, 0); note.setLineSpacing(dp(5), 1); form.addView(note);
        settingsHeader.addView(icon(LineIcon.CANCEL,R.string.cancel,()->settingsDialog.dismiss()));
        settingsHeader.addView(icon(LineIcon.SAVE,R.string.save,()->{
            SharedPreferences.Editor edit=preferences.edit().putBoolean("awake",awake.isChecked()).putBoolean("full",full.isChecked()).putBoolean("large",large.isChecked())
                .putBoolean("lunar",lunar.isChecked()).putBoolean("terms",terms.isChecked()).putBoolean("holidays",holidays.isChecked())
                .putBoolean("hide_done",hideDone.isChecked()).putBoolean("desk",desk.isChecked())
                .putString("language",languages.getCheckedRadioButtonId()==english.getId()?"en":"zh");
            pendingTheme.save(edit);edit.apply();settingsDialog.dismiss();applyLanguage();buildScreen();applyDisplay();
        }));
        showPage(settingsDialog,form);
    }
    private void settingActions(LinearLayout form,Runnable cancel,Runnable save){LinearLayout head=(LinearLayout)form.getChildAt(0);head.addView(icon(LineIcon.CANCEL,R.string.cancel,cancel));head.addView(icon(LineIcon.SAVE,R.string.save,save));}
    private void themeSection(LinearLayout form,int title){TextView label=label(title,17,true);label.setPadding(0,dp(20),0,dp(8));form.addView(label);}
    private void showAppearance(ThemeConfig destination,Runnable changed){
        ThemeConfig draft=restoredSettings==null?new ThemeConfig(destination):ThemeConfig.fromBundle(restoredSettings,"appearanceTheme.");appearanceThemeDraft=draft;
        appearanceDialog=new Dialog(this);appearanceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form=pageForm(R.string.appearance);
        settingActions(form,()->appearanceDialog.dismiss(),()->{destination.font=draft.font;destination.style=draft.style;destination.overrides.clear();destination.overrides.putAll(draft.overrides);changed.run();appearanceDialog.dismiss();});
        themeSection(form,R.string.theme_style);LinearLayout gallery=column();form.addView(gallery);
        final Runnable[] update={null};
        update[0]=()->{
            gallery.removeAllViews();
            for(int i=0;i<ThemeConfig.STYLES.length;i+=2){LinearLayout row=row();row.setGravity(Gravity.TOP);
                for(int j=i;j<Math.min(i+2,ThemeConfig.STYLES.length);j++){
                    final String id=ThemeConfig.STYLES[j];ThemeConfig sample=new ThemeConfig(draft);if(!draft.style.equals(id))sample.chooseStyle(id);
                    String title=getString(ThemeConfig.STYLE_LABELS[j])+(draft.style.equals(id)&&!draft.overrides.isEmpty()?" · "+getString(R.string.theme_custom):"");
                    row.addView(themeCard(sample,null,title,draft.style.equals(id),()->{draft.chooseStyle(id);update[0].run();}),cardBounds());
                }gallery.addView(row);
            }
        };update[0].run();appearanceChanged=update[0];
        themeSection(form,R.string.theme_font);form.addView(label(R.string.theme_font_note,13,false));
        RadioGroup choices=new RadioGroup(this);
        for(int i=0;i<ThemeConfig.FONTS.length;i++){
            final String id=ThemeConfig.FONTS[i];RadioButton option=new RadioButton(this);option.setId(View.generateViewId());option.setTag(id);
            option.setText(ThemeConfig.FONT_NAMES[i]+"\nInk Calendar · 墨日历 0123456789");styleChoice(option,true);
            option.setTypeface(fonts.latin(id,false));option.setTextSize(16*textScale);option.setText(ThemeFonts.mixed(option.getText(),fonts.chinese(id),false));option.setPadding(0,dp(10),0,dp(10));
            option.setContentDescription(ThemeConfig.FONT_NAMES[i]);choices.addView(option);
        }
        choices.check(choices.findViewWithTag(draft.font).getId());choices.setOnCheckedChangeListener((group,id)->draft.font=(String)group.findViewById(id).getTag());form.addView(choices);
        form.addView(button(getString(R.string.theme_components),()->showComponents(draft,update[0])));
        showPage(appearanceDialog,form);
    }
    private LinearLayout.LayoutParams cardBounds(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(4),dp(3),dp(4));return p;}
    private View themeCard(ThemeConfig value,String component,String title,boolean chosen,Runnable action){
        LinearLayout card=column();card.setPadding(dp(6),dp(6),dp(6),dp(6));StateListDrawable border=new StateListDrawable();border.addState(new int[]{android.R.attr.state_focused},shape(PAPER,3));border.addState(new int[]{android.R.attr.state_pressed},shape(PAPER,3));border.addState(new int[]{},shape(PAPER,chosen?2:1));card.setBackground(border);card.setFocusable(true);card.setSelected(chosen);
        ThemePreview preview=new ThemePreview(this,value,component);card.addView(preview,new LinearLayout.LayoutParams(-1,dp(86)));
        RadioButton caption=new RadioButton(this);caption.setText(title);styleChoice(caption,true);caption.setTextSize(14*textScale);caption.setChecked(chosen);caption.setClickable(false);caption.setFocusable(false);caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(caption);
        card.setContentDescription(title+(chosen?", "+getString(R.string.selected):""));card.setOnClickListener(v->action.run());return card;
    }
    private void showComponents(ThemeConfig destination,Runnable changed){
        ThemeConfig draft=restoredSettings==null?new ThemeConfig(destination):ThemeConfig.fromBundle(restoredSettings,"componentTheme.");componentThemeDraft=draft;componentDialog=new Dialog(this);componentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout form=pageForm(R.string.theme_components),sections=column();
        settingActions(form,()->componentDialog.dismiss(),()->{destination.overrides.clear();destination.overrides.putAll(draft.overrides);changed.run();componentDialog.dismiss();});
        final Runnable[] update={null};update[0]=()->{
            sections.removeAllViews();
            for(int i=0;i<ThemeConfig.KEYS.length;i++){
                final String key=ThemeConfig.KEYS[i];themeSection(sections,ThemeConfig.KEY_LABELS[i]);
                for(int j=0;j<ThemeConfig.OPTIONS[i].length;j+=2){LinearLayout row=row();row.setGravity(Gravity.TOP);
                    for(int k=j;k<Math.min(j+2,ThemeConfig.OPTIONS[i].length);k++){
                        final String value=ThemeConfig.OPTIONS[i][k];ThemeConfig sample=new ThemeConfig(draft);sample.choose(key,value);
                        row.addView(themeCard(sample,key,getString(ThemeConfig.OPTION_LABELS[i][k]),draft.is(key,value),()->{draft.choose(key,value);update[0].run();}),cardBounds());
                    }
                    if(row.getChildCount()==1)row.addView(new View(this),cardBounds());sections.addView(row);
                }
            }
        };
        form.addView(button(getString(R.string.theme_reset),()->{draft.overrides.clear();update[0].run();}));form.addView(sections);update[0].run();showPage(componentDialog,form);
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
        LinearLayout item=row();item.setGravity(Gravity.TOP);
        CheckBox done=check("",event.completed);done.setContentDescription(getString(event.completed?R.string.mark_incomplete:R.string.mark_complete)+": "+event.title);
        done.setOnCheckedChangeListener((button,checked)->{
            try{store.complete(event.id,checked);changed.run();}
            catch(RuntimeException e){done.setOnCheckedChangeListener(null);done.setChecked(event.completed);toast(getString(R.string.save_failed));}
        });item.addView(done,new LinearLayout.LayoutParams(dp(48),dp(48)));
        String when=(dateShown?(event.date.equals(today)?getString(R.string.today):shortDate(event.date))+"  ":"")+(event.time.isEmpty()?getString(R.string.all_day):event.time);
        boolean horizontal=theme.is("agenda","timeline")||theme.is("agenda","ledger");
        LinearLayout body=horizontal?row():column();body.setGravity(Gravity.TOP);body.setPadding(dp(8),dp(10),dp(8),dp(10));
        TextView meta=stableText(when+(event.completed?" · "+getString(R.string.completed):""),12,false);
        ((StableTextView)meta).reserveFor((dateShown?formatDate(event.date,alternateLocale().equals(Locale.US)?"MMM d":"M月d日",alternateLocale())+"  ":"")+(event.time.isEmpty()?alternateString(R.string.all_day):event.time),display);
        TextView content=text(event.title,20,false);content.setTypeface(reading);content.setLineSpacing(0,1.2f);
        if(event.completed)content.setPaintFlags(content.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
        meta.setTextColor(interactiveText(PAPER,INK));content.setTextColor(interactiveText(PAPER,INK));meta.setDuplicateParentStateEnabled(true);content.setDuplicateParentStateEnabled(true);
        if(horizontal){
            LinearLayout.LayoutParams time=new LinearLayout.LayoutParams(0,-2,.3f),title=new LinearLayout.LayoutParams(0,-2,.7f);
            if(theme.is("agenda","ledger")){meta.setGravity(Gravity.END);title.rightMargin=dp(8);body.addView(content,title);body.addView(meta,time);}
            else{title.leftMargin=dp(10);body.addView(meta,time);body.addView(content,title);}
        }else{body.addView(meta);body.addView(content);}
        body.setBackground(background(PAPER,theme.is("agenda","cards")?1:0));body.setMinimumHeight(dp(64));body.setFocusable(true);
        body.setContentDescription(when+", "+event.title+", "+getString(R.string.edit_event));body.setOnClickListener(v->showEditor(event.id,event.date,event.title,event.time));
        item.addView(body,weight());LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,-2);bounds.bottomMargin=dp(theme.is("agenda","cards")?8:0);parent.addView(item,bounds);
        if(theme.is("agenda","stacked")||theme.is("agenda","ledger")){View line=rule(INK);if(theme.is("agenda","ledger"))line.setBackground(new InkRule(getResources().getDisplayMetrics().density,true,false));parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
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
        box.useChinese(serif);
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
        choice.setText(StableTextView.uniformLines(ThemeFonts.mixed(choice.getText(),serif,false), choice));
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
        TextView view = new TextView(this); view.setText(ThemeFonts.mixed(value,serif,bold)); view.setTextColor(INK); view.setTextSize(size * textScale);
        Typeface face = fontFor(value.isEmpty() ? (displayLocale.equals(Locale.US) ? "A" : "中") : value, bold ? labelFont : display);
        view.setTypeface(face); return view;
    }
    private StableTextView stableText(String value, int size, boolean bold) {
        StableTextView view = new StableTextView(this);
        view.setTextSize(size * textScale); view.setTextColor(INK); view.setGravity(Gravity.CENTER_VERTICAL);
        Typeface face = fontFor(value.isEmpty() ? (displayLocale.equals(Locale.US) ? "A" : "中") : value, bold ? labelFont : display);
        view.setTypeface(face);view.useChinese(serif,bold);
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
        b.setIncludeFontPadding(false); b.setText(StableTextView.uniformLines(ThemeFonts.mixed(value,serif,false), b));
        b.setTextColor(interactiveText(PAPER, INK));
        b.setAllCaps(false); b.setMinimumWidth(dp(48)); b.setMinWidth(dp(48)); b.setMinimumHeight(dp(48)); b.setMinHeight(dp(48));
        b.setPadding(dp(10), dp(4), dp(10), dp(4)); b.setBackground(background(PAPER, 1));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private ImageButton icon(int kind, int label, Runnable action) {
        ImageButton button = new ImageButton(this);
        boolean solid=theme.is("buttons","solid");
        button.setImageDrawable(new LineIcon(kind, getResources().getDisplayMetrics().density,solid));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(dp(48), dp(48));
        bounds.leftMargin = dp(8); button.setLayoutParams(bounds);
        button.setBackground(background(solid?INK:PAPER,theme.is("buttons","bare")?0:1));
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
        // Chinese spans select the paired font per run, including mixed-language event titles.
        return latin;
    }
    private android.graphics.drawable.Drawable bottomRule(){return new InkRule(getResources().getDisplayMetrics().density,false,true);}
    private GradientDrawable shape(int color, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color);
        if (border > 0) d.setStroke(dp(border), INK); return d;
    }
    private View rule(int color) { View v = new View(this); v.setBackgroundColor(color); return v; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
