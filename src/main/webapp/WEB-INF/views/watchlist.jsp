<%@ page contentType="text/html;charset=UTF-8" language="java" buffer="64kb" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Watchlist — AUTO TRADING</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css" rel="stylesheet">

<style>
:root {
  --void: #04050a;
  --base: #080b12;
  --panel: #0e1220;
  --panel-hi: #131825;
  --panel-2: #171d2e;
  --hover: #1c2238;
  --hover2: #212840;

  --lime: #b8ff47;
  --lime-dim: #8acc30;
  --lime-d: rgba(184,255,71,.09);
  --lime-b: rgba(184,255,71,.25);
  --lime-glow: 0 0 20px rgba(184,255,71,.35);

  --blue: #5aadff;
  --blue-dim: #3d8fd9;
  --blue-d: rgba(90,173,255,.09);
  --blue-b: rgba(90,173,255,.28);
  --blue-glow: 0 0 20px rgba(90,173,255,.3);

  --emerald: #00e888;
  --emerald-d: rgba(0,232,136,.08);
  --emerald-b: rgba(0,232,136,.28);

  --red: #ff4f6b;
  --red-d: rgba(255,79,107,.09);
  --red-b: rgba(255,79,107,.3);

  --gold: #ffcc44;
  --gold-d: rgba(255,204,68,.08);
  --gold-b: rgba(255,204,68,.28);

  --purple: #a78bfa;
  --purple-d: rgba(167,139,250,.08);
  --purple-b: rgba(167,139,250,.28);

  --rim: rgba(255,255,255,.06);
  --rim-hi: rgba(255,255,255,.12);
  --rim-lo: rgba(255,255,255,.03);

  --t0: #ffffff;
  --t1: #d4dff5;
  --t2: #e6eefc;
  --t3: #d7e3f8;
  --t4: #b9c9e8;

  --mono: 'JetBrains Mono', 'Pretendard', monospace;
  --sans: 'Pretendard', sans-serif;
  --r: 6px;
  --r2: 10px;
  --r3: 14px;
  --topbar-h: 54px;
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html, body { height: 100%; background: var(--void); }
body { font-family: var(--sans); font-size: 13px; color: var(--t1); min-height: 100vh; overflow-x: hidden; }

/* ── BG ── */
.bg-layer {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background:
    radial-gradient(ellipse 70% 60% at 20% -5%, rgba(184,255,71,.06) 0%, transparent 50%),
    radial-gradient(ellipse 50% 50% at 85% 110%, rgba(90,173,255,.05) 0%, transparent 50%),
    radial-gradient(ellipse 40% 40% at 50% 50%, rgba(10,15,30,.8) 0%, transparent 70%);
}
.bg-grid {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background-image:
    linear-gradient(var(--rim-lo) 1px, transparent 1px),
    linear-gradient(90deg, var(--rim-lo) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(ellipse 90% 90% at 50% 50%, black 30%, transparent 80%);
}

/* ── ANIMATIONS ── */
@keyframes sd { from { opacity:0; transform:translateY(-10px); } to { opacity:1; transform:none; } }
@keyframes fu { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:none; } }
@keyframes slideIn { from { opacity:0; transform:translateX(-8px); } to { opacity:1; transform:none; } }
@keyframes fadeUp { from { opacity:0; transform:translateY(6px); } to { opacity:1; transform:none; } }
@keyframes pulse { 0%,100%{transform:scale(1);opacity:1;} 50%{transform:scale(.6);opacity:.3;} }
@keyframes spin { from{transform:rotate(0);} to{transform:rotate(360deg);} }
@keyframes shimmer { 0%{background-position:-200% 0;} 100%{background-position:200% 0;} }
@keyframes dropIn { from{opacity:0;transform:translateY(-6px) scale(.97);} to{opacity:1;transform:none;} }
@keyframes glow { 0%,100%{box-shadow:0 0 8px rgba(184,255,71,.2);} 50%{box-shadow:0 0 20px rgba(184,255,71,.5);} }

/* ── TOPBAR ── */
.topbar {
  position: sticky; top: 0; z-index: 300; height: var(--topbar-h);
  display: flex; align-items: center;
  background: rgba(4,5,10,.95); backdrop-filter: blur(24px);
  border-bottom: 1px solid var(--rim);
  animation: sd .35s ease both;
}
.topbar::after {
  content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent 0%, var(--lime) 35%, rgba(184,255,71,.3) 65%, transparent 100%);
  opacity: .4;
}
.tb-logo {
  display: flex; align-items: center; gap: 10px; padding: 0 18px; height: 100%;
  border-right: 1px solid var(--rim); min-width: 190px; flex-shrink: 0;
}
.logo-mk {
  width: 30px; height: 30px; background: var(--lime); border-radius: 7px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--lime-glow); flex-shrink: 0; animation: glow 3s ease-in-out infinite;
}
.logo-mk svg { width: 15px; height: 15px; }
.logo-name { font-size: 12px; font-weight: 800; letter-spacing: .8px; color: var(--t0); }
.logo-name span { color: var(--lime); }
.logo-ver { font-family: var(--mono); font-size: 7px; color: var(--t3); letter-spacing: 2px; margin-top: 2px; }
.tb-sp { flex: 1; }
.tb-nav { display: flex; align-items: center; gap: 2px; padding: 0 12px; }
.tb-a {
  font-family: var(--mono); font-size: 9px; letter-spacing: .5px; padding: 5px 10px;
  border-radius: var(--r); border: 1px solid transparent; background: transparent;
  color: var(--t2); cursor: pointer; transition: all .15s; text-decoration: none;
}
.tb-a:hover { background: var(--hover); border-color: var(--rim-hi); color: var(--t1); }
.tb-a.cur { background: var(--lime-d); border-color: var(--lime-b); color: var(--lime); font-weight: 600; }
.tb-clock {
  padding: 0 16px; height: 100%; border-left: 1px solid var(--rim);
  display: flex; flex-direction: column; align-items: flex-end; justify-content: center; gap: 2px;
}
.clk-t { font-family: var(--mono); font-size: 14px; font-weight: 600; letter-spacing: 2.5px; color: var(--t0); }
.clk-d { font-family: var(--mono); font-size: 7px; color: var(--t3); letter-spacing: 1.5px; }

/* ── PAGE ── */
.page {
  position: relative; z-index: 1;
  padding: 14px 16px;
  display: flex; flex-direction: column; gap: 12px;
  min-height: calc(100vh - var(--topbar-h));
}

/* ── PAGE HEADER ── */
.page-header {
  display: flex; align-items: center; gap: 12px;
  animation: fu .35s .05s ease both;
}
.page-title {
  font-size: 18px; font-weight: 800; letter-spacing: .5px; color: var(--t0);
}
.page-title span { color: var(--lime); }
.page-sub { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1.5px; }

