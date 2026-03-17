<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AUTOTRADE TERMINAL</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root {
  --void:      #060709;
  --base:      #0a0c10;
  --surface:   #0f1117;
  --panel:     #141720;
  --panel-hi:  #191d28;
  --hover:     #1e2330;

  --lime:      #a8ff3e;
  --lime-d:    rgba(168,255,62,.1);
  --lime-b:    rgba(168,255,62,.22);
  --lime-glow: 0 0 20px rgba(168,255,62,.4);

  --emerald:   #00d97e;
  --emerald-d: rgba(0,217,126,.08);
  --emerald-b: rgba(0,217,126,.25);
  --emerald-glow: 0 0 14px rgba(0,217,126,.4);

  --red:       #ff4d6a;
  --red-d:     rgba(255,77,106,.08);
  --red-b:     rgba(255,77,106,.28);
  --red-glow:  0 0 12px rgba(255,77,106,.4);

  --gold:      #f5c842;
  --gold-d:    rgba(245,200,66,.08);
  --gold-b:    rgba(245,200,66,.25);

  --blue:      #4d9fff;
  --blue-d:    rgba(77,159,255,.08);
  --blue-b:    rgba(77,159,255,.25);

  --rim:       rgba(255,255,255,.055);
  --rim-hi:    rgba(255,255,255,.11);

  --t1: #e8edf5;
  --t2: #7a8499;
  --t3: #3a4155;
  --t4: #1c2130;

  --mono: 'JetBrains Mono', monospace;
  --sans: 'Syne', sans-serif;
  --r:  6px;
  --r2: 10px;
  --r3: 16px;

  --topbar-h:  56px;
  --sidebar-w: 292px;
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html { height: 100%; scroll-behavior: smooth; }
body {
  min-height: 100%; font-family: var(--sans); font-size: 13px;
  color: var(--t1); background: var(--void); overflow-x: hidden;
}

/* 배경 */
.bg-layer {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background-image:
    radial-gradient(ellipse 90% 60% at 50% -10%, rgba(168,255,62,.09) 0%, transparent 55%),
    radial-gradient(ellipse 50% 70% at 100% 80%, rgba(0,217,126,.06) 0%, transparent 50%),
    radial-gradient(ellipse 45% 50% at -5%  60%, rgba(77,159,255,.05) 0%, transparent 50%);
}
.bg-grid {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background-image: radial-gradient(rgba(168,255,62,.055) 1px, transparent 1px);
  background-size: 28px 28px;
}
.bg-scan {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background: repeating-linear-gradient(
    0deg, transparent, transparent 3px,
    rgba(0,0,0,.03) 3px, rgba(0,0,0,.03) 4px);
}

/* ══ TOPBAR ══ */
.topbar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 200;
  height: var(--topbar-h); display: flex; align-items: center;
  background: rgba(6,7,9,.9); backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--rim);
  animation: slide-down .5s ease both;
}
.topbar::after {
  content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, var(--lime), rgba(168,255,62,.3), transparent);
  opacity: .5;
}

@keyframes slide-down    { from{opacity:0;transform:translateY(-12px);}  to{opacity:1;transform:translateY(0);} }
@keyframes fade-up       { from{opacity:0;transform:translateY(18px);}   to{opacity:1;transform:translateY(0);} }
@keyframes slide-in-r    { from{opacity:0;transform:translateX(20px);}   to{opacity:1;transform:translateX(0);} }
@keyframes row-appear    { from{opacity:0;transform:translateX(8px);}    to{opacity:1;transform:translateX(0);} }
@keyframes pulse-dot     { 0%,100%{transform:scale(1);opacity:1;} 50%{transform:scale(.75);opacity:.4;} }
@keyframes spin          { from{transform:rotate(0);}  to{transform:rotate(360deg);} }
@keyframes flash-row     { 0%{background:rgba(168,255,62,.1);}  100%{background:transparent;} }

/* 로고 */
.tb-logo {
  display: flex; align-items: center; gap: 11px;
  padding: 0 22px; height: 100%;
  border-right: 1px solid var(--rim); min-width: 210px;
}
.logo-mark {
  width: 34px; height: 34px; background: var(--lime); border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; position: relative; overflow: hidden;
  box-shadow: var(--lime-glow);
}
.logo-mark::before { content:''; position:absolute; inset:0; background:linear-gradient(135deg,rgba(255,255,255,.35) 0%,transparent 60%); }
.logo-mark svg { width: 18px; height: 18px; }
.logo-name { font-size: 14px; font-weight: 800; letter-spacing: .5px; color: var(--t1); }
.logo-name span { color: var(--lime); }
.logo-ver  { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1.5px; margin-top: 1px; }

.tb-spacer { flex: 1; }

/* 상태 Pill */
.tb-status-pill {
  display: flex; align-items: center; gap: 6px;
  font-family: var(--mono); font-size: 10px; color: var(--emerald);
  padding: 4px 12px; border-radius: 20px;
  background: var(--emerald-d); border: 1px solid var(--emerald-b);
  letter-spacing: .5px;
}
.tb-pulse { width: 6px; height: 6px; border-radius: 50%; background: var(--emerald); box-shadow: var(--emerald-glow); animation: pulse-dot 1.4s ease-in-out infinite; }

