// Browser-only storage for sample data. No connection to the Android database.
(() => {
  if (window.openai) return;
  const key = 'ink-calendar-prototype-v2';
  let saved = null;
  try { saved = JSON.parse(localStorage.getItem(key)); } catch (_) { /* Storage can be unavailable for file URLs. */ }
  window.openai = {
    widgetState: saved,
    setWidgetState: async snapshot => {
      window.openai.widgetState = snapshot;
      try { localStorage.setItem(key, JSON.stringify(snapshot)); } catch (_) { /* Keep the current session usable. */ }
    }
  };
})();