/* ── STAT CARDS ── */
.stat-cards {
  display: flex; gap: 8px; flex-wrap: wrap;
  animation: fu .3s .08s ease both;
}
.stat-card {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r2);
  padding: 10px 16px; display: flex; flex-direction: column; gap: 3px;
  min-width: 100px;
}
.stat-card-label { font-family: var(--mono); font-size: 7px; color: var(--t3); letter-spacing: 1.5px; text-transform: uppercase; }
.stat-card-val { font-family: var(--mono); font-size: 20px; font-weight: 600; color: var(--t0); line-height: 1; }
.stat-card.kr .stat-card-val { color: var(--lime); }
.stat-card.us .stat-card-val { color: var(--blue); }
.stat-card.fld .stat-card-val { color: var(--gold); }

/* ── ADD TOOLBAR ── */
.add-toolbar {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r3);
  padding: 14px 16px; display: flex; align-items: flex-start; gap: 14px; flex-wrap: wrap;
  animation: fu .35s .1s ease both; position: relative;
}
.add-toolbar::before {
  content: ''; position: absolute; top: 0; left: 16px; right: 16px; height: 1px;
  background: linear-gradient(90deg, transparent, var(--lime-b), transparent);
}
.add-section { display: flex; flex-direction: column; gap: 6px; }
.add-section-label { font-family: var(--mono); font-size: 7px; color: var(--t3); letter-spacing: 1.5px; text-transform: uppercase; }
.add-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }

/* Market toggle */
.mkt-seg { display: flex; gap: 3px; }
.mkt-btn {
  height: 32px; padding: 0 13px; font-family: var(--mono); font-size: 9px; font-weight: 600; letter-spacing: .6px;
  border: 1px solid var(--rim-hi); border-radius: var(--r); background: var(--base); color: var(--t3);
  cursor: pointer; transition: all .15s;
}
.mkt-btn:hover { color: var(--t1); border-color: var(--rim-hi); background: var(--hover); }
.mkt-btn.kr { background: var(--lime-d); border-color: var(--lime-b); color: var(--lime); box-shadow: inset 0 0 12px rgba(184,255,71,.04); }
.mkt-btn.us { background: var(--blue-d); border-color: var(--blue-b); color: var(--blue); box-shadow: inset 0 0 12px rgba(90,173,255,.04); }

/* Symbol autocomplete wrapper */
.sym-wrap { position: relative; }
.sym-in {
  height: 32px; width: 160px; background: var(--base); border: 1px solid var(--rim-hi);
  border-radius: var(--r); color: var(--t0); font-family: var(--mono); font-size: 12px;
  letter-spacing: 1.5px; font-weight: 500; padding: 0 10px; outline: none;
  text-transform: uppercase; transition: all .15s;
}
.sym-in:focus { border-color: var(--lime-b); box-shadow: 0 0 0 3px rgba(184,255,71,.08); background: var(--panel); }
.sym-in::placeholder { color: var(--t3); text-transform: none; font-size: 10px; letter-spacing: .5px; font-weight: 400; }

/* Autocomplete dropdown */
.ac-drop {
  position: absolute; top: calc(100% + 4px); left: 0; width: 280px;
  background: var(--panel-2); border: 1px solid var(--rim-hi); border-radius: var(--r2);
  box-shadow: 0 12px 40px rgba(0,0,0,.6), 0 0 0 1px rgba(255,255,255,.04);
  z-index: 900; overflow: hidden; display: none;
  animation: dropIn .18s ease both;
}
.ac-drop.open { display: block; }
.ac-item {
  display: flex; align-items: center; gap: 10px; padding: 9px 12px;
  cursor: pointer; transition: background .12s; border-bottom: 1px solid var(--rim-lo);
}
.ac-item:last-child { border-bottom: none; }
.ac-item:hover, .ac-item.active { background: var(--hover2); }
.ac-sym { font-family: var(--mono); font-size: 11px; font-weight: 700; min-width: 70px; }
.ac-sym.kr { color: var(--lime); }
.ac-sym.us { color: var(--blue); }
.ac-name { font-size: 11px; color: var(--t2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.ac-badge {
  font-family: var(--mono); font-size: 7px; padding: 2px 6px; border-radius: 4px;
  letter-spacing: .5px; flex-shrink: 0;
}
.ac-badge.kr { background: var(--lime-d); border: 1px solid var(--lime-b); color: var(--lime); }
.ac-badge.us { background: var(--blue-d); border: 1px solid var(--blue-b); color: var(--blue); }
.ac-loading { padding: 14px 12px; font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1px; text-align: center; }
.ac-empty { padding: 14px 12px; font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1px; text-align: center; }

/* Folder select */
.folder-sel {
  height: 32px; min-width: 130px; background: var(--base); border: 1px solid var(--rim-hi);
  border-radius: var(--r); color: var(--t1); font-family: var(--mono); font-size: 10px;
  padding: 0 10px; outline: none; cursor: pointer; transition: border-color .15s;
  appearance: none; -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6' viewBox='0 0 10 6'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%234a5a78' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 10px center;
  padding-right: 28px;
}
.folder-sel:focus { border-color: var(--lime-b); }

/* Add button */
.add-btn {
  height: 32px; padding: 0 16px; font-family: var(--mono); font-size: 9px; font-weight: 600; letter-spacing: .6px;
  border: 1px solid var(--lime-b); border-radius: var(--r); background: var(--lime-d); color: var(--lime);
  cursor: pointer; transition: all .15s; display: inline-flex; align-items: center; gap: 6px;
}
.add-btn:hover { background: var(--lime); color: var(--void); box-shadow: var(--lime-glow); }
.add-btn:disabled { opacity: .4; cursor: not-allowed; }
.add-btn:disabled:hover { background: var(--lime-d); color: var(--lime); box-shadow: none; }
.add-btn svg { width: 11px; height: 11px; }
.add-btn.loading svg { animation: spin .6s linear infinite; }

/* New folder btn */
.new-folder-btn {
  height: 32px; padding: 0 12px; font-family: var(--mono); font-size: 9px; letter-spacing: .5px;
  border: 1px solid var(--rim-hi); border-radius: var(--r); background: transparent; color: var(--t2);
  cursor: pointer; transition: all .15s; display: inline-flex; align-items: center; gap: 5px;
}
.new-folder-btn:hover { background: var(--hover); border-color: var(--gold-b); color: var(--gold); }
.new-folder-btn svg { width: 10px; height: 10px; }

/* Message chip */
.msg-chip { display: none; font-family: var(--mono); font-size: 9px; padding: 4px 10px; border-radius: 5px; border: 1px solid var(--rim); letter-spacing: .3px; }
.msg-chip.ok { display: inline-flex; color: var(--emerald); border-color: var(--emerald-b); background: var(--emerald-d); }
.msg-chip.err { display: inline-flex; color: var(--red); border-color: var(--red-b); background: var(--red-d); }
.msg-chip.info { display: inline-flex; color: var(--gold); border-color: var(--gold-b); background: var(--gold-d); }

/* ── SEARCH BAR ── */
.search-bar {
  display: flex; align-items: center; gap: 10px;
  animation: fu .3s .15s ease both;
}
.search-wrap { position: relative; flex: 1; max-width: 320px; }
.search-wrap svg { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); width: 13px; height: 13px; color: var(--t3); pointer-events: none; }
.search-in {
  width: 100%; height: 32px; background: var(--panel); border: 1px solid var(--rim);
  border-radius: var(--r); color: var(--t1); font-family: var(--mono); font-size: 10px;
  padding: 0 10px 0 30px; outline: none; transition: all .15s;
}
.search-in:focus { border-color: var(--lime-b); box-shadow: 0 0 0 3px rgba(184,255,71,.06); }
.search-in::placeholder { color: var(--t3); }
.view-tabs { display: flex; gap: 3px; margin-left: auto; }
.view-tab {
  height: 30px; padding: 0 12px; font-family: var(--mono); font-size: 9px; letter-spacing: .5px;
  border: 1px solid var(--rim-hi); border-radius: var(--r); background: transparent; color: var(--t3);
  cursor: pointer; transition: all .13s;
}
.view-tab:hover { background: var(--hover); color: var(--t1); }
.view-tab.active { background: var(--panel-2); border-color: var(--rim-hi); color: var(--t1); }

/* ── MAIN LAYOUT ── */
.main-layout { display: flex; gap: 12px; min-height: 500px; animation: fu .35s .18s ease both; }
.sidebar { width: 200px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; }
.content { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }

/* ── FOLDER SIDEBAR ── */
.folder-panel {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r3);
  overflow: hidden; flex: 1;
}
.folder-panel-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; height: 36px; border-bottom: 1px solid var(--rim);
  background: var(--panel-hi);
}
.folder-panel-title { font-family: var(--mono); font-size: 8px; font-weight: 600; color: var(--t2); letter-spacing: 1.5px; text-transform: uppercase; }
.folder-list { padding: 6px; display: flex; flex-direction: column; gap: 2px; }
.folder-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 8px;
  border-radius: var(--r); cursor: pointer; transition: all .13s; border: 1px solid transparent;
  font-family: var(--mono); font-size: 10px; color: var(--t2); white-space: nowrap;
}
.folder-item:hover { background: var(--hover); color: var(--t1); }
.folder-item.active { background: var(--hover2); border-color: var(--rim-hi); color: var(--t0); }
.folder-item svg { width: 13px; height: 13px; flex-shrink: 0; opacity: .7; }
.folder-item.active svg { opacity: 1; }
.folder-item-count {
  margin-left: auto; font-family: var(--mono); font-size: 8px;
  padding: 1px 6px; border-radius: 4px; background: var(--base); color: var(--t3); border: 1px solid var(--rim);
  flex-shrink: 0;
}
.folder-item.active .folder-item-count { color: var(--t2); background: var(--panel); }
.folder-item.kr .folder-icon { color: var(--lime); }
.folder-item.us .folder-icon { color: var(--blue); }
        '<button class="folder-del-btn" onclick="deleteFolder(event,&quot;'+fnameEsc+'&quot;)" title="Delete folder">' +
  margin-left: 2px; width: 18px; height: 18px; border: none; background: transparent;
  color: var(--t3); cursor: pointer; border-radius: 3px; display: none; align-items: center; justify-content: center;
  transition: all .12s; flex-shrink: 0;
}
.folder-item:hover .folder-del-btn { display: flex; }
.folder-del-btn:hover { background: var(--red-d); color: var(--red); }