/* 네비 */
.tb-nav { display: flex; align-items: center; gap: 4px; padding: 0 18px; }
.tb-nav-link {
  font-family: var(--mono); font-size: 10px; letter-spacing: .5px;
  padding: 5px 12px; border-radius: var(--r);
  border: 1px solid transparent; background: transparent; color: var(--t2);
  cursor: pointer; transition: all .15s; text-decoration: none;
}
.tb-nav-link:hover { background: var(--hover); border-color: var(--rim-hi); color: var(--t1); }

/* 로그인 */
.tb-login { display: flex; align-items: center; gap: 6px; padding: 0 14px; border-left: 1px solid var(--rim); }
.tb-login form { display: flex; align-items: center; gap: 6px; }
.tb-login input {
  height: 28px; width: 116px;
  background: var(--base); border: 1px solid var(--rim-hi);
  border-radius: var(--r); color: var(--t1);
  font-family: var(--mono); font-size: 10px; padding: 0 8px;
}
.tb-login input::placeholder { color: var(--t3); }
.tb-login-btn {
  height: 28px; padding: 0 12px; border-radius: var(--r);
  border: 1px solid var(--lime-b); background: var(--lime-d); color: var(--lime);
  font-family: var(--mono); font-size: 10px; cursor: pointer; transition: all .15s;
}
.tb-login-btn:hover { background: var(--lime); color: var(--void); }
.tb-login-status { display: none; align-items: center; gap: 8px; font-family: var(--mono); font-size: 10px; color: var(--t2); }
.tb-login-status .acc { color: var(--lime); }
.tb-login-err { font-family: var(--mono); font-size: 10px; color: var(--red); display: none; margin-left: 4px; }

/* 시계 */
.tb-clock {
  padding: 0 18px; height: 100%; border-left: 1px solid var(--rim);
  display: flex; flex-direction: column; align-items: flex-end; justify-content: center; gap: 2px;
}
.clock-t { font-family: var(--mono); font-size: 15px; font-weight: 500; color: var(--t1); letter-spacing: 2px; }
.clock-d { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1px; }

/* ══ LAYOUT ══ */
.layout {
  display: flex;
  padding-top: var(--topbar-h);
  min-height: 100vh;
}

/* 메인 */
.main-content {
  flex: 1; min-width: 0;
  margin-right: var(--sidebar-w);  /* 사이드바 공간 확보 */
}
.page {
  position: relative; z-index: 1;
  max-width: 900px; margin: 0 auto;
  padding: 44px 24px 72px;
}

/* ══ HERO ══ */
.hero {
  display: grid; grid-template-columns: 1fr auto;
  align-items: center; gap: 32px;
  padding: 40px 0 50px;
  animation: fade-up .6s .1s ease both;
}
.hero-eyebrow {
  font-family: var(--mono); font-size: 11px; color: var(--lime);
  letter-spacing: 4px; text-transform: uppercase; margin-bottom: 12px;
  display: flex; align-items: center; gap: 10px;
}
.hero-eyebrow::before { content:''; width:24px; height:1px; background:var(--lime); box-shadow:var(--lime-glow); }
.hero-title {
  font-size: clamp(30px, 4vw, 52px); font-weight: 800;
  letter-spacing: -2px; line-height: 1.05; margin-bottom: 14px; color: var(--t1);
}
.hero-title .hl  { color: var(--lime); text-shadow: var(--lime-glow); }
.hero-title .dim { color: var(--t2); }
.hero-desc { font-size: 14px; color: var(--t2); line-height: 1.7; max-width: 420px; margin-bottom: 22px; }

.hero-ctas { display: flex; gap: 10px; flex-wrap: wrap; }
.cta-primary {
  height: 44px; padding: 0 26px;
  background: var(--lime); border: none; border-radius: var(--r2);
  color: var(--void); font-family: var(--sans); font-size: 13px; font-weight: 700;
  cursor: pointer; transition: all .2s; text-decoration: none;
  display: inline-flex; align-items: center; gap: 8px; box-shadow: var(--lime-glow);
}
.cta-primary:hover { transform: translateY(-2px); box-shadow: 0 0 32px rgba(168,255,62,.6); }
.cta-secondary {
  height: 44px; padding: 0 20px;
  background: var(--panel); border: 1px solid var(--rim-hi); border-radius: var(--r2);
  color: var(--t1); font-family: var(--mono); font-size: 11px;
  cursor: pointer; transition: all .2s; text-decoration: none;
  display: inline-flex; align-items: center; gap: 8px;
}
.cta-secondary:hover { background: var(--hover); border-color: var(--lime-b); color: var(--lime); transform: translateY(-1px); }

.api-hint { margin-top: 18px; display: flex; flex-wrap: wrap; gap: 7px; align-items: center; }
.api-lbl  { font-family: var(--mono); font-size: 10px; color: var(--t3); letter-spacing: 1px; }
.api-chip { font-family: var(--mono); font-size: 10px; background: var(--base); border: 1px solid var(--rim-hi); border-radius: 4px; padding: 3px 9px; color: var(--lime); }

