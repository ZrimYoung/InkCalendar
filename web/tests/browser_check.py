"""Browser acceptance checks against a running local Web client.

Requires Python Playwright and its Chromium browser. Each check uses an isolated
browser context; existing browser profiles and calendar data are never opened.
"""
import argparse
import json
import time
from datetime import date, timedelta
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

READ_RECORD = """async () => {
 const db=await new Promise((resolve,reject)=>{const r=indexedDB.open('ink-calendar-web',1);r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);});
 return await new Promise((resolve,reject)=>{const tx=db.transaction('snapshots');let value=null;tx.objectStore('snapshots').get('calendar').onsuccess=e=>value=e.target.result;tx.oncomplete=()=>{db.close();resolve(value);};tx.onerror=()=>reject(tx.error);});
}"""


def run(browser, base_url, production_url, screenshots):
    checks, errors = [], []

    def passed(label):
        checks.append(label)
        print("PASS " + label, flush=True)

    def context():
        return browser.new_context(viewport={"width": 1280, "height": 900}, timezone_id="Asia/Hong_Kong")

    def open_page(ctx, url=base_url):
        page = ctx.new_page()
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.goto(url)
        expect(page.locator(".home-identity")).to_be_visible()
        return page

    def record(page):
        return page.evaluate(READ_RECORD)

    def revision(page):
        return (record(page) or {}).get("revision", 0)

    def commit(page, before):
        deadline = time.monotonic() + 8
        while revision(page) <= before and time.monotonic() < deadline:
            page.wait_for_timeout(25)
        assert revision(page) > before, "No committed revision after saving"
        expect(page.locator("#ink-calendar-app")).to_have_attribute("aria-busy", "false")
        expect(page.locator("#ink-error")).to_be_hidden()

    def settings(page):
        page.locator("[data-action=open-settings]").click()

    def save_settings(page):
        before = revision(page)
        page.locator("[data-action=save]").click()
        commit(page, before)

    def add_event(page, title, on_date=None, time=""):
        page.locator("[data-action=add-event]").click()
        form = page.locator("#week-task-form")
        form.locator("[name=title]").fill(title)
        if on_date:
            form.locator("[name=date]").fill(on_date)
        form.locator("[name=time]").fill(time)
        before = revision(page)
        form.locator("[data-action=save-event]").click()
        commit(page, before)

    def seed(page, data):
        page.evaluate("""async data=>{
         const db=await new Promise(resolve=>{const r=indexedDB.open('ink-calendar-web',1);r.onsuccess=()=>resolve(r.result);});
         await new Promise((resolve,reject)=>{const tx=db.transaction('snapshots','readwrite'),store=tx.objectStore('snapshots');store.get('calendar').onsuccess=e=>store.put({key:'calendar',revision:(e.target.result?.revision||0)+1,data});tx.oncomplete=resolve;tx.onerror=()=>reject(tx.error);});db.close();sessionStorage.clear();
        }""", data)
        page.reload()
        expect(page.locator(".home-identity")).to_be_visible()
        page.evaluate("scrollTo(0,0)")
        expect(page.locator("#ink-error")).to_be_hidden()

    ctx = context()
    page = open_page(ctx)
    today = page.locator(".home-identity").get_attribute("data-today")
    assert today == page.evaluate("new Date().toLocaleDateString('sv-SE')")
    assert page.locator(".month-cell").count() == 42
    assert page.locator(".task").count() == 0
    passed("real date and empty initial calendar")

    literal_title = '读书 <img src=x onerror="alert(1)">'
    add_event(page, literal_title, time="09:30")
    assert page.locator(".task-title").inner_text() == literal_title
    assert page.locator(".task-title img").count() == 0
    before = revision(page)
    page.locator("[data-event-check]").check()
    commit(page, before)
    page.locator("[data-edit-event]").click()
    page.locator("#week-task-form [name=title]").fill("已完成后修改")
    before = revision(page)
    page.locator("[data-action=save-event]").click()
    commit(page, before)
    page.reload()
    expect(page.locator("[data-event-check]")).to_be_checked()
    assert record(page)["data"]["events"][0]["title"] == "已完成后修改"
    passed("event create/edit/completion, escaped titles and reload persistence")

    add_event(page, "删除前需确认")
    page.locator("[data-edit-event]").filter(has=page.locator(".task-title", has_text="删除前需确认")).click()
    page.locator("[data-action=delete-event]").click()
    assert len(record(page)["data"]["events"]) == 2
    before = revision(page)
    page.locator("[data-action=confirm-delete-event]").click()
    commit(page, before)
    assert len(record(page)["data"]["events"]) == 1
    passed("event deletion requires confirmation")

    page.locator("[data-action=add-event]").click()
    page.locator("#week-task-form [name=title]").fill("尚未保存的内容")
    page.locator("#week-task-form [name=time]").fill("13:45")
    page.reload()
    expect(page.locator("#week-task-form [name=title]")).to_have_value("尚未保存的内容")
    page.set_viewport_size({"width": 390, "height": 844})
    expect(page.locator("#ink-calendar-app")).to_have_attribute("data-direction", "portrait")
    expect(page.locator("#week-task-form [name=time]")).to_have_value("13:45")
    page.set_viewport_size({"width": 1280, "height": 900})
    expect(page.locator("#week-task-form [name=title]")).to_have_value("尚未保存的内容")
    page.locator("[data-action=cancel-edit]").click()
    assert len(record(page)["data"]["events"]) == 1
    passed("unsaved form survives reload and orientation changes; cancellation keeps saved data")

    settings(page)
    page.locator("[data-scale-step='5']").click()
    page.locator("[data-action=appearance]").click()
    page.locator("[data-style=minimal]").click()
    page.locator("[data-font=atkinson]").click()
    page.locator("[data-action=components]").click()
    expect(page.locator("[data-component-mini]")).to_have_count(22)
    page.evaluate("document.fonts.ready")
    for key, count in [("date", 5), ("calendar", 3), ("agenda", 4), ("buttons", 3), ("flow", 2), ("tracking", 3), ("caps", 2)]:
        images = page.locator(f"[data-component-mini='{key}']").evaluate_all("els=>els.map(e=>e.toDataURL())")
        assert len(images) == count and len(set(images)) == count, key
    if screenshots:
        page.screenshot(path=str(screenshots / "components-1280.png"), full_page=True)
    page.locator("[data-segment=date][data-value=center]").click()
    page.locator("[data-action=save]").click()
    page.locator("[data-action=save]").click()
    page.locator("[data-action=cancel]").click()
    assert record(page)["data"]["applied"]["theme"]["style"] == "editorial"
    assert record(page)["data"]["applied"]["scale"] == 100
    passed("nested appearance/component saves remain a draft until outer save")
    passed("all 22 component options have distinct focused previews")

    settings(page)
    page.locator("[data-scale-step='5']").click()
    page.locator("[data-scale-step='5']").click()
    page.locator("[data-segment=view][data-value=week]").click()
    page.locator("[data-segment=layout][data-value=balanced]").click()
    page.locator("[data-action=appearance]").click()
    page.locator("[data-font=space]").click()
    page.locator("[data-style=grid]").click()
    expect(page.locator("[data-font=space]")).to_have_attribute("aria-pressed", "true")
    page.locator("[data-action=save]").click()
    save_settings(page)
    applied = record(page)["data"]["applied"]
    assert applied["theme"]["font"] == "space" and applied["theme"]["style"] == "grid"
    assert applied["scale"] == 110 and applied["layout"] == "balanced"
    assert page.locator(".day-row").count() == 7
    assert page.locator(".home-identity").get_attribute("data-today") == today
    passed("week, balanced layout, independent font/style and plus/minus settings save")

    page.locator("[data-action=milestones]").click()
    for i in range(3):
        page.locator("[data-action=new-milestone]").click()
        page.locator("#milestone-form [name=title]").fill("纪念日 " + str(i + 1))
        if i == 2:
            page.locator("#milestone-form [name=pinned]").check()
            page.locator("[data-action=save]").click()
            expect(page.locator("#ink-error")).to_be_visible()
            expect(page.locator("#milestone-form [name=title]")).to_have_value("纪念日 3")
            assert len(record(page)["data"]["milestones"]) == 2
            page.locator("#milestone-form [name=pinned]").uncheck()
        before = revision(page)
        page.locator("[data-action=save]").click()
        commit(page, before)
    assert len(record(page)["data"]["milestones"]) == 3
    page.locator("[data-edit-milestone]").filter(has=page.locator("strong", has_text="纪念日 3")).click()
    page.locator("#milestone-form [name=kind]").select_option("countdown")
    page.locator("#milestone-form [name=date]").fill((date.fromisoformat(today) - timedelta(days=2)).isoformat())
    page.locator("#milestone-form [name=title]").fill("已过期倒计时")
    expect(page.locator("#milestone-form [name=lunar]")).to_be_disabled()
    before = revision(page)
    page.locator("[data-action=save]").click()
    commit(page, before)
    page.locator("[data-edit-milestone]").filter(has=page.locator("strong", has_text="已过期倒计时")).click()
    page.locator("[data-action=delete-milestone]").click()
    assert len(record(page)["data"]["milestones"]) == 3
    before = revision(page)
    page.locator("[data-action=confirm-delete-milestone]").click()
    commit(page, before)
    assert len(record(page)["data"]["milestones"]) == 2
    page.locator("[data-action=close-milestones]").click()
    assert page.locator(".home-milestones .milestone-row").count() == 2
    passed("milestone CRUD path and two-pin limit with failed-save draft retention")

    page.locator("[data-action=jump]").click()
    target_date = (date.fromisoformat(today) + timedelta(days=10)).isoformat()
    page.locator("#jump-form [name=date]").fill(target_date)
    page.locator("[data-action=save-jump]").click()
    expect(page.locator(f"[data-day='{target_date}']")).to_have_attribute("aria-pressed", "true")
    assert page.locator(".home-identity").get_attribute("data-today") == today
    page.locator("[data-action=today]").click()
    passed("date jump changes selection while the main date stays today")

    later = (date.fromisoformat(today) + timedelta(days=29)).isoformat()
    add_event(page, "三十天内事项", on_date=later)
    page.locator("[data-action=today]").click()
    page.locator("[data-action=future]").click()
    assert "三十天内事项" not in page.locator("#ink-calendar-app").inner_text()
    before = revision(page)
    page.locator("[data-future-days='30']").click()
    commit(page, before)
    assert "三十天内事项" in page.locator("#ink-calendar-app").inner_text()
    page.reload()
    expect(page.locator("[data-future-days='30']")).to_have_attribute("aria-pressed", "true")
    before = revision(page)
    page.locator("[data-future-hide]").check()
    commit(page, before)
    assert page.locator(".task").count() == 1
    page.locator("[data-action=close-future]").click()
    passed("7/30 day ranges, saved range choice and persisted completed-item visibility")

    # Inject a write failure before a transaction starts; saved data must survive.
    page.locator("[data-action=add-event]").click()
    page.locator("#week-task-form [name=title]").fill("写入失败后保留")
    before_record = record(page)
    page.evaluate("""()=>{window.originalTransaction=IDBDatabase.prototype.transaction;IDBDatabase.prototype.transaction=function(names,mode,...rest){if(mode==='readwrite')throw new DOMException('Simulated write failure','QuotaExceededError');return window.originalTransaction.call(this,names,mode,...rest);};}""")
    page.locator("[data-action=save-event]").click()
    expect(page.locator("#ink-error")).to_be_visible()
    expect(page.locator("#week-task-form [name=title]")).to_have_value("写入失败后保留")
    assert record(page) == before_record
    page.evaluate("()=>{IDBDatabase.prototype.transaction=window.originalTransaction;}")
    before = revision(page)
    page.locator("[data-action=save-event]").click()
    commit(page, before)
    passed("storage failure retains editor and saved snapshot, then retry succeeds")

    # The second writer deliberately lacks cross-window notifications, exercising CAS.
    other = ctx.new_page()
    other.on("pageerror", lambda error: errors.append(str(error)))
    other.add_init_script("window.BroadcastChannel=undefined")
    other.goto(base_url)
    other.locator("[data-action=add-event]").click()
    other.locator("#week-task-form [name=title]").fill("另一个窗口的草稿")
    add_event(page, "第一个窗口已保存")
    current = record(page)
    other.locator("[data-action=save-event]").click()
    expect(other.locator("#ink-error")).to_be_visible()
    expect(other.locator("#week-task-form [name=title]")).to_have_value("另一个窗口的草稿")
    assert record(page) == current
    other.locator("[data-action=reload-data]").click()
    expect(other.locator("#ink-error")).to_be_hidden()
    assert record(other) == current
    other.close()
    passed("multi-window stale writes rejected without overwriting either draft or saved data")

    # A notification-capable idle window follows committed changes automatically.
    other = open_page(ctx)
    add_event(page, "跨窗口更新")
    expect(other.locator(".task-title", has_text="跨窗口更新")).to_be_visible()
    other.close()
    passed("idle window refreshes after another window saves")

    snapshot = record(page)["data"]
    snapshot["applied"]["hideDone"] = False
    snapshot["applied"]["scale"] = 100
    # Verify month/week keep the hero in the same position for each recipe/layout.
    for width, height in [(390, 844), (1280, 900)]:
        page.set_viewport_size({"width": width, "height": height})
        for style in ["editorial", "minimal", "grid", "ticket", "today"]:
            for layout in ["classic", "balanced"]:
                rects = []
                for view in ["month", "week"]:
                    snapshot["applied"].update(view=view, layout=layout, orientation="auto", desk=False)
                    snapshot["applied"]["theme"] = {"style": style, "font": "original", "overrides": {}}
                    seed(page, snapshot)
                    page.evaluate("document.fonts.ready")
                    rects.append(page.locator(".home-identity").bounding_box())
                    assert page.locator(".month-cell" if view == "month" else ".day-row").count() == (42 if view == "month" else 7)
                    assert page.locator(".home-identity").get_attribute("data-today") == today
                    assert page.evaluate("document.documentElement.scrollWidth<=innerWidth"), (width, style, layout, view)
                    if style == "grid":
                        digits = page.locator(".home-tiles span")
                        assert digits.evaluate_all("els=>els.every(e=>getComputedStyle(e).borderTopWidth==='0px')")
                        assert digits.nth(1).bounding_box()["y"] > digits.nth(0).bounding_box()["y"]
                        assert page.locator("[data-agenda=cards] .task").first.evaluate("e=>getComputedStyle(e).borderBottomStyle") == "dashed"
                    if style == "editorial":
                        assert page.locator(".home-day").evaluate("e=>getComputedStyle(e).backgroundColor") == "rgb(0, 0, 0)"
                assert abs(rects[0]["x"] - rects[1]["x"]) < 1 and abs(rects[0]["y"] - rects[1]["y"]) < 1, (style, layout, rects)
                if screenshots and layout == "balanced":
                    page.screenshot(path=str(screenshots / f"{style}-{width}-week.png"), full_page=True)
    passed("40 theme/view/layout/viewport combinations keep today's date position and avoid page overflow")

    for width, height in [(320, 740), (736, 414), (1280, 900)]:
        page.set_viewport_size({"width": width, "height": height})
        settings(page)
        page.locator("[data-action=appearance]").click()
        expect(page.locator("[data-style]")).to_have_count(5)
        expect(page.locator("[data-font]")).to_have_count(7)
        widths = page.locator(".choice").evaluate_all("els=>els.map(e=>e.getBoundingClientRect().width)")
        assert all(120 <= w <= 208.1 for w in widths), (width, widths)
        assert page.evaluate("document.documentElement.scrollWidth<=innerWidth")
        expected_ratio = 188 / 148 if width < height else 240 / 148
        for box in page.locator("[data-mini]").evaluate_all("els=>els.map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height}))"):
            assert abs(box["w"] / box["h"] - expected_ratio) < 0.02
        if screenshots:
            page.screenshot(path=str(screenshots / f"appearance-{width}.png"), full_page=True)
        page.locator("[data-action=cancel]").click()
        page.locator("[data-action=cancel]").click()
    passed("bounded theme/font grids and orientation-aware thumbnails at narrow, short and wide sizes")

    # Ordinary landscape sizes fit the full six-week month without moving today.
    snapshot["events"] = []
    snapshot["milestones"] = []
    for width, height in [(1024, 600), (1280, 720)]:
        page.set_viewport_size({"width": width, "height": height})
        for style in ["editorial", "minimal", "grid", "ticket", "today"]:
            for layout in ["classic", "balanced"]:
                snapshot["applied"].update(view="month", layout=layout, orientation="auto", scale=130, desk=False)
                snapshot["applied"]["theme"] = {"style": style, "font": "original", "overrides": {}}
                seed(page, snapshot)
                page.evaluate("document.fonts.ready")
                page.wait_for_timeout(60)
                factor = float(page.locator(".calendar-panel").get_attribute("data-calendar-scale"))
                assert .55 <= factor <= 1
                widths = page.locator(".calendar-panel").evaluate("e=>({panel:e.getBoundingClientRect().width,surface:e.querySelector('.calendar-surface').getBoundingClientRect().width})")
                assert abs(widths["panel"] - widths["surface"]) < 1, (width, height, style, layout, widths)
                boxes = page.locator(".month-weekrow").evaluate_all("els=>els.map(e=>e.getBoundingClientRect().bottom+scrollY)")
                # Very short agenda-first frames may hit the readable minimum.
                if factor > .551:
                    assert max(boxes) <= height + 1, (width, height, style, layout, boxes)
                assert page.locator(".home-identity").get_attribute("data-today") == today
                if screenshots and style == "editorial" and layout == "classic":
                    page.screenshot(path=str(screenshots / f"month-fit-{width}.png"), full_page=True)
    page.set_viewport_size({"width": 390, "height": 844})
    seed(page, snapshot)
    assert page.locator(".calendar-panel").get_attribute("data-calendar-scale") == "1"
    snapshot["applied"]["view"] = "week"
    page.set_viewport_size({"width": 1280, "height": 900})
    seed(page, snapshot)
    assert page.locator(".calendar-panel").get_attribute("data-calendar-scale") == "1"
    passed("landscape monthly calendar fits available height while portrait and week keep their scale")

    # Horizontal gestures change only the selected calendar period.
    panel = page.locator(".calendar-panel")
    before = page.locator(".home-month-title").inner_text()
    panel.dispatch_event("pointerdown", {"pointerId": 1, "isPrimary": True, "button": 0, "clientX": 600, "clientY": 200})
    panel.dispatch_event("pointerup", {"pointerId": 1, "isPrimary": True, "button": 0, "clientX": 400, "clientY": 208})
    assert page.locator(".home-month-title").inner_text() != before
    assert page.locator(".home-identity").get_attribute("data-today") == today
    passed("calendar swipe retains the current-day hero")

    settings(page)
    page.locator("[data-segment=language][data-value=en]").click()
    save_settings(page)
    settings(page)
    assert "Calendar text size" in page.locator("#ink-calendar-app").inner_text()
    assert "Page orientation" in page.locator("#ink-calendar-app").inner_text()
    assert "Primary / secondary" in page.locator("#ink-calendar-app").inner_text()
    assert "Landscape only." not in page.locator("#ink-calendar-app").inner_text()
    page.locator("[data-action=cancel]").click()
    passed("English UI uses shared native wording and Web-specific orientation wording")
    ctx.close()

    # A bad stored snapshot is retained and shown as a recoverable startup error.
    ctx = context()
    page = open_page(ctx)
    page.evaluate("""async()=>{const db=await new Promise(resolve=>{const r=indexedDB.open('ink-calendar-web',1);r.onsuccess=()=>resolve(r.result);});await new Promise(resolve=>{const tx=db.transaction('snapshots','readwrite');tx.objectStore('snapshots').put({key:'calendar',revision:1,data:{schema:99,events:[{title:'Keep this record'}]}});tx.oncomplete=resolve;});db.close();}""")
    page.reload()
    expect(page.locator("#ink-calendar-app button")).to_have_text("重试 / Retry")
    assert record(page)["data"]["schema"] == 99
    passed("unreadable local data is retained and never silently reset")
    ctx.close()

    # The file prototype is self-contained and never opens the real calendar DB.
    ctx = context()
    prototype = Path(__file__).resolve().parents[2] / "preview/ink-calendar-preview.html"
    page = ctx.new_page()
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.goto(prototype.as_uri())
    expect(page.locator(".lab-bar")).to_be_visible()
    assert page.locator(".task").count() > 0
    page.locator("[data-lab-page=settings]").click()
    page.locator("[data-action=appearance]").click()
    assert page.locator("[data-font]").count() == 7
    passed("standalone file prototype runs offline with the shared UI")
    ctx.close()

    if production_url:
        ctx = context()
        page = open_page(ctx, production_url)
        add_event(page, "离线保留事项")
        page.evaluate("navigator.serviceWorker.ready")
        page.wait_for_function("navigator.serviceWorker.controller !== null")
        ctx.set_offline(True)
        page.reload()
        expect(page.locator(".task-title", has_text="离线保留事项")).to_be_visible()
        settings(page)
        page.locator("[data-action=appearance]").click()
        page.locator("[data-font=fraunces]").click()
        page.locator("[data-action=save]").click()
        save_settings(page)
        page.evaluate("document.fonts.ready")
        assert page.evaluate("document.fonts.check('16px InkFraunces')")
        page.reload()
        assert record(page)["data"]["applied"]["theme"]["font"] == "fraunces"
        ctx.set_offline(False)
        page.goto(production_url + "?demo=1")
        expect(page.locator(".lab-bar")).to_be_visible()
        assert len(record(page)["data"]["events"]) == 1
        page.goto(production_url)
        expect(page.locator(".task-title", has_text="离线保留事项")).to_be_visible()
        passed("production assets/fonts and IndexedDB work offline; demo data stays isolated")
        ctx.close()

    assert not errors, errors
    print(json.dumps({"passed": len(checks), "checks": checks, "pageErrors": errors}, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:5173/")
    parser.add_argument("--production-url")
    parser.add_argument("--browser", help="Optional Chromium executable")
    parser.add_argument("--screenshots", type=Path, help="Optional local ignored screenshot directory")
    args = parser.parse_args()
    if args.screenshots:
        args.screenshots.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(**({"executable_path": args.browser} if args.browser else {}))
        try:
            run(browser, args.base_url, args.production_url, args.screenshots)
        finally:
            browser.close()