/* Mini stat inside sidebar */
.sidebar-stats {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r2);
  padding: 10px 12px; display: flex; flex-direction: column; gap: 8px;
}
.ss-row { display: flex; justify-content: space-between; align-items: center; }
.ss-label { font-family: var(--mono); font-size: 8px; color: var(--t3); letter-spacing: 1px; }
.ss-val { font-family: var(--mono); font-size: 12px; font-weight: 600; }
.ss-val.kr { color: var(--lime); }
.ss-val.us { color: var(--blue); }
.ss-divider { height: 1px; background: var(--rim); }

/* ── PANELS GRID ── */
.panels { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.panels.single { grid-template-columns: 1fr; }
@media(max-width:1100px) { .main-layout { flex-direction: column; } .sidebar { width: 100%; flex-direction: row; } .sidebar-stats { display: none; } .folder-panel { flex: none; } .folder-list { flex-direction: row; flex-wrap: wrap; } }
@media(max-width:700px) { .panels { grid-template-columns: 1fr; } }

/* ── TABLE PANEL ── */
.pn {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r3);
  display: flex; flex-direction: column; overflow: hidden;
}
.pn-hd {
  flex-shrink: 0; display: flex; align-items: center; justify-content: space-between;
  padding: 0 14px; height: 40px; border-bottom: 1px solid var(--rim); background: var(--panel-hi);
}
.pn-hd-l { display: flex; align-items: center; gap: 8px; }
.pn-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; animation: pulse 2.5s ease-in-out infinite; }
.pn-title { font-family: var(--mono); font-size: 9px; font-weight: 600; color: var(--t1); letter-spacing: 1.5px; text-transform: uppercase; }
.pn-count { font-family: var(--mono); font-size: 9px; padding: 2px 9px; border-radius: 5px; border: 1px solid var(--rim); color: var(--t3); background: var(--base); }
.pn-count.kr { color: var(--lime); border-color: var(--lime-b); background: var(--lime-d); }
.pn-count.us { color: var(--blue); border-color: var(--blue-b); background: var(--blue-d); }