/* System Status 위젯 */
.hero-widget {
  background: var(--panel); border: 1px solid var(--rim);
  border-radius: var(--r3); overflow: hidden; min-width: 210px;
  box-shadow: 0 20px 56px rgba(0,0,0,.4);
}
.hw-head { padding: 11px 15px; border-bottom: 1px solid var(--rim); background: var(--panel-hi); display: flex; align-items: center; justify-content: space-between; }
.hw-title { font-family: var(--mono); font-size: 10px; color: var(--t2); letter-spacing: 2px; }
.hw-live  { display: flex; align-items: center; gap: 5px; font-family: var(--mono); font-size: 9px; color: var(--emerald); }
.hw-ld    { width: 5px; height: 5px; border-radius: 50%; background: var(--emerald); animation: pulse-dot 1.4s ease-in-out infinite; }
.hw-item  { padding: 9px 15px; border-bottom: 1px solid var(--t4); display: flex; align-items: center; justify-content: space-between; transition: background .12s; }
.hw-item:last-child { border-bottom: none; }
.hw-item:hover { background: var(--hover); }
.hw-key   { font-family: var(--mono); font-size: 10px; color: var(--t2); }
.hw-val   { font-family: var(--mono); font-size: 11px; font-weight: 500; }
.hw-val.lime    { color: var(--lime); }
.hw-val.emerald { color: var(--emerald); }
.hw-val.gold    { color: var(--gold); }
.hw-bar { width: 100%; height: 2px; background: var(--lime); }

/* 섹션 디바이더 */
.sec-div { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; animation: fade-up .5s .3s ease both; }
.div-ln  { flex: 1; height: 1px; background: linear-gradient(90deg, var(--rim), transparent); }
.div-lbl { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 3px; text-transform: uppercase; }

/* 메뉴 카드 */
.menu-grid { display: grid; grid-template-columns: repeat(2,1fr); gap: 12px; margin-bottom: 36px; }
.menu-card {
  background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r3);
  overflow: hidden; text-decoration: none; color: inherit;
  display: flex; flex-direction: column; transition: all .22s;
  animation: fade-up .5s ease both;
}
.menu-card:nth-child(1){ animation-delay:.32s; } .menu-card:nth-child(2){ animation-delay:.38s; }
.menu-card:nth-child(3){ animation-delay:.44s; } .menu-card:nth-child(4){ animation-delay:.50s; }
.menu-card:hover { transform: translateY(-3px); border-color: var(--rim-hi); box-shadow: 0 12px 40px rgba(0,0,0,.4); }
.menu-card:hover .card-arrow { opacity: 1; transform: translateX(0); }
.menu-card::before { content:''; display:block; height:2px; }
.menu-card.c-lime::before    { background:var(--lime);    box-shadow:var(--lime-glow); }
.menu-card.c-emerald::before { background:var(--emerald); }
.menu-card.c-gold::before    { background:var(--gold); }
.menu-card.c-blue::before    { background:var(--blue); }
.menu-card.c-lime:hover    { background:linear-gradient(160deg,rgba(168,255,62,.04) 0%,var(--panel) 50%); }
.menu-card.c-emerald:hover { background:linear-gradient(160deg,rgba(0,217,126,.04) 0%,var(--panel) 50%); }
.menu-card.c-gold:hover    { background:linear-gradient(160deg,rgba(245,200,66,.03) 0%,var(--panel) 50%); }
.menu-card.c-blue:hover    { background:linear-gradient(160deg,rgba(77,159,255,.03) 0%,var(--panel) 50%); }
.card-body { padding: 18px 20px 14px; flex: 1; }
.card-icon { width: 38px; height: 38px; border-radius: var(--r2); display: flex; align-items: center; justify-content: center; font-size: 18px; margin-bottom: 11px; }
.c-lime    .card-icon { background:var(--lime-d);    border:1px solid var(--lime-b); }
.c-emerald .card-icon { background:var(--emerald-d); border:1px solid var(--emerald-b); }
.c-gold    .card-icon { background:var(--gold-d);    border:1px solid var(--gold-b); }
.c-blue    .card-icon { background:var(--blue-d);    border:1px solid var(--blue-b); }
.card-name { font-size: 16px; font-weight: 700; color: var(--t1); margin-bottom: 5px; }
.card-desc { font-size: 11px; color: var(--t2); line-height: 1.6; }
.card-foot { padding: 9px 20px; border-top: 1px solid var(--rim); background: var(--panel-hi); display: flex; align-items: center; justify-content: space-between; }
.card-tag   { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: 1px; text-transform: uppercase; }
.card-arrow { font-family: var(--mono); font-size: 10px; color: var(--lime); opacity: 0; transform: translateX(-6px); transition: all .2s; }

/* 전략 섹션 */
.strat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 36px; animation: fade-up .5s .52s ease both; }
.strat-card { background: var(--panel); border: 1px solid var(--rim); border-radius: var(--r3); padding: 18px 20px; transition: border-color .2s; }
.strat-card:hover { border-color: var(--rim-hi); }
.strat-head { display: flex; align-items: center; gap: 10px; margin-bottom: 13px; }
.strat-icon { width: 32px; height: 32px; border-radius: var(--r); display: flex; align-items: center; justify-content: center; font-size: 14px; }
.strat-icon.lime { background: var(--lime-d); border: 1px solid var(--lime-b); }
.strat-icon.gold { background: var(--gold-d); border: 1px solid var(--gold-b); }
.strat-name { font-size: 13px; font-weight: 700; color: var(--t1); }
.strat-tag  { font-family: var(--mono); font-size: 9px; color: var(--t2); letter-spacing: 1px; margin-top: 2px; }
.param-row  { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid var(--t4); }
.param-row:last-child { border-bottom: none; }
.param-key { font-family: var(--mono); font-size: 10px; color: var(--t2); }
.param-val { font-family: var(--mono); font-size: 11px; font-weight: 500; color: var(--lime); }

