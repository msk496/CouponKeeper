const SHEET_NAME = 'Coupons';
const SHARED_KEY = 'REPLACE_WITH_YOUR_OWN_LONG_RANDOM_KEY';
const HEADERS = ['id', 'title', 'body', 'expiry', 'used', 'updatedAt'];

function doGet(e) {
  if (!authorized_(e.parameter.key)) return json_({ ok: false, error: 'unauthorized' });
  const sheet = sheet_();
  const rows = sheet.getDataRange().getValues();
  const coupons = [];
  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0]) continue;
    coupons.push({ id: String(r[0]), title: String(r[1]), body: String(r[2]), expiry: Number(r[3]), used: String(r[4]).toLowerCase() === 'true' || r[4] === true });
  }
  return json_({ ok: true, coupons: coupons });
}

function doPost(e) {
  let input;
  try { input = JSON.parse(e.postData.contents); } catch (_) { return json_({ ok: false, error: 'invalid_json' }); }
  if (!authorized_(input.key || e.parameter.key)) return json_({ ok: false, error: 'unauthorized' });
  const c = input.coupon || {};
  if (!c.id) return json_({ ok: false, error: 'missing_id' });
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const sheet = sheet_();
    const rows = sheet.getDataRange().getValues();
    let row = -1;
    for (let i = 1; i < rows.length; i++) if (String(rows[i][0]) === String(c.id)) { row = i + 1; break; }
    const values = [String(c.id), String(c.title || ''), String(c.body || ''), Number(c.expiry || 0), Boolean(c.used), new Date()];
    if (row < 0) sheet.appendRow(values); else sheet.getRange(row, 1, 1, values.length).setValues([values]);
    return json_({ ok: true });
  } finally { lock.releaseLock(); }
}

function authorized_(key) { return Boolean(key) && key === SHARED_KEY && SHARED_KEY !== 'REPLACE_WITH_YOUR_OWN_LONG_RANDOM_KEY'; }
function sheet_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) sheet = ss.insertSheet(SHEET_NAME);
  if (sheet.getLastRow() === 0) sheet.appendRow(HEADERS);
  return sheet;
}
function json_(value) { return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON); }