/* ── TABLE ── */
.tbl-wrap { overflow: auto; flex: 1; max-height: 480px; scrollbar-width: thin; scrollbar-color: var(--rim-hi) transparent; }
.tbl-wrap::-webkit-scrollbar { width: 3px; }
.tbl-wrap::-webkit-scrollbar-thumb { background: var(--rim-hi); border-radius: 2px; }
table { width: 100%; border-collapse: collapse; }
thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--panel-hi); border-bottom: 1px solid var(--rim);
  font-family: var(--mono); font-size: 8px; font-weight: 600; color: var(--t2);
  letter-spacing: 1.2px; text-transform: uppercase; padding: 9px 12px; text-align: left; white-space: nowrap;
}
tbody td { padding: 10px 12px; border-bottom: 1px solid var(--rim-lo); vertical-align: middle; white-space: nowrap; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover td { background: var(--hover); }
tbody tr { animation: fadeUp .2s ease both; }

.td-idx { font-family: var(--mono); font-size: 9px; color: var(--t3); width: 28px; }
.td-sym { font-family: var(--mono); font-size: 12px; font-weight: 700; }
.td-sym.kr { color: var(--lime); }
.td-sym.us { color: var(--blue); }
.td-name { font-size: 12px; color: var(--t1); max-width: 180px; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.td-name.dim { color: var(--t3); font-size: 10px; font-style: italic; }
.td-folder { font-family: var(--mono); font-size: 9px; color: var(--t3); }
.td-folder span { padding: 2px 7px; border-radius: 4px; background: var(--panel-2); border: 1px solid var(--rim); color: var(--t2); }
.td-date { font-family: var(--mono); font-size: 9px; color: var(--t3); }

/* Action buttons */
.action-cell { display: flex; align-items: center; gap: 4px; }
.del-btn {
  height: 26px; padding: 0 10px; font-family: var(--mono); font-size: 8px; font-weight: 600; letter-spacing: .3px;
  border: 1px solid var(--red-b); border-radius: 4px; background: var(--red-d); color: var(--red);
  cursor: pointer; transition: all .12s;
}
.del-btn:hover { background: var(--red); color: #fff; }
.del-btn:disabled { opacity: .35; cursor: not-allowed; }
.mv-btn {
  height: 26px; padding: 0 8px; font-family: var(--mono); font-size: 8px; letter-spacing: .3px;
  border: 1px solid var(--rim-hi); border-radius: 4px; background: transparent; color: var(--t3);
  cursor: pointer; transition: all .12s;
}
.mv-btn:hover { background: var(--hover2); color: var(--t1); border-color: var(--rim-hi); }

.tbl-empty { text-align: center; padding: 44px !important; font-family: var(--mono); font-size: 10px; color: var(--t3); letter-spacing: 1.5px; }
.tbl-empty-icon { font-size: 28px; margin-bottom: 10px; opacity: .3; }

/* ── FOLDER NEW MODAL ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 600; background: rgba(0,0,0,.7);
  backdrop-filter: blur(8px); display: none; align-items: center; justify-content: center;
}
.modal-overlay.open { display: flex; }
.modal {
  background: var(--panel-2); border: 1px solid var(--rim-hi); border-radius: var(--r3);
  padding: 24px; width: 340px;
  box-shadow: 0 24px 60px rgba(0,0,0,.7);
  animation: dropIn .2s ease both;
}
.modal-title { font-size: 14px; font-weight: 700; color: var(--t0); margin-bottom: 16px; }
.modal-label { font-family: var(--mono); font-size: 8px; color: var(--t3); letter-spacing: 1.5px; margin-bottom: 6px; }
.modal-in {
  width: 100%; height: 36px; background: var(--base); border: 1px solid var(--rim-hi);
  border-radius: var(--r); color: var(--t0); font-family: var(--mono); font-size: 12px;
  padding: 0 12px; outline: none; margin-bottom: 14px; transition: border-color .15s;
}
.modal-in:focus { border-color: var(--lime-b); }

/* Color picker */
.color-grid { display: flex; gap: 6px; margin-bottom: 18px; }
.color-swatch {
  width: 26px; height: 26px; border-radius: 6px; cursor: pointer; border: 2px solid transparent;
  transition: transform .12s, border-color .12s;
}
.color-swatch:hover { transform: scale(1.15); }
.color-swatch.sel { border-color: rgba(255,255,255,.6); transform: scale(1.1); }

.modal-btns { display: flex; gap: 8px; justify-content: flex-end; }
.modal-cancel {
  height: 32px; padding: 0 14px; font-family: var(--mono); font-size: 9px;
  border: 1px solid var(--rim-hi); border-radius: var(--r); background: transparent; color: var(--t2);
  cursor: pointer; transition: all .13s;
}
.modal-cancel:hover { background: var(--hover); color: var(--t1); }
.modal-ok {
  height: 32px; padding: 0 16px; font-family: var(--mono); font-size: 9px; font-weight: 600;
  border: 1px solid var(--lime-b); border-radius: var(--r); background: var(--lime-d); color: var(--lime);
  cursor: pointer; transition: all .13s;
}
.modal-ok:hover { background: var(--lime); color: var(--void); }

/* ── MOVE DROPDOWN ── */
.move-drop {
  position: absolute; min-width: 160px;
  background: var(--panel-2); border: 1px solid var(--rim-hi); border-radius: var(--r2);
  box-shadow: 0 8px 28px rgba(0,0,0,.6); z-index: 450; overflow: hidden;
  animation: dropIn .15s ease both;
}
.move-drop-item {
  padding: 8px 12px; font-family: var(--mono); font-size: 10px; color: var(--t2);
  cursor: pointer; transition: background .1s; border-bottom: 1px solid var(--rim-lo);
}
.move-drop-item:last-child { border-bottom: none; }
.move-drop-item:hover { background: var(--hover2); color: var(--t1); }

/* Folder icon dot */
.f-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

/* Inline folder badge on table row */
.td-folbadge {
  display: inline-flex; align-items: center; gap: 4px;
  font-family: var(--mono); font-size: 8px; padding: 2px 7px; border-radius: 4px;
  border: 1px solid var(--rim); background: var(--panel); color: var(--t2);
}
.td-folbadge .f-dot { width: 6px; height: 6px; }

/* Shimmer skeleton */
.skeleton {
  display: inline-block; width: 80px; height: 10px; border-radius: 4px;
  background: linear-gradient(90deg, var(--panel) 0%, var(--hover) 50%, var(--panel) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mk">
      <svg viewBox="0 0 24 24" fill="none" stroke="#04050a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div>
      <div class="logo-name">AUTO<span>TRADE</span></div>
      <div class="logo-ver">TERMINAL v2.0</div>
    </div>
  </div>
  <div class="tb-sp"></div>
  <div class="tb-nav">
    <a class="tb-a" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- PAGE HEADER -->
  <div class="page-header">
    <div>
      <div class="page-title">Watch<span>list</span></div>
      <div class="page-sub">종목 관심 목록 관리</div>
    </div>
  </div>

  <!-- STAT CARDS -->
  <div class="stat-cards">
    <div class="stat-card">
      <div class="stat-card-label">Total</div>
      <div class="stat-card-val" id="statTotal">0</div>
    </div>
    <div class="stat-card kr">
      <div class="stat-card-label">한국 KR</div>
      <div class="stat-card-val" id="statKR">0</div>
    </div>
    <div class="stat-card us">
      <div class="stat-card-label">미국 US</div>
      <div class="stat-card-val" id="statUS">0</div>
    </div>
    <div class="stat-card fld">
      <div class="stat-card-label">폴더</div>
      <div class="stat-card-val" id="statFld">0</div>
    </div>
  </div>

  <!-- ADD TOOLBAR -->
  <div class="add-toolbar">
    <!-- Market -->
    <div class="add-section">
      <div class="add-section-label">마켓</div>
      <div class="mkt-seg">
        <button class="mkt-btn kr" id="mktKR" onclick="setAddMkt('KR')">🇰🇷 KR</button>
        <button class="mkt-btn" id="mktUS" onclick="setAddMkt('US')">🇺🇸 US</button>
      </div>
    </div>

    <!-- Symbol input w/ autocomplete -->
    <div class="add-section">
      <div class="add-section-label">종목 코드</div>
      <div class="sym-wrap">
        <input class="sym-in" id="symIn" placeholder="005930" maxlength="12" autocomplete="off" spellcheck="false">
        <div class="ac-drop" id="acDrop"></div>
      </div>
    </div>

    <!-- Folder -->
    <div class="add-section">
      <div class="add-section-label">폴더</div>
      <select class="folder-sel" id="folderSel">
        <option value="">📂 기본 (미분류)</option>
      </select>
    </div>

    <!-- Add button -->
    <div class="add-section" style="justify-content:flex-end;">
      <div class="add-section-label">&nbsp;</div>
      <div class="add-row">
        <button class="add-btn" id="addBtn" onclick="addSymbol()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          Add
        </button>
        <button class="new-folder-btn" onclick="openFolderModal()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
            <line x1="12" y1="11" x2="12" y2="17"/><line x1="9" y1="14" x2="15" y2="14"/>
          </svg>
          폴더 추가
        </button>
        <span class="msg-chip" id="msgChip"></span>
      </div>
    </div>
  </div>

  <!-- SEARCH BAR -->
  <div class="search-bar">
    <div class="search-wrap">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
      <input class="search-in" id="searchIn" placeholder="종목 / 종목명 검색" autocomplete="off" spellcheck="false">
 
    </div>
    <div class="view-tabs">
      <button class="view-tab active" id="viewBoth" onclick="setView('both')">전체</button>
      <button class="view-tab" id="viewKR" onclick="setView('kr')">KR만</button>
      <button class="view-tab" id="viewUS" onclick="setView('us')">US만</button>
    </div>
  </div>

  <!-- MAIN LAYOUT -->
  <div class="main-layout">
    <!-- SIDEBAR -->
    <div class="sidebar">
      <div class="folder-panel">
        <div class="folder-panel-hd">
          <span class="folder-panel-title">Folders</span>
        </div>
        <div class="folder-list" id="folderList">
          <!-- filled by JS -->
        </div>
      </div>
      <div class="sidebar-stats">
        <div class="ss-row">
          <span class="ss-label">KR 종목</span>
          <span class="ss-val kr" id="sideKR">0</span>
        </div>
        <div class="ss-divider"></div>
        <div class="ss-row">
          <span class="ss-label">US 종목</span>
          <span class="ss-val us" id="sideUS">0</span>
        </div>
      </div>
    </div>

    <!-- TABLE PANELS -->
    <div class="content">
      <div class="panels" id="panelsGrid">
        <!-- KR Panel -->
        <section class="pn" id="panelKR">
          <div class="pn-hd">
            <div class="pn-hd-l">
              <div class="pn-dot" style="background:var(--lime);box-shadow:0 0 8px rgba(184,255,71,.6);"></div>
              <span class="pn-title">한국 종목</span>
            </div>
            <span class="pn-count kr" id="krCount">0</span>
          </div>
          <div class="tbl-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width:32px;">#</th>
                  <th>Symbol</th>
                  <th>종목명</th>
                  <th>폴더</th>
                  <th>등록일시</th>
                  <th style="width:80px;"></th>
                </tr>
              </thead>
              <tbody id="krBody">
                <tr><td colspan="6" class="tbl-empty"><div class="tbl-empty-icon">🇰🇷</div>로딩 중…</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- US Panel -->
        <section class="pn" id="panelUS">
          <div class="pn-hd">
            <div class="pn-hd-l">
              <div class="pn-dot" style="background:var(--blue);box-shadow:0 0 8px rgba(90,173,255,.6);"></div>
              <span class="pn-title">미국 종목</span>
            </div>
            <span class="pn-count us" id="usCount">0</span>
          </div>
          <div class="tbl-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width:32px;">#</th>
                  <th>Symbol</th>
                  <th>종목명</th>
                  <th>폴더</th>
                  <th>등록일시</th>
                  <th style="width:80px;"></th>
                </tr>
              </thead>
              <tbody id="usBody">
                <tr><td colspan="6" class="tbl-empty"><div class="tbl-empty-icon">🇺🇸</div>로딩 중…</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  </div>
</div>

<!-- FOLDER MODAL -->
<div class="modal-overlay" id="folderModal">
  <div class="modal">
    <div class="modal-title">새 폴더 만들기</div>
    <div class="modal-label">폴더 이름</div>
    <input class="modal-in" id="folderNameIn" placeholder="예: 반도체, 빅테크…" maxlength="20">
    <div class="modal-label">폴더 색상</div>
    <div class="color-grid" id="colorGrid"></div>
    <div class="modal-btns">
      <button class="modal-cancel" onclick="closeFolderModal()">취소</button>
      <button class="modal-ok" onclick="createFolder()">만들기</button>
    </div>
  </div>
</div>

<!-- MOVE DROPDOWN (portal) -->
<div class="move-drop" id="moveDrop" style="display:none;position:fixed;"></div>

<script>
'use strict';
const BASE = '${pageContext.request.contextPath}';
const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  /* ───── State ───── */
  let allItems    = [];
  let nameCache   = Object.create(null);
  let fetchingSet = new Set();
  let addMkt      = 'KR';
  let searchQ     = '';
  let viewMode    = 'both';   // 'both' | 'kr' | 'us'
  let activeFolderKey = 'ALL'; // 'ALL' | folder name
  let acTimer     = null;
  let acIdx       = -1;
  let acResults   = [];
  let moveDDTarget = null;

  /* ───── Folders (DB-backed) ───── */
  let folders     = []; // [{name, color}]


  const COLORS = ['#b8ff47','#5aadff','#ff4f6b','#ffcc44','#a78bfa','#00e888','#ff8c42','#38b6ff'];

  function loadStorage(){
    try { folders = JSON.parse(localStorage.getItem('wl_folders_v1') || '[]'); } catch(e){ folders=[]; }
  }
  function saveStorage(){
    localStorage.setItem('wl_folders_v1', JSON.stringify(folders));
  }

  /* ───── Clock ───── */
  function p2(v){ return String(v).padStart(2,'0'); }
  function tick(){
    const n=new Date();
    document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
    document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
  }
  setInterval(tick,1000); tick();

  /* ───── XSS ───── */
  function esc(s){
    return String(s==null?'':s)
      .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
      .replace(/"/g,'&quot;').replace(/'/g,'&#x27;');
  }
  function escJsSingle(s){
    return String(s==null?'':s).replace(/\\/g,'\\\\').replace(/'/g,"\\'");
  }

  /* ───── Validation ───── */
  const KR_PAT = /^[0-9]{6}$/;
  const US_PAT = /^[A-Z][A-Z0-9.\-]{0,9}$/;
  function validateSym(s,m){ return m==='KR'?KR_PAT.test(s):US_PAT.test(s); }

  /* ───── Market detect ───── */
  function detectMkt(item){
    const ex=String(item.exchange||'').toUpperCase();
    if(ex==='KRX'||ex==='KR') return 'KR';
    if(['NAS','NYS','AMS','US'].includes(ex)) return 'US';
    return /^[A-Za-z]/.test(item.symbol) ? 'US' : 'KR';
  }

  /* ───── Message ───── */
  let msgTimer=null;
  function showMsg(txt,type){
    const el=document.getElementById('msgChip');
    el.textContent=txt;
    el.className='msg-chip '+(type==='ok'?'ok':type==='err'?'err':'info');
    clearTimeout(msgTimer);
    msgTimer=setTimeout(()=>el.className='msg-chip',3200);
  }

  /* ───── Market toggle ───── */
  window.setAddMkt=function(m){
    addMkt=m;
    document.getElementById('mktKR').className='mkt-btn'+(m==='KR'?' kr':'');
    document.getElementById('mktUS').className='mkt-btn'+(m==='US'?' us':'');
    const inp=document.getElementById('symIn');
    inp.placeholder=m==='KR'?'005930':'AAPL';
    inp.value='';
    inp.focus();
    closeAc();
  };

  /* ───── View mode ───── */
  window.setView=function(mode){
    viewMode=mode;
    ['both','kr','us'].forEach(v=>{
      document.getElementById('view'+v.replace('b','B').replace('k','K').replace('u','U')).classList.toggle('active',v===mode);
    });
    document.getElementById('viewBoth').classList.toggle('active',mode==='both');
    document.getElementById('viewKR').classList.toggle('active',mode==='kr');
    document.getElementById('viewUS').classList.toggle('active',mode==='us');
    const pgrid=document.getElementById('panelsGrid');
    pgrid.className='panels'+(mode!=='both'?' single':'');
    document.getElementById('panelKR').style.display=(mode==='us')?'none':'';
    document.getElementById('panelUS').style.display=(mode==='kr')?'none':'';
    renderAll(allItems);
  };

  /* ───── Search ───── */
  window.applySearch=function(){
    searchQ=document.getElementById('searchIn').value.toLowerCase().trim();
    renderAll(allItems);
  };

  window.searchByApi=function(){
    const q=document.getElementById('searchIn').value.trim();
    if(!q){ 
      searchQ='';
      renderAll(allItems);
      return;
    }
    fetch(BASE+'/api/watchlist/search?q='+encodeURIComponent(q)+'&limit=100')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d||!Array.isArray(d.data)){
          applySearch();
          return;
        }
        allItems=d.data;
        allItems.forEach(it=>{ if(it.symbol&&(it.name||it.symbolName)) nameCache[it.symbol]=it.name||it.symbolName||''; });
        searchQ=q.toLowerCase();
        const syms=allItems.map(it=>it.symbol).filter(Boolean);
        loadNames(syms).then(()=>renderAll(allItems));
      })
      .catch(()=>applySearch());
  };
  /* ───── Date format ───── */
  function formatDate(v){
    if(!v) return '—';
    if(Array.isArray(v)&&v.length>=3)
      return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+(v.length>=6?' '+p2(v[3])+':'+p2(v[4]):'');
    return String(v).replace('T',' ').substring(0,16);
  }

  /* ───── Name loading ───── */
  function loadNames(symbols){
    const missing=[...new Set(symbols)].filter(s=>s&&nameCache[s]===undefined&&!fetchingSet.has(s));
    if(!missing.length) return Promise.resolve();
    missing.forEach(s=>fetchingSet.add(s));
    return Promise.all(missing.map(s=>
      fetch(BASE+'/api/watchlist/name?symbol='+encodeURIComponent(s))
        .then(r=>r.ok?r.json():null)
        .then(d=>{ nameCache[s]=(d&&(d.symbolName||d.name)||''); })
        .catch(()=>{ nameCache[s]=''; })
        .finally(()=>fetchingSet.delete(s))
    ));
  }
  function fillNamesInDOM(items){
    const syms=items.map(it=>it.symbol).filter(Boolean);
    loadNames(syms).then(()=>{
      syms.forEach(s=>{
        const el=document.getElementById('nm-'+s);
        if(el&&nameCache[s]){
          el.textContent=nameCache[s];
          el.className='td-name';
        }
      });
    });
  }

  /* ───── Render folders sidebar ───── */
  function renderFolders(krItems,usItems){
    const list=document.getElementById('folderList');
    // Count per folder
    const counts=Object.create(null);
    [...krItems,...usItems].forEach(it=>{
      const fn=(it.folder||'').trim();
      counts[fn]=(counts[fn]||0)+1;
    });
    const total=[...krItems,...usItems].length;

    let html='';
    // ALL
    html+='<div class="folder-item'+(activeFolderKey==='ALL'?' active':'')+' " onclick="setActiveFolder(&quot;ALL&quot;)">' +
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' +
      '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>' +
      '<rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>' +
      '</svg>' +
      '전체 종목' +
      '<span class="folder-item-count">'+total+'</span></div>';

    // Unclassified
    const unclassifiedCount=counts['']||0;
    html+='<div class="folder-item'+(activeFolderKey===''?' active':'')+' " onclick="setActiveFolder(&quot;&quot;)">' +
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' +
      '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>' +
      '</svg>' +
      '미분류' +
      '<span class="folder-item-count">'+unclassifiedCount+'</span></div>';

    // Ensure known folders from data are present in sidebar
    const knownFolderNames = new Set(folders.map(f=>f.name));
    [...krItems, ...usItems].forEach(it=>{
      const folderName=(it.folder||'').trim();
      if(folderName && !knownFolderNames.has(folderName)){
        knownFolderNames.add(folderName);
        folders.push({name:folderName, color:COLORS[knownFolderNames.size % COLORS.length] || COLORS[0]});
      }
    });

    // Custom folders
    folders.forEach(f=>{
      const cnt=counts[f.name]||0;
      const fname=f.name;
      const fnameEsc=esc(f.name);
      const fnameJs=escJsSingle(fname);
      const fcolor=esc(f.color);
      html+='<div class="folder-item'+(activeFolderKey===f.name?' active':'')+' " onclick="setActiveFolder(&quot;'+fnameEsc+'&quot;)">' +
        '<span class="f-dot" style="background:'+fcolor+';box-shadow:0 0 5px '+fcolor+'55;"></span>' +
        fnameEsc +
        '<span class="folder-item-count">'+cnt+'</span>' +
        '<button class="folder-del-btn" onclick="deleteFolder(event,&quot;'+fnameEsc+'&quot;)" title="Delete folder">' +
        '<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
        '</button></div>';
    });

    list.innerHTML=html;

    // Update folder select
    const sel=document.getElementById('folderSel');
    const cur=sel.value;
    sel.innerHTML='<option value="">📂 기본 (미분류)</option>';
    folders.forEach(f=>{
      const opt=document.createElement('option');
      opt.value=f.name;
      opt.textContent='🗂 '+f.name;
      sel.appendChild(opt);
    });
    if([...sel.options].some(o=>o.value===cur)) sel.value=cur;

    document.getElementById('statFld').textContent=folders.length;
  }

  /* ───── Set active folder ───── */
  window.setActiveFolder=function(name){
    activeFolderKey=name;
    renderAll(allItems);
  };

  /* ───── Render rows ───── */
  function renderRows(bodyId, items, mktCls){
    const body=document.getElementById(bodyId);

    // Filter by folder
    let filtered=items;
    if(activeFolderKey!=='ALL'){
      filtered=items.filter(it=>(it.folder||'').trim()===(activeFolderKey||'').trim());
    }

    // Filter by search
    if(searchQ){
      filtered=filtered.filter(it=>{
        const sym=(it.symbol||'').toLowerCase();
        const nm=((it.name||it.symbolName||nameCache[it.symbol])||'').toLowerCase();
        return sym.includes(searchQ)||nm.includes(searchQ);
      });
    }

    if(!filtered.length){
      body.innerHTML='<tr><td colspan="6" class="tbl-empty"><div class="tbl-empty-icon">'+(mktCls==='kr'?'🇰🇷':'🇺🇸')+'</div>종목이 없습니다</td></tr>';
      return;
    }

    body.innerHTML=filtered.map((it,i)=>{
      const sym=esc(it.symbol||'—');
      const nm=(it.name||it.symbolName||nameCache[it.symbol]);
      const nmHtml=nm!=null
        ?'<td class="td-name">'+esc(nm||'—')+'</td>'
        :'<td class="td-name dim" id="nm-'+sym+'"><span class="skeleton"></span></td>';
      const dt=esc(formatDate(it.createdAt||it.addedAt||''));
      const folName=(it.folder||'').trim();
      const folBadge=folName
        ? '<span class="td-folbadge"><span class="f-dot" style="background:'+esc(getFolderColor(folName))+';"></span>'+esc(folName)+'</span>'
        : '<span style="color:var(--t4);font-family:var(--mono);font-size:9px;">—</span>';
      return '<tr style="animation-delay:'+i*15+'ms">' +
        '<td class="td-idx">'+(i+1)+'</td>' +
        '<td class="td-sym '+mktCls+'">'+sym+'</td>' +
        nmHtml +
        '<td class="td-folder">'+folBadge+'</td>' +
        '<td class="td-date">'+dt+'</td>' +
        '<td>' +
          '<div class="action-cell" style="position:relative;">' +
            '<button class="mv-btn" data-id="'+esc(it.id)+'" data-sym="'+sym+'" onclick="openMoveDrop(event,'+it.id+')">▸</button>' +
            '<button class="del-btn" data-id="'+esc(it.id)+'">삭제</button>' +
          '</div>' +
        '</td>' +
      '</tr>';
    }).join('');
  }

  function getFolderColor(name){
    const f=folders.find(x=>x.name===name);
    return f?f.color:'#4a5a78';
  }

  /* ───── Render all ───── */
  function renderAll(items){
    const krItems=items.filter(it=>detectMkt(it)==='KR');
    const usItems=items.filter(it=>detectMkt(it)==='US');

    document.getElementById('statTotal').textContent=items.length;
    document.getElementById('statKR').textContent=krItems.length;
    document.getElementById('statUS').textContent=usItems.length;
    document.getElementById('sideKR').textContent=krItems.length;
    document.getElementById('sideUS').textContent=usItems.length;
    document.getElementById('krCount').textContent=krItems.length+'개';
    document.getElementById('usCount').textContent=usItems.length+'개';

    renderFolders(krItems,usItems);
    renderRows('krBody',krItems,'kr');
    renderRows('usBody',usItems,'us');
    fillNamesInDOM([...krItems,...usItems]);
  }

  /* ───── Load watchlist ───── */
  function load(){
    fetch(BASE+'/api/watchlist')
      .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
      .then(rows=>{
        allItems=Array.isArray(rows)?rows:[];
        allItems.forEach(it=>{
          if(it.symbol&&(it.name||it.symbolName)){
            nameCache[it.symbol]=it.name||it.symbolName||'';
          }
        });
        renderAll(allItems);
      })
      .catch(e=>{
        const msg='<tr><td colspan="6" class="tbl-empty">로드 실패: '+esc(e.message)+'</td></tr>';
        document.getElementById('krBody').innerHTML=msg;
        document.getElementById('usBody').innerHTML=msg;
      });
  }

  /* ───── AUTOCOMPLETE ───── */
  function closeAc(){
    document.getElementById('acDrop').classList.remove('open');
    document.getElementById('acDrop').innerHTML='';
    acResults=[]; acIdx=-1;
  }

  function renderAc(items, loading){
    const drop=document.getElementById('acDrop');
    if(loading){
      drop.innerHTML='<div class="ac-loading">검색 중…</div>';
      drop.classList.add('open');
      return;
    }
    if(!items.length){
      drop.innerHTML='<div class="ac-empty">결과 없음</div>';
      drop.classList.add('open');
      return;
    }
    acResults=items;
    drop.innerHTML=items.map((it,i)=>{
      const sym=esc(it.symbol||'');
      const nm=esc(it.name||'');
      const mkt=(it.market||addMkt).toLowerCase();
      return '<div class="ac-item" data-idx="'+i+'" data-sym="'+sym+'" onmousedown="pickAc('+i+')">' +
        '<span class="ac-sym '+mkt+'">'+sym+'</span>' +
        '<span class="ac-name">'+(nm||'-')+'</span>' +
        '<span class="ac-badge '+mkt+'">'+mkt.toUpperCase()+'</span>' +
        '</div>';
    }).join('');
    drop.classList.add('open');
    acIdx=-1;
  }

  function triggerAc(val){
    if(!val||val.length<1){ closeAc(); return; }
    clearTimeout(acTimer);
    renderAc([], true);
    acTimer=setTimeout(()=>{
      fetch(BASE+'/api/market/symbol-suggest?q='+encodeURIComponent(val)+'&market='+addMkt+'&exch='+(addMkt==='KR'?'KRX':'NAS')+'&limit=8')
        .then(r=>r.ok?r.json():null)
        .then(d=>{
          if(!d||!Array.isArray(d.data)){ closeAc(); return; }
          renderAc(d.data, false);
        })
        .catch(()=>closeAc());
    }, 260);
  }

  window.pickAc=function(idx){
    const it=acResults[idx];
    if(!it) return;
    document.getElementById('symIn').value=it.symbol.toUpperCase();
    closeAc();
  };

  /* ───── Add Symbol ───── */
  window.addSymbol=function(){
    const inp=document.getElementById('symIn');
    const raw=(inp.value||'').trim().toUpperCase().replace(/[^A-Z0-9.\-]/g,'');
    if(!raw){ showMsg('종목코드를 입력하세요.','err'); inp.focus(); return; }
    if(!validateSym(raw,addMkt)){
      showMsg(addMkt==='KR'?'KR: 6자리 숫자 (예: 005930)':'US: 영문+숫자 (예: AAPL)','err');
      inp.focus(); return;
    }
    const dup=allItems.some(it=>String(it.symbol||'').toUpperCase()===raw);
    if(dup){ showMsg(raw+' 은(는) 이미 등록됨','info'); return; }

    const btn=document.getElementById('addBtn');
    btn.disabled=true; btn.classList.add('loading');
    const exchange=addMkt==='KR'?'KRX':'NAS';
    const folder=document.getElementById('folderSel').value;
    fetch(BASE+'/api/watchlist',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'symbol='+encodeURIComponent(raw)
        +'&exchange='+encodeURIComponent(exchange)
        +'&folder='+encodeURIComponent(folder||'')
    })
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json().catch(()=>({}));})
    .then(d=>{
      if(d.status==='DUPLICATE'){ showMsg(raw+' 이미 등록됨','info'); return; }
      if(d.status&&d.status!=='OK'){ showMsg(d.message||'추가 실패','err'); return; }
      // Apply folder assignment after reload
      const pendingFolder=document.getElementById('folderSel').value;
      inp.value=''; closeAc();
      showMsg(raw+' 추가 완료 ✓','ok');
      fetch(BASE+'/api/watchlist')
        .then(r=>r.json())
        .then(rows=>{
          allItems=Array.isArray(rows)?rows:[];
          allItems.forEach(it=>{ if(it.symbol&&(it.name||it.symbolName)) nameCache[it.symbol]=it.name||it.symbolName||''; });
          // Assign folder to newly added item
          if(pendingFolder){
            const newItem=allItems.find(it=>String(it.symbol||'').toUpperCase()===raw);
            if(newItem){ newItem.folder=pendingFolder; }
          }
          renderAll(allItems);
        });
    })
    .catch(e=>showMsg('추가 실패: '+e.message,'err'))
    .finally(()=>{ btn.disabled=false; btn.classList.remove('loading'); });
  };

  /* ───── Delete ───── */
  function onDelete(e){
    const btn=e.target.closest('.del-btn');
    if(!btn) return;
    const id=btn.dataset.id;
    if(!id||isNaN(Number(id))||Number(id)<=0) return;
    if(!confirm('삭제하시겠습니까?')) return;
    btn.disabled=true; btn.textContent='…';
    fetch(BASE+'/api/watchlist/delete',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'id='+encodeURIComponent(id)
    })
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json().catch(()=>({}));})
    .then(d=>{
      if(d.status==='OK'||!d.status){
        showMsg('삭제 완료','ok');
        load();
      } else { showMsg(d.message||'삭제 실패','err'); btn.disabled=false; btn.textContent='삭제'; }
    })
    .catch(e=>{ showMsg('삭제 실패: '+e.message,'err'); btn.disabled=false; btn.textContent='삭제'; });
  }
  document.getElementById('krBody').addEventListener('click', onDelete);
  document.getElementById('usBody').addEventListener('click', onDelete);

  /* ───── Move to folder dropdown ───── */
  window.openMoveDrop=function(e, itemId){
    e.stopPropagation();
    const drop=document.getElementById('moveDrop');
    const rect=e.currentTarget.getBoundingClientRect();
    drop.style.display='block';
    drop.style.top=(rect.bottom+4)+'px';
    drop.style.left=rect.left+'px';
    moveDDTarget=itemId;

    let html='<div class="move-drop-item" onclick="moveToFolder(null)">Move to unassigned</div>';
    folders.forEach(f=>{
      const fnameRaw=f.name;
      const fname=esc(f.name);
      const fnameJs=escJsSingle(fnameRaw);
      const fcolor=esc(f.color);
      html+='<div class="move-drop-item" onclick="moveToFolder(&quot;'+fname+'&quot;)">'
        + '<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:'+fcolor+';margin-right:6px;vertical-align:middle;"></span>'
        + fname
        + '</div>';
    });
    drop.innerHTML=html;
  };
  window.moveToFolder=function(folderName){
    if(moveDDTarget!=null){
      const id=Number(moveDDTarget);
      const payload='id='+encodeURIComponent(id)+'&folder='+encodeURIComponent(folderName||'');
      fetch(BASE+'/api/watchlist/folder',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:payload})
        .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
        .then(()=>{
          const item=allItems.find(x=>x.id===id);
          if(item){ item.folder = folderName||''; }
          renderAll(allItems);
        })
        .catch(e=>showMsg('폴더 이동 실패: '+e.message,'err'));
    }
    document.getElementById('moveDrop').style.display='none';
  };
  document.addEventListener('click',()=>{ document.getElementById('moveDrop').style.display='none'; });

  /* ───── Folder modal ───── */
  let selectedColor=COLORS[0];

  function buildColorGrid(){
    const grid=document.getElementById('colorGrid');
    grid.innerHTML=COLORS.map(c=>
      '<div class="color-swatch'+(c===selectedColor?' sel':'')+'" style="background:'+c+'" onclick="selectColor(\''+c+'\')"></div>'
    ).join('');
  }
  window.selectColor=function(c){
    selectedColor=c;
    buildColorGrid();
  };
  window.openFolderModal=function(){
    document.getElementById('folderNameIn').value='';
    selectedColor=COLORS[0];
    buildColorGrid();
    document.getElementById('folderModal').classList.add('open');
    setTimeout(()=>document.getElementById('folderNameIn').focus(),80);
  };
  window.closeFolderModal=function(){
    document.getElementById('folderModal').classList.remove('open');
  };
  window.createFolder=function(){
    const name=document.getElementById('folderNameIn').value.trim();
    if(!name){ document.getElementById('folderNameIn').focus(); return; }
    if(folders.some(f=>f.name===name)){ alert('동일한 이름의 폴더가 이미 있습니다.'); return; }
    folders.push({name, color:selectedColor});
    saveStorage();
    closeFolderModal();
    renderAll(allItems);
  };
  window.deleteFolder=function(e, name){
    e.stopPropagation();
    if(!confirm('"'+name+'" 폴더를 삭제할까요?\n(종목은 삭제되지 않고 미분류로 이동됩니다.)')) return;
    folders=folders.filter(f=>f.name!==name);
    fetch(BASE+'/api/watchlist/folder/clear',{
      method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'folder='+encodeURIComponent(name)
    })
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
    .then(()=>{
      allItems.forEach(item=>{ if((item.folder||'')===name) item.folder=''; });
      if(activeFolderKey===name) activeFolderKey='ALL';
      renderAll(allItems);
      showMsg('폴더 '+name+' 삭제 및 매핑 초기화 완료','ok');
    })
    .catch(e=>showMsg('폴더 삭제 실패: '+e.message,'err'));
  };

  /* ───── Autocomplete input events ───── */
  const symIn=document.getElementById('symIn');
  symIn.addEventListener('input',e=>{
    const v=e.target.value.trim().toUpperCase();
    if(v.length>=1) triggerAc(v);
    else closeAc();
  });
  symIn.addEventListener('keydown',e=>{
    const drop=document.getElementById('acDrop');
    if(e.key==='ArrowDown'){ e.preventDefault();
      acIdx=Math.min(acIdx+1,acResults.length-1);
      highlightAc();
    } else if(e.key==='ArrowUp'){ e.preventDefault();
      acIdx=Math.max(acIdx-1,0);
      highlightAc();
    } else if(e.key==='Enter'){
      if(acIdx>=0&&acResults[acIdx]){ pickAc(acIdx); }
      else addSymbol();
    } else if(e.key==='Escape'){ closeAc(); }
  });
  symIn.addEventListener('blur',()=>setTimeout(closeAc,180));

  function highlightAc(){
    document.querySelectorAll('.ac-item').forEach((el,i)=>{
      el.classList.toggle('active',i===acIdx);
    });
  }

  /* ───── Init ───── */
  let searchTimer=null;
  const searchIn=document.getElementById('searchIn');
  if(searchIn){
    searchIn.addEventListener('input',e=>{
      clearTimeout(searchTimer);
      searchTimer=setTimeout(()=>{
        const q=e.target.value.trim();
        if(q.length>=2) searchByApi();
        else applySearch();
      }, 260);
    });
  }
  
  loadStorage();
  load();

  // Folder modal ESC
  document.addEventListener('keydown',e=>{ if(e.key==='Escape') closeFolderModal(); });
  document.getElementById('folderModal').addEventListener('click',e=>{ if(e.target===e.currentTarget) closeFolderModal(); });
  document.getElementById('folderNameIn').addEventListener('keydown',e=>{ if(e.key==='Enter') createFolder(); });
</script>
</body>
</html>