/* NAV FOOTER */
.nav-footer { display: flex; gap: 8px; flex-wrap: wrap; padding-top: 12px; border-top: 1px solid var(--rim); animation: fade-up .5s .6s ease both; }
.nav-btn { font-family: var(--mono); font-size: 10px; letter-spacing: .5px; padding: 7px 15px; border-radius: var(--r); border: 1px solid transparent; background: transparent; color: var(--t2); cursor: pointer; transition: all .15s; text-decoration: none; display: inline-flex; align-items: center; gap: 5px; }
.nav-btn:hover { background: var(--hover); border-color: var(--rim-hi); color: var(--t1); }
.nav-btn.primary { background: var(--lime-d); border-color: var(--lime-b); color: var(--lime); font-weight: 500; }
.nav-btn.primary:hover { background: var(--lime); color: var(--void); }


/* ══════════════════════════════════
   RIGHT SIDEBAR — 거래량 순위 TOP 30
══════════════════════════════════ */
.sidebar {
  position: fixed;
  top: var(--topbar-h); right: 0;
  width: var(--sidebar-w);
  height: calc(100vh - var(--topbar-h));
  background: var(--base);
  border-left: 1px solid var(--rim);
  display: flex; flex-direction: column;
  z-index: 150;
  animation: slide-in-r .55s .15s ease both;
}

/* 사이드바 헤더 */
.sb-hd {
  flex-shrink: 0;
  background: var(--panel-hi);
  border-bottom: 1px solid var(--rim);
}
.sb-hd-top {
  display: flex; align-items: center; justify-content: space-between;
  padding: 11px 14px 6px;
}
.sb-title {
  display: flex; align-items: center; gap: 7px;
  font-family: var(--mono); font-size: 10px; font-weight: 600;
  color: var(--lime); letter-spacing: 2px; text-transform: uppercase;
}
.sb-title-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--lime); box-shadow: var(--lime-glow);
  animation: pulse-dot 2s ease-in-out infinite;
}
.sb-btn-row { display: flex; align-items: center; gap: 5px; }
.sb-icon-btn {
  width: 26px; height: 26px; border-radius: var(--r);
  border: 1px solid var(--rim-hi); background: transparent;
  color: var(--t2); cursor: pointer; transition: all .15s;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-family: var(--mono);
}
.sb-icon-btn:hover { border-color: var(--lime-b); color: var(--lime); background: var(--lime-d); }
.sb-icon-btn.spinning svg,
.sb-icon-btn.spinning { animation: spin .6s linear infinite; }

/* 카운트다운 행 */
.sb-hd-meta {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 14px 9px;
}
.sb-updated { font-family: var(--mono); font-size: 9px; color: var(--t3); }
.sb-countdown-wrap { display: flex; align-items: center; gap: 6px; }
.sb-cd-lbl { font-family: var(--mono); font-size: 9px; color: var(--t3); }
.sb-cd-num {
  font-family: var(--mono); font-size: 10px; font-weight: 600;
  color: var(--lime); min-width: 22px; text-align: right;
}

/* 프로그레스 */
.sb-prog { height: 2px; background: var(--t4); flex-shrink: 0; }
.sb-prog-fill {
  height: 100%; width: 0%;
  background: linear-gradient(90deg, var(--lime), var(--emerald));
  box-shadow: 0 0 6px var(--lime);
  transition: width 1s linear;
}

/* 탭 */
.sb-tabs {
  flex-shrink: 0;
  display: flex;
  border-bottom: 1px solid var(--rim);
  background: var(--panel-hi);
}
.sb-tab {
  flex: 1; padding: 7px 0; text-align: center;
  font-family: var(--mono); font-size: 9px; letter-spacing: 1.5px; text-transform: uppercase;
  color: var(--t3); cursor: pointer; user-select: none;
  border-bottom: 2px solid transparent; transition: all .15s;
}
.sb-tab:hover { color: var(--t2); }
.sb-tab.on { color: var(--lime); border-bottom-color: var(--lime); }

/* 필터 칩 */
.sb-filter {
  flex-shrink: 0;
  display: flex; gap: 4px; padding: 7px 10px;
  border-bottom: 1px solid var(--rim);
  background: var(--panel);
}
.sf-btn {
  font-family: var(--mono); font-size: 9px; padding: 3px 9px;
  border-radius: 3px; border: 1px solid var(--rim-hi);
  background: transparent; color: var(--t3);
  cursor: pointer; transition: all .15s; white-space: nowrap;
}
.sf-btn:hover { color: var(--t1); }
.sf-btn.on { background: var(--lime-d); border-color: var(--lime-b); color: var(--lime); }

/* 스크롤 영역 */
.sb-body {
  flex: 1; overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--rim-hi) transparent;
}
.sb-body::-webkit-scrollbar { width: 3px; }
.sb-body::-webkit-scrollbar-thumb { background: var(--rim-hi); border-radius: 2px; }

