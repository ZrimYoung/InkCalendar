package com.inkcalendar.app;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** These fixtures are also executed against the Web domain implementation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={19,28})
public class PlatformParityTest {
    private static JSONObject fixtures,contract;
    private static Almanac almanac;
    private static JSONObject json(String name) throws Exception {
        return new JSONObject(new String(Files.readAllBytes(Paths.get("../shared/"+name)),StandardCharsets.UTF_8));
    }
    @BeforeClass public static void load() throws Exception {
        fixtures=json("parity-cases.json");contract=json("calendar-contract.json");
        almanac=new Almanac(new FileReader("src/main/assets/calendar/months.csv"),new FileReader("src/main/assets/calendar/terms.csv"));
    }
    @Test public void datesAndCalendarBoundsMatchSharedCases() throws Exception {
        JSONArray cases=fixtures.getJSONArray("dates");
        for(int i=0;i<cases.length();i++) {
            JSONObject c=cases.getJSONObject(i);String date=c.getString("date");
            assertEquals(date,Dates.key(Dates.parse(date)));
            assertEquals(c.getString("weekStart"),Dates.weekStart(date));
            String[] days=Dates.grid(date);assertEquals(42,days.length);
            assertEquals(c.getString("gridFirst"),days[0]);assertEquals(c.getString("gridLast"),days[41]);
        }
        JSONArray invalid=fixtures.getJSONArray("invalidDates");
        for(int i=0;i<invalid.length();i++) {
            String date=invalid.getString(i);
            try {Dates.parse(date);fail("Accepted invalid date: "+date);}catch(IllegalArgumentException expected) {}
        }
    }
    @Test public void landscapeCalendarFitMatchesSharedCases() throws Exception {
        JSONArray cases=fixtures.getJSONArray("calendarFits");
        for(int i=0;i<cases.length();i++){
            JSONObject c=cases.getJSONObject(i);
            assertEquals(c.getDouble("scale"),MonthFitLayout.scaleFor(c.getInt("natural"),c.getInt("available")),.0001);
        }
    }
    @Test public void eventValidationMatchesSharedCases() throws Exception {
        JSONArray cases=fixtures.getJSONArray("events");
        for(int i=0;i<cases.length();i++) {
            JSONObject c=cases.getJSONObject(i),input=c.getJSONObject("input");
            String title=input.optString("title","");
            if(c.has("repeat")) {JSONObject repeat=c.getJSONObject("repeat");StringBuilder text=new StringBuilder();for(int n=0;n<repeat.getInt("count");n++)text.append(repeat.getString("text"));title=text.toString();}
            try {
                Event event=new Event(1,input.getString("date"),title,input.isNull("time")?null:input.optString("time",""),input.optBoolean("completed",false));
                assertFalse("Expected validation failure at case "+i,c.has("error"));
                assertEquals(c.optString("title",title),event.title);assertEquals(c.getString("time"),event.time);assertEquals(c.getBoolean("completed"),event.completed);
            }catch(IllegalArgumentException error) {
                assertTrue("Unexpected validation failure at case "+i,c.has("error"));
                if(!"date_invalid".equals(c.getString("error")))assertEquals(c.getString("error"),error.getMessage());
            }
        }
    }
    @Test public void milestoneRecurrencesMatchSharedCases() throws Exception {
        JSONArray cases=fixtures.getJSONArray("milestones");
        for(int i=0;i<cases.length();i++) {
            JSONObject c=cases.getJSONObject(i);boolean countdown="countdown".equals(c.getString("kind"));
            Milestone m=new Milestone(1,c.getString("date"),"Test",countdown?Milestone.COUNTDOWN:Milestone.ANNIVERSARY,c.getBoolean("lunar"),false);
            String next=m.nextDate(c.getString("today"),almanac);
            assertEquals("case "+i,c.isNull("next")?null:c.getString("next"),next);
            if(c.has("years"))assertEquals(c.getInt("years"),m.yearsAt(next,almanac));
            if(c.has("distance"))assertEquals(c.getLong("distance"),Milestone.distance(c.getString("today"),next));
            if(countdown)assertFalse(m.lunar);
        }
    }
    @Test public void lunarLabelsMatchSharedCases() throws Exception {
        JSONArray cases=fixtures.getJSONArray("lunarDates");
        for(int i=0;i<cases.length();i++) {
            JSONObject c=cases.getJSONObject(i);Almanac.LunarDate lunar=almanac.lunar(c.getString("date"));assertNotNull(lunar);
            assertEquals(c.getInt("month"),lunar.month);assertEquals(c.getInt("day"),lunar.day);assertEquals(c.getString("short"),lunar.shortName());
        }
    }
    @Test public void nativeThemeRecipesAndDefaultsUseSharedCatalog() throws Exception {
        JSONArray themes=contract.getJSONArray("themes"),components=contract.getJSONArray("components"),fonts=contract.getJSONArray("fonts");
        assertEquals(themes.length(),ThemeConfig.STYLES.length);assertEquals(fonts.length(),ThemeConfig.FONTS.length);
        ThemeConfig theme=new ThemeConfig();theme.font="space";
        for(int i=0;i<themes.length();i++) {
            JSONObject preset=themes.getJSONObject(i);theme.chooseStyle(preset.getString("id"));
            assertEquals("space",theme.font);assertEquals(preset.getInt("ruleWidth"),theme.ruleWidth());
            for(int j=0;j<components.length();j++)assertEquals(preset.getJSONArray("recipe").getString(j),theme.component(components.getJSONObject(j).getString("key")));
        }
        for(int i=0;i<fonts.length();i++){JSONObject f=fonts.getJSONObject(i);theme.font=f.getString("id");assertEquals(f.getBoolean("sans"),theme.sans());}
        JSONObject d=contract.getJSONObject("defaults");DisplayOptions options=new DisplayOptions();
        assertEquals(d.getInt("scale"),options.scale);assertEquals(d.getString("view"),options.view);assertEquals(d.getString("layout"),options.layout);
        assertEquals(d.getString("orientation"),options.orientation);assertEquals(d.getString("language"),options.language);
        assertEquals(d.getString("colorMode"),options.colorMode);
        assertEquals(d.getBoolean("full"),options.full);assertEquals(d.getBoolean("awake"),options.awake);
        assertEquals(contract.getJSONObject("limits").getInt("scaleMin"),DisplayOptions.clamp(Integer.MIN_VALUE));
        assertEquals(contract.getJSONObject("limits").getInt("scaleMax"),DisplayOptions.clamp(Integer.MAX_VALUE));
    }
    @Test public void colorModeMigrationAndPalettesMatchSharedCases() throws Exception {
        android.content.SharedPreferences prefs=org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("color-mode-parity",0);
        JSONArray cases=fixtures.getJSONArray("colorModes");
        for(int i=0;i<cases.length();i++){
            JSONObject c=cases.getJSONObject(i);prefs.edit().clear().commit();
            if(!c.isNull("value"))prefs.edit().putString("color_mode",c.getString("value")).commit();
            assertEquals(c.getString("expected"),DisplayOptions.read(prefs).colorMode);
        }
        JSONArray palettes=contract.getJSONArray("colorModes");
        for(int i=0;i<palettes.length();i++){
            JSONObject p=palettes.getJSONObject(i);InkColors colors=InkColors.forMode(p.getString("id"));
            assertEquals(android.graphics.Color.parseColor(p.getString("paper")),colors.paper);
            assertEquals(android.graphics.Color.parseColor(p.getString("ink")),colors.ink);
        }
    }
}