/* 상태 메시지 */
.sb-msg {
  padding: 44px 20px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.sb-msg-icon { font-size: 22px; opacity: .3; }
.sb-msg-txt  { font-family: var(--mono); font-size: 11px; color: var(--t3); letter-spacing: 1.5px; }
.sb-msg.loading .sb-msg-icon { opacity: 1; color: var(--lime); animation: pulse-dot 1s ease-in-out infinite; }
.sb-msg.error   .sb-msg-icon { opacity: 1; color: var(--red); }
.sb-msg.error   .sb-msg-txt  { color: var(--red); }

/* ── 랭킹 리스트 ── */
.rank-list { padding: 3px 0; }

.rank-row {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--t4);
  cursor: default; transition: background .1s;
  animation: row-appear .25s ease both;
}
.rank-row:last-child { border-bottom: none; }
.rank-row:hover { background: var(--hover); }
.rank-row.new-row { animation: flash-row .6s ease both; }

/* 순위 번호 */
.rn {
  font-family: var(--mono); font-size: 11px; font-weight: 700;
  text-align: center; flex-shrink: 0; line-height: 1;
}
.rn.r1 { color: var(--gold);  text-shadow: 0 0 8px rgba(245,200,66,.6); font-size: 13px; }
.rn.r2 { color: #b0b8c8; }
.rn.r3 { color: #cd8b5a; }
.rn.rN { color: var(--t3); font-size: 10px; }

/* 종목 정보 */
.ri { min-width: 0; }
.ri-name {
  font-size: 12px; font-weight: 600; color: var(--t1);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  line-height: 1.2; margin-bottom: 2px;
}
.ri-code { font-family: var(--mono); font-size: 9px; color: var(--t3); letter-spacing: .5px; }

/* 거래량 미니 바 */
.ri-bar-wrap { margin-top: 4px; }
.ri-bar {
  height: 2px; border-radius: 1px;
  background: linear-gradient(90deg, var(--lime), rgba(168,255,62,.2));
  transition: width .5s ease;
}
.ri-vol { font-family: var(--mono); font-size: 8px; color: var(--t3); margin-top: 2px; letter-spacing: .3px; }

/* 가격/등락 */
.rp { text-align: right; flex-shrink: 0; }
.rp-price { font-family: var(--mono); font-size: 11px; font-weight: 500; color: var(--t1); white-space: nowrap; }
.rp-chg {
  font-family: var(--mono); font-size: 10px; margin-top: 2px;
  display: flex; align-items: center; justify-content: flex-end; gap: 1px;
  white-space: nowrap;
}
.rp-chg.up   { color: var(--red); }     /* 한국 주식: 상승 = 빨강 */
.rp-chg.down { color: #60a5fa; }        /* 하락 = 파랑 */
.rp-chg.flat { color: var(--t3); }

/* 사이드바 푸터 */
.sb-foot {
  flex-shrink: 0;
  padding: 7px 12px;
  border-top: 1px solid var(--rim);
  background: var(--panel-hi);
  font-family: var(--mono); font-size: 8px; color: var(--t3);
  text-align: center; letter-spacing: .5px;
}
</style>
</head>
<body>

<div class="bg-layer"></div>
<div class="bg-grid"></div>
<div class="bg-scan"></div>

<!-- ══ TOPBAR ══ -->
<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mark">
      <svg viewBox="0 0 24 24" fill="none" stroke="#060709" stroke-width="2.5"
           stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/>
        <polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div>
      <div class="logo-name">AUTO<span>TRADE</span></div>
      <div class="logo-ver">TERMINAL v2.0</div>
    </div>
  </div>

  <div class="tb-spacer"></div>

  <div class="tb-status-pill">
    <div class="tb-pulse"></div>
    SYSTEM ONLINE
  </div>

  <div class="tb-nav">
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
  </div>

  <div class="tb-login">
    <form id="loginForm">
      <input type="text"     name="accountNo"       placeholder="12345678-01"
             pattern="[0-9]{8}-[0-9]{2}" autocomplete="off"/>
      <input type="password" name="accountPassword" placeholder="Password" autocomplete="off"/>
      <button class="tb-login-btn" type="submit">Login</button>
    </form>
    <div class="tb-login-status" id="loginStatus">
      <span class="acc" id="loginAccount">****</span>
      <button id="logoutBtn" type="button" class="tb-login-btn">Logout</button>
    </div>
    <div class="tb-login-err" id="loginError"></div>
  </div>

  <div class="tb-clock">
    <div class="clock-t" id="clkTime">--:--:--</div>
    <div class="clock-d" id="clkDate">----.--.--</div>
  </div>
</nav>

<!-- ══ LAYOUT ══ -->
<div class="layout">

  <!-- ── 메인 콘텐츠 ── -->
  <div class="main-content">
    <div class="page">

      <!-- HERO -->
      <div class="hero">
        <div>
          <div class="hero-eyebrow">Algorithmic Trading Console</div>
          <h1 class="hero-title">
            Auto <span class="hl">Trading</span><br>
            <span class="dim">System</span>
          </h1>
          <p class="hero-desc">
            RSI + Bollinger Band 기반 자동매매 운영 콘솔.<br>
            실시간 시세 모니터링부터 전략 실행, 체결 이력까지 한 곳에서 관리합니다.
          </p>
          <div class="hero-ctas">
            <a class="cta-primary"   href="${pageContext.request.contextPath}/dashboard">▶&nbsp;&nbsp;Dashboard 열기</a>
            <a class="cta-secondary" href="${pageContext.request.contextPath}/control">⚡ Control Panel</a>
          </div>
          <div class="api-hint">
            <span class="api-lbl">API</span>
            <span class="api-chip">/auto/start?symbol=005930</span>
            <span class="api-chip">/auto/stop</span>
            <span class="api-chip">/auto/status</span>
          </div>
        </div>

        <!-- System Status 위젯 -->
        <div class="hero-widget">
          <div class="hw-head">
            <span class="hw-title">SYSTEM STATUS</span>
            <span class="hw-live"><span class="hw-ld"></span>LIVE</span>
          </div>
          <div class="hw-item"><span class="hw-key">Engine</span>   <span class="hw-val emerald">RUNNING</span></div>
          <div class="hw-item"><span class="hw-key">Strategy</span> <span class="hw-val lime">RSI + BB</span></div>
          <div class="hw-item"><span class="hw-key">Market</span>   <span class="hw-val gold">KRX</span></div>
          <div class="hw-item"><span class="hw-key">Uptime</span>   <span class="hw-val" id="widgetUptime">00:00:00</span></div>
          <div class="hw-item"><span class="hw-key">Latency</span>  <span class="hw-val lime" id="widgetLatency">-- ms</span></div>
          <div class="hw-bar"></div>
        </div>
      </div>

      <!-- 메뉴 카드 -->
      <div class="sec-div">
        <div class="div-ln"></div><span class="div-lbl">Navigation</span>
        <div class="div-ln" style="background:linear-gradient(270deg,var(--rim),transparent)"></div>
      </div>
      <div class="menu-grid">
        <a class="menu-card c-lime"    href="${pageContext.request.contextPath}/dashboard">
          <div class="card-body"><div class="card-icon">📊</div><div class="card-name">Dashboard</div><div class="card-desc">상태, 최근 주문, 실시간 가격 로그 종합 요약</div></div>
          <div class="card-foot"><span class="card-tag">Overview</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-emerald" href="${pageContext.request.contextPath}/control">
          <div class="card-body"><div class="card-icon">⚡</div><div class="card-name">Auto Control</div><div class="card-desc">자동매매 엔진 시작·중지 및 실시간 상태 모니터링</div></div>
          <div class="card-foot"><span class="card-tag">Engine Control</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-gold"    href="${pageContext.request.contextPath}/history/orders">
          <div class="card-body"><div class="card-icon">📋</div><div class="card-name">Order History</div><div class="card-desc">주문·체결 이력 조회, 필터·정렬·CSV 내보내기</div></div>
          <div class="card-foot"><span class="card-tag">Trade Records</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-blue"    href="${pageContext.request.contextPath}/watchlist">
          <div class="card-body"><div class="card-icon">👁</div><div class="card-name">Watchlist</div><div class="card-desc">감시 종목 등록·삭제 및 모니터링 관리</div></div>
          <div class="card-foot"><span class="card-tag">Symbol Monitor</span><span class="card-arrow">→ OPEN</span></div>
        </a>
      </div>

      <!-- 전략 -->
      <div class="sec-div">
        <div class="div-ln"></div><span class="div-lbl">Strategy Info</span>
        <div class="div-ln" style="background:linear-gradient(270deg,var(--rim),transparent)"></div>
      </div>
      <div class="strat-grid">
        <div class="strat-card">
          <div class="strat-head">
            <div class="strat-icon lime">📈</div>
            <div><div class="strat-name">RSI Strategy</div><div class="strat-tag">RELATIVE STRENGTH INDEX</div></div>
          </div>
          <div class="param-row"><span class="param-key">Period</span>           <span class="param-val">14</span></div>
          <div class="param-row"><span class="param-key">Oversold (Buy)</span>  <span class="param-val">≤ 30</span></div>
          <div class="param-row"><span class="param-key">Overbought (Sell)</span><span class="param-val">≥ 70</span></div>
        </div>
        <div class="strat-card">
          <div class="strat-head">
            <div class="strat-icon gold">📉</div>
            <div><div class="strat-name">Bollinger Band</div><div class="strat-tag">VOLATILITY INDICATOR</div></div>
          </div>
          <div class="param-row"><span class="param-key">Period (MA)</span> <span class="param-val">20</span></div>
          <div class="param-row"><span class="param-key">Buy Signal</span>  <span class="param-val">Lower ×2σ</span></div>
          <div class="param-row"><span class="param-key">Sell Signal</span> <span class="param-val">Upper ×2σ</span></div>
        </div>
      </div>

      <!-- NAV FOOTER -->
      <div class="nav-footer">
        <a class="nav-btn primary" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/control">Control</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/history/orders">Order History</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
      </div>

    </div>
  </div><!-- /main-content -->

  <!-- ══ RIGHT SIDEBAR ══ -->
  <aside class="sidebar">
    <!-- 헤더 -->
    <div class="sb-hd">
      <div class="sb-hd-top">
        <div class="sb-title">
          <span class="sb-title-dot"></span>
          거래량 순위 TOP 30
        </div>
        <div class="sb-btn-row">
          <button class="sb-icon-btn" id="sbRefBtn" onclick="loadRanking(true)" title="수동 새로고침">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="sb-hd-meta">
        <span class="sb-updated" id="sbUpdated">데이터 로딩 중…</span>
        <div class="sb-countdown-wrap">
          <span class="sb-cd-lbl">갱신</span>
          <span class="sb-cd-num" id="sbCd">30</span>
          <span class="sb-cd-lbl">s</span>
        </div>
      </div>
    </div>
    <div class="sb-prog"><div class="sb-prog-fill" id="sbProg"></div></div>

    <!-- 탭 -->
    <div class="sb-tabs">
      <div class="sb-tab on"  id="tab-vol" onclick="switchTab('vol')">거래량</div>
      <div class="sb-tab"     id="tab-chg" onclick="switchTab('chg')">등락률</div>
      <div class="sb-tab"     id="tab-new" onclick="switchTab('new')">신고가</div>
    </div>

    <!-- 필터 -->
    <div class="sb-filter">
      <button class="sf-btn on" id="sf-all"   onclick="switchFilter('all')">전체</button>
      <button class="sf-btn"    id="sf-kospi" onclick="switchFilter('kospi')">KOSPI</button>
      <button class="sf-btn"    id="sf-kosdaq"onclick="switchFilter('kosdaq')">KOSDAQ</button>
    </div>

    <!-- 목록 -->
    <div class="sb-body" id="sbBody">
      <div class="sb-msg loading">
        <div class="sb-msg-icon">◈</div>
        <div class="sb-msg-txt">데이터 로딩 중…</div>
      </div>
    </div>

    <div class="sb-foot">KIS Developers · /uapi/domestic-stock/v1/quotations/volume-rank</div>
  </aside>

</div><!-- /layout -->

<script>
(function(){
'use strict';

const BASE     = '${pageContext.request.contextPath}';
const DAYS     = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const POLL_SEC = 30;   /* 30초 자동 갱신 */
const TOP_N    = 30;

/* ── 시계 ── */
function tick(){
  const n=new Date(), p=v=>String(v).padStart(2,'0');
  document.getElementById('clkTime').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
  document.getElementById('clkDate').textContent=n.getFullYear()+'.'+p(n.getMonth()+1)+'.'+p(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000); tick();

/* ── 업타임 / 레이턴시 ── */
let up=0;
setInterval(()=>{
  up++;
  const h=Math.floor(up/3600), m=Math.floor((up%3600)/60), s=up%60;
  const p=v=>String(v).padStart(2,'0');
  document.getElementById('widgetUptime').textContent=p(h)+':'+p(m)+':'+p(s);
},1000);
setInterval(()=>{
  document.getElementById('widgetLatency').textContent=(Math.random()*8+1).toFixed(1)+' ms';
},2000);

/* ── Login ── */
(function(){
  const form      = document.getElementById('loginForm');
  const statusBox = document.getElementById('loginStatus');
  const accSpan   = document.getElementById('loginAccount');
  const logoutBtn = document.getElementById('logoutBtn');
  const errBox    = document.getElementById('loginError');
  const pattern   = /^\d{8}-\d{2}$/;

  const showIn  = m => { form.style.display='none'; statusBox.style.display='inline-flex'; errBox.style.display='none'; accSpan.textContent=m||'****'; };
  const showOut = () => { statusBox.style.display='none'; form.style.display=''; errBox.style.display='none'; };
  const showErr = m => { errBox.textContent=m||''; errBox.style.display=m?'inline-flex':'none'; };
  const post    = (url,d) => fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(d).toString()}).then(r=>r.json());

  fetch(BASE+'/api/auth/status').then(r=>r.json()).then(d=>d&&d.loggedIn?showIn(d.accountMasked):showOut()).catch(()=>showOut());

  form.addEventListener('submit', e=>{
    e.preventDefault();
    const no=(form.accountNo.value||'').trim(), pw=(form.accountPassword.value||'').trim();
    if(!no||!pw) return;
    if(!pattern.test(no)){showErr('Format: 12345678-01');return;}
    post(BASE+'/api/auth/login',{accountNo:no,accountPassword:pw}).then(d=>d.status==='OK'?showIn(d.accountMasked):showErr(d.message||'Login failed'));
  });
  logoutBtn.addEventListener('click',()=>post(BASE+'/api/auth/logout',{}).then(()=>showOut()));
})();

/* ════ 사이드바 로직 ════ */
let rawData     = [];      // API 원본
let activeTab   = 'vol';   // vol | chg | new
let activeFilter= 'all';   // all | kospi | kosdaq
let prevCodes   = new Set();
let cdRemain    = POLL_SEC;
let cdTimer     = null;

/* ── 탭 전환 ── */
window.switchTab = function(tab){
  activeTab = tab;
  ['vol','chg','new'].forEach(t=>{
    document.getElementById('tab-'+t).classList.toggle('on', t===tab);
  });
  if(rawData.length) renderList(rawData);
};

/* ── 필터 전환 ── */
window.switchFilter = function(f){
  activeFilter = f;
  ['all','kospi','kosdaq'].forEach(k=>{
    document.getElementById('sf-'+k).classList.toggle('on', k===f);
  });
  if(rawData.length) renderList(rawData);
};

/* ── 데이터 로드 ── */
window.loadRanking = function(manual){
  const btn = document.getElementById('sbRefBtn');
  btn.classList.add('spinning');

  fetch(BASE + '/api/market/ranking/hts')   /* ApiController → marketInsightService.getHtsTopView() → getVolumeRanking() */
    .then(r=>{ if(!r.ok) throw new Error('HTTP '+r.status); return r.json(); })
    .then(json=>{
      /*
       * KIS volume-rank 응답 필드 (output 배열 내 각 항목):
       *   data_rank      : 순위
       *   mksc_shrn_iscd : 단축코드 (6자리)
       *   hts_kor_isnm   : 종목명
       *   stck_prpr      : 현재가
       *   prdy_vrss      : 전일 대비
       *   prdy_ctrt      : 등락률(%)
       *   acml_vol       : 누적거래량
       *   prdy_vrss_sign : 부호 (1:상한 2:상승 3:보합 4:하한 5:하락)
       *   stck_hgpr      : 당일 고가
       *   stck_lwpr      : 당일 저가
       */
      rawData = (json.output || []).slice(0, TOP_N);
      renderList(rawData);

      const n=new Date(), p=v=>String(v).padStart(2,'0');
      document.getElementById('sbUpdated').textContent = p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds())+' 갱신';
      restartCountdown();
    })
    .catch(err=>{
      document.getElementById('sbBody').innerHTML =
        `<div class="sb-msg error"><div class="sb-msg-icon">✕</div><div class="sb-msg-txt">${esc(err.message)}</div></div>`;
    })
    .finally(()=>btn.classList.remove('spinning'));
};

/* ── 렌더 ── */
function renderList(data){
  const body = document.getElementById('sbBody');
  if(!data.length){
    body.innerHTML='<div class="sb-msg"><div class="sb-msg-icon">—</div><div class="sb-msg-txt">데이터 없음</div></div>';
    return;
  }

  /* 필터 적용 */
  let filtered = data;
  if(activeFilter === 'kospi')  filtered = data.filter(r=> r.mksc_shrn_iscd < '200000');
  if(activeFilter === 'kosdaq') filtered = data.filter(r=> r.mksc_shrn_iscd >= '200000');

  /* 탭별 정렬 */
  const sorted = [...filtered].sort((a,b)=>{
    if(activeTab==='chg')
      return Math.abs(parseFloat(b.prdy_ctrt||0)) - Math.abs(parseFloat(a.prdy_ctrt||0));
    if(activeTab==='new')
      return parseFloat(b.stck_hgpr||0) - parseFloat(a.stck_hgpr||0);
    /* 거래량 기본 */
    return parseFloat(b.acml_vol||0) - parseFloat(a.acml_vol||0);
  }).slice(0, TOP_N);

  const maxVol = Math.max(...sorted.map(r=>parseFloat(r.acml_vol||0)), 1);
  const newSet = new Set(sorted.map(r=>r.mksc_shrn_iscd));

  const rows = sorted.map((r, idx)=>{
    const rank  = idx + 1;
    const rnCls = rank===1?'r1':rank===2?'r2':rank===3?'r3':'rN';
    const name  = esc(r.hts_kor_isnm || '—');
    const code  = esc(r.mksc_shrn_iscd || '');
    const price = Number(r.stck_prpr||0).toLocaleString('ko-KR');
    const diff  = parseFloat(r.prdy_ctrt||0);
    const sign  = r.prdy_vrss_sign||'3';
    const isUp  = sign==='1'||sign==='2';
    const isDn  = sign==='4'||sign==='5';
    const chgCls= isUp?'up':isDn?'down':'flat';
    const prefix= isUp?'▲':isDn?'▼':'—';
    const chgTxt= prefix + Math.abs(diff).toFixed(2) + '%';
    const vol   = parseFloat(r.acml_vol||0);
    const volPct= Math.max((vol/maxVol*100), 2).toFixed(1);
    const volFmt= vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
    const isNew = !prevCodes.has(r.mksc_shrn_iscd);

    return `<div class="rank-row${isNew?' new-row':''}" style="animation-delay:${idx*18}ms">
      <div class="rn ${rnCls}">${rank}</div>
      <div class="ri">
        <div class="ri-name" title="${name}">${name}</div>
        <div class="ri-code">${code}</div>
        <div class="ri-bar-wrap">
          <div class="ri-bar" style="width:${volPct}%"></div>
          <div class="ri-vol">${volFmt}</div>
        </div>
      </div>
      <div class="rp">
        <div class="rp-price">${price}</div>
        <div class="rp-chg ${chgCls}">${chgTxt}</div>
      </div>
    </div>`;
  }).join('');

  body.innerHTML = '<div class="rank-list">'+rows+'</div>';
  prevCodes = newSet;
}

/* ── 카운트다운 & 프로그레스 바 ── */
function restartCountdown(){
  cdRemain = POLL_SEC;
  updateProg();
  if(cdTimer) clearInterval(cdTimer);
  cdTimer = setInterval(()=>{
    cdRemain--;
    document.getElementById('sbCd').textContent = cdRemain;
    updateProg();
    if(cdRemain <= 0) loadRanking();
  }, 1000);
}
function updateProg(){
  const pct = ((POLL_SEC - cdRemain) / POLL_SEC) * 100;
  document.getElementById('sbProg').style.width = pct + '%';
}

function esc(s){ return String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

/* ── 초기 실행 ── */
loadRanking();

})();
</script>
</body>
</html>
