<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Monitor — AUTO TRADING</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#060709;--base:#0a0c10;--panel:#141720;--panel-hi:#191d28;--hover:#1e2330;
  --lime:#a8ff3e;--lime-d:rgba(168,255,62,.1);--lime-b:rgba(168,255,62,.22);--lime-glow:0 0 16px rgba(168,255,62,.4);
  --emerald:#00d97e;--emerald-d:rgba(0,217,126,.08);--emerald-b:rgba(0,217,126,.25);
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --purple:#b07fff;--purple-d:rgba(176,127,255,.08);--purple-b:rgba(176,127,255,.22);
  --rim:rgba(255,255,255,.07);--rim-hi:rgba(255,255,255,.14);
  /* 가독성 개선: t2/t3 밝게 */
  --t1:#e8edf5;--t2:#9baabb;--t3:#6b7a99;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:12px;--topbar-h:52px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{background:var(--void);min-height:100%;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.05) 0%,transparent 55%),
    radial-gradient(ellipse 40% 60% at 100% 110%,rgba(0,217,126,.04) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.032) 1px,transparent 1px);
  background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{opacity:1;transform:scale(1);}50%{opacity:.25;transform:scale(.6);}}
@keyframes spin{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes bar{from{width:0;}to{width:var(--bw,0%);}}
@keyframes pulse-border{0%,100%{border-color:rgba(168,255,62,.35);}50%{border-color:rgba(168,255,62,.7);}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.96);
  backdrop-filter:blur(20px);border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime) 40%,rgba(168,255,62,.2) 70%,transparent);
  opacity:.4;}
.tb-logo{display:flex;align-items:center;gap:9px;padding:0 16px;height:100%;
  border-right:1px solid var(--rim);min-width:180px;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;
  display:flex;align-items:center;justify-content:center;
  box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
.tb-a{font-family:var(--mono);font-size:9px;letter-spacing:.4px;padding:4px 9px;
  border-radius:var(--r);border:1px solid transparent;background:transparent;
  color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.tb-fx{padding:0 14px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:2px;min-width:90px;}
.fx-rate{font-family:var(--mono);font-size:13px;font-weight:600;letter-spacing:.5px;color:var(--gold);}
.fx-meta{display:flex;align-items:center;gap:4px;}
.fx-lbl{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.2px;}
.fx-chg{font-family:var(--mono);font-size:8px;letter-spacing:.3px;}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── PAGE ── */
.page{position:relative;z-index:1;padding:12px 14px;
  display:flex;flex-direction:column;gap:10px;
  min-height:calc(100vh - var(--topbar-h));}

/* ── TOOLBAR ── */
.toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:9px 12px;animation:fu .35s .04s ease both;}
.seg{display:flex;gap:3px;}
.seg-btn{height:28px;padding:0 12px;font-family:var(--mono);font-size:9px;letter-spacing:.8px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;
  color:var(--t3);cursor:pointer;transition:all .12s;}
.seg-btn.on-kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.seg-btn.on-us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.month-in{height:28px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;outline:none;cursor:pointer;}
.month-in:focus{border-color:var(--lime-b);}
.ref-btn{height:28px;padding:0 11px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--lime-b);border-radius:var(--r);
  background:var(--lime-d);color:var(--lime);cursor:pointer;
  transition:all .12s;display:inline-flex;align-items:center;gap:5px;}
.ref-btn:hover{background:var(--lime);color:var(--void);}
.ref-btn svg{width:10px;height:10px;}
.ref-btn.spinning svg{animation:spin .6s linear infinite;}
.auto-wrap{display:flex;align-items:center;gap:6px;}
.auto-lbl{font-family:var(--mono);font-size:8px;color:var(--t3);}
.auto-btn{height:28px;padding:0 10px;font-family:var(--mono);font-size:8px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;
  color:var(--t3);cursor:pointer;transition:all .12s;}
.auto-btn.on{background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.cd-ring{width:20px;height:20px;flex-shrink:0;}
.cd-ring circle{fill:none;stroke-width:2;}
.cd-bg{stroke:var(--t4);}
.cd-fg{stroke:var(--lime);stroke-linecap:round;
  transform:rotate(-90deg);transform-origin:50% 50%;
  transition:stroke-dashoffset .9s linear;}
.status-chip{margin-left:auto;display:flex;align-items:center;gap:5px;
  font-family:var(--mono);font-size:8px;color:var(--t3);}
.status-dot{width:5px;height:5px;border-radius:50%;background:var(--t3);flex-shrink:0;}
.status-dot.live{background:var(--emerald);animation:pd 1.4s ease-in-out infinite;}
.status-dot.err{background:var(--red);}

/* ── TODAY HERO ── */
.today-hero{display:grid;grid-template-columns:1fr 1fr;gap:10px;animation:fu .35s .06s ease both;}
@media(max-width:720px){.today-hero{grid-template-columns:1fr;}}

.hero-pnl{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:18px 20px;position:relative;overflow:hidden;}
.hero-pnl.pos-card{border-color:rgba(0,217,126,.3);animation:none;}
.hero-pnl.neg-card{border-color:rgba(255,77,106,.25);animation:none;}
.hero-pnl::before{content:'';position:absolute;top:0;left:0;right:0;height:3px;
  background:var(--hero-acc,var(--lime));}
.hero-pnl::after{content:'';position:absolute;inset:0;pointer-events:none;
  background:radial-gradient(ellipse 70% 50% at 50% 0%,var(--hero-soft,transparent),transparent);
  opacity:.4;}
.hero-tag{font-family:var(--mono);font-size:9px;letter-spacing:1.5px;text-transform:uppercase;
  color:var(--t2);margin-bottom:6px;display:flex;align-items:center;gap:6px;}
.hero-tag-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.hero-amount{font-family:var(--mono);font-size:36px;font-weight:700;
  line-height:1;letter-spacing:-1px;position:relative;z-index:1;}
.hero-meta{display:flex;align-items:center;gap:14px;margin-top:10px;position:relative;z-index:1;}
.hero-meta-item{display:flex;flex-direction:column;gap:2px;}
.hero-meta-lbl{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:.8px;}
.hero-meta-val{font-family:var(--mono);font-size:12px;font-weight:600;color:var(--t2);}

/* ── FLOW SECTION ── */
.flow-section{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:16px 18px;}
.flow-item{display:flex;flex-direction:column;gap:5px;padding:0 6px;}
.flow-item+.flow-item{border-left:1px solid var(--rim);}
.flow-lbl{font-family:var(--mono);font-size:8px;letter-spacing:1.2px;text-transform:uppercase;color:var(--t3);}
.flow-val{font-family:var(--mono);font-size:18px;font-weight:600;line-height:1;}
.flow-sub{font-family:var(--mono);font-size:9px;color:var(--t2);}

/* ── KPI GRID ── */
.kpi-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;
  animation:fu .35s .10s ease both;}
@media(max-width:900px){.kpi-grid{grid-template-columns:repeat(2,1fr);}}
@media(max-width:500px){.kpi-grid{grid-template-columns:1fr;}}

.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:14px 14px 12px;position:relative;overflow:hidden;transition:border-color .2s;}
.kpi:hover{border-color:var(--rim-hi);}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;
  background:var(--acc,transparent);border-radius:var(--r3) var(--r3) 0 0;}
.kpi::after{content:'';position:absolute;inset:0;pointer-events:none;
  background:radial-gradient(ellipse 60% 40% at 50% 0%,var(--acc-soft,transparent),transparent);
  opacity:.35;}
.kpi-label{font-family:var(--mono);font-size:8px;color:var(--t2);
  letter-spacing:1.2px;text-transform:uppercase;margin-bottom:8px;
  display:flex;align-items:center;gap:5px;}
.kpi-dot{width:4px;height:4px;border-radius:50%;flex-shrink:0;}
.kpi-val{font-family:var(--mono);font-size:24px;font-weight:600;
  line-height:1;letter-spacing:-.5px;position:relative;z-index:1;}
.kpi-sub{font-family:var(--mono);font-size:9px;color:var(--t2);margin-top:5px;
  position:relative;z-index:1;}

/* ── 월간 요약 ── */
.month-summary{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;
  animation:fu .35s .12s ease both;}
.ms-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:14px 16px;display:flex;flex-direction:column;gap:7px;}
.ms-label{font-family:var(--mono);font-size:8px;color:var(--t2);
  letter-spacing:1.2px;text-transform:uppercase;}
.ms-val{font-family:var(--mono);font-size:20px;font-weight:600;line-height:1;}
.ms-bar-wrap{height:4px;background:var(--t4);border-radius:2px;overflow:hidden;}
.ms-bar{height:100%;border-radius:2px;animation:bar .8s ease both;}
.ms-sub{font-family:var(--mono);font-size:9px;color:var(--t2);}

/* ── CALENDAR PANEL ── */
.cal-panel{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  overflow:hidden;animation:fu .35s .16s ease both;}
.pn-hd{display:flex;align-items:center;justify-content:space-between;
  padding:0 14px;height:40px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.pn-hd-l{display:flex;align-items:center;gap:7px;}
.pn-dot{width:5px;height:5px;border-radius:50%;}
.pn-title{font-family:var(--mono);font-size:9px;font-weight:500;color:var(--t1);
  letter-spacing:1.2px;text-transform:uppercase;}
.pn-badge{font-family:var(--mono);font-size:8px;padding:3px 9px;border-radius:5px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.pn-bd{padding:14px;}

.week-hdr{display:grid;grid-template-columns:repeat(7,1fr);gap:6px;margin-bottom:6px;}
.week-hdr span{text-align:center;font-family:var(--mono);font-size:8px;
  color:var(--t2);letter-spacing:.5px;padding:3px 0;}
.week-hdr span:first-child{color:var(--red);}
.week-hdr span:last-child{color:var(--blue);}

.cal-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:6px;}

.cal-day{min-height:86px;border:1px solid var(--rim);border-radius:var(--r2);
  padding:9px 10px;background:var(--base);
  display:flex;flex-direction:column;gap:3px;
  transition:border-color .15s,background .15s,transform .15s;
  cursor:default;position:relative;overflow:hidden;}
.cal-day::before{content:'';position:absolute;bottom:0;left:0;right:0;height:3px;
  background:var(--day-bar,transparent);opacity:.7;}
.cal-day:not(.empty):hover{border-color:var(--rim-hi);background:var(--hover);transform:translateY(-1px);}
.cal-day.empty{background:transparent;border-style:dashed;opacity:.15;pointer-events:none;}
.cal-day.today{border-color:rgba(168,255,62,.45);background:rgba(168,255,62,.04);}
.cal-day.today::after{content:'TODAY';position:absolute;top:6px;right:7px;
  font-family:var(--mono);font-size:6px;letter-spacing:1px;color:var(--lime);opacity:.8;}
.cal-day.pos{border-color:rgba(0,217,126,.25);background:rgba(0,217,126,.04);}
.cal-day.neg{border-color:rgba(255,77,106,.2);background:rgba(255,77,106,.04);}
.cal-day.pos::before{background:var(--emerald);}
.cal-day.neg::before{background:var(--red);}
.cal-day.today::before{background:var(--lime);}

.day-num{font-family:var(--mono);font-size:10px;color:var(--t2);line-height:1;}
.cal-day.today .day-num{color:var(--lime);font-weight:700;}

.day-val{font-family:var(--mono);font-size:13px;font-weight:700;
  margin-top:auto;line-height:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.day-val.pos{color:var(--emerald);}
.day-val.neg{color:var(--red);}
.day-val.zero{color:var(--t3);font-size:10px;font-weight:400;}

.day-meta{display:flex;align-items:center;justify-content:space-between;margin-top:2px;}
.day-dot{width:4px;height:4px;border-radius:50%;}

/* 주별 합계 */
.cal-week-sum{display:grid;grid-template-columns:repeat(7,1fr);gap:6px;margin-top:6px;}
.wsum{text-align:center;font-family:var(--mono);font-size:9px;color:var(--t3);padding:3px 0;}
.wsum.pos{color:var(--emerald);}
.wsum.neg{color:var(--red);}

/* 범례 */
.cal-legend{display:flex;align-items:center;gap:12px;margin-top:12px;
  font-family:var(--mono);font-size:9px;color:var(--t2);}
.leg-item{display:flex;align-items:center;gap:5px;}
.leg-chip{width:12px;height:12px;border-radius:3px;flex-shrink:0;}
.leg-chip.pos{background:rgba(0,217,126,.35);border:1px solid var(--emerald-b);}
.leg-chip.neg{background:rgba(255,77,106,.3);border:1px solid var(--red-b);}
.leg-chip.today{background:rgba(168,255,62,.2);border:1px solid var(--lime-b);}
.leg-sep{flex:1;}
.cal-total-row{display:flex;align-items:center;gap:8px;}
.cal-total-lbl{font-family:var(--mono);font-size:9px;color:var(--t2);}
.cal-total-val{font-family:var(--mono);font-size:13px;font-weight:700;}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mk">
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
  <div class="tb-sp"></div>
  <div class="tb-nav">
    <a class="tb-a"     href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-fx" id="tbFx">
    <div class="fx-rate" id="fxRate">—</div>
    <div class="fx-meta">
      <span class="fx-lbl">USD/KRW</span>
      <span class="fx-chg" id="fxChg"></span>
    </div>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- ── TOOLBAR ── -->
  <div class="toolbar">
    <div class="seg">
      <button class="seg-btn on-kr" id="btnKR" onclick="setMarket('KR')">🇰🇷 KR</button>
      <button class="seg-btn"       id="btnUS" onclick="setMarket('US')">🇺🇸 US</button>
    </div>
    <input class="month-in" id="monthInput" type="month">
    <button class="ref-btn" id="refBtn" onclick="refreshAll()">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
           stroke-linecap="round" stroke-linejoin="round">
        <polyline points="23 4 23 10 17 10"/>
        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
      </svg>
      Refresh
    </button>
    <div class="auto-wrap">
      <span class="auto-lbl">Auto</span>
      <button class="auto-btn on" id="autoBtn" onclick="toggleAuto()">60s</button>
      <svg class="cd-ring" viewBox="0 0 20 20">
        <circle class="cd-bg" cx="10" cy="10" r="8"/>
        <circle class="cd-fg" id="cdRing" cx="10" cy="10" r="8"
          stroke-dasharray="50.3" stroke-dashoffset="0"/>
      </svg>
    </div>
    <div class="status-chip">
      <div class="status-dot" id="statusDot"></div>
      <span id="statusTxt">Ready</span>
    </div>
  </div>

  <!-- ── TODAY HERO: 오늘 실현손익 + 매수/매도 흐름 ── -->
  <div class="today-hero">
    <!-- 오늘 실현손익 -->
    <div class="hero-pnl" id="heroPnlCard"
         style="--hero-acc:var(--emerald);--hero-soft:rgba(0,217,126,.06);">
      <div class="hero-tag">
        <div class="hero-tag-dot" style="background:var(--emerald);box-shadow:0 0 6px rgba(0,217,126,.5);"></div>
        오늘 실현 손익
      </div>
      <div class="hero-amount" id="heroRealizedPnl" style="color:var(--emerald);">—</div>
      <div class="hero-meta">
        <div class="hero-meta-item">
          <div class="hero-meta-lbl">미실현 평가손익</div>
          <div class="hero-meta-val" id="heroUnrealized">—</div>
        </div>
        <div class="hero-meta-item">
          <div class="hero-meta-lbl">수익률</div>
          <div class="hero-meta-val" id="heroProfitRate">—</div>
        </div>
        <div class="hero-meta-item">
          <div class="hero-meta-lbl">보유 종목</div>
          <div class="hero-meta-val" id="heroHolding">—</div>
        </div>
      </div>
    </div>

    <!-- 오늘 매매 흐름 -->
    <div class="flow-section">
      <div class="flow-item">
        <div class="flow-lbl">매수 체결</div>
        <div class="flow-val" id="flowBuy" style="color:var(--blue);">—</div>
        <div class="flow-sub" id="flowBuySub">당일 총 매수</div>
      </div>
      <div class="flow-item">
        <div class="flow-lbl">매도 체결</div>
        <div class="flow-val" id="flowSell" style="color:var(--gold);">—</div>
        <div class="flow-sub" id="flowSellSub">당일 총 매도</div>
      </div>
      <div class="flow-item">
        <div class="flow-lbl">순 현금 흐름</div>
        <div class="flow-val" id="flowNet" style="color:var(--t1);">—</div>
        <div class="flow-sub">매도 − 매수</div>
      </div>
    </div>
  </div>

  <!-- ── KPI: 포트폴리오 전체 지표 ── -->
  <div class="kpi-grid">
    <div class="kpi" style="--acc:var(--lime);--acc-soft:rgba(168,255,62,.05);">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--lime)"></div>Total Eval</div>
      <div class="kpi-val" id="kEval" style="color:var(--lime)">—</div>
      <div class="kpi-sub">보유 포지션 평가액</div>
    </div>
    <div class="kpi" id="kpiProfitCard" style="--acc:var(--gold);--acc-soft:rgba(245,200,66,.05);">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--gold)"></div>Total P&amp;L</div>
      <div class="kpi-val" id="kProfit" style="color:var(--gold)">—</div>
      <div class="kpi-sub" id="kProfitRate">—</div>
    </div>
    <div class="kpi" style="--acc:var(--purple);--acc-soft:rgba(176,127,255,.05);">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--purple)"></div>Holding</div>
      <div class="kpi-val" id="kHolding" style="color:var(--purple)">—</div>
      <div class="kpi-sub">보유 종목 수</div>
    </div>
    <div class="kpi" style="--acc:var(--emerald);--acc-soft:rgba(0,217,126,.04);">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--emerald)"></div>Running</div>
      <div class="kpi-val" id="kRunning" style="color:var(--emerald)">—</div>
      <div class="kpi-sub">전략 실행 중</div>
    </div>
  </div>

  <!-- ── 월간 요약 ── -->
  <div class="month-summary">
    <div class="ms-card">
      <div class="ms-label">월간 누적 손익</div>
      <div class="ms-val" id="msTotal" style="color:var(--emerald)">—</div>
      <div class="ms-bar-wrap">
        <div class="ms-bar" id="msTotalBar" style="background:var(--emerald);--bw:0%;width:0%;"></div>
      </div>
      <div class="ms-sub" id="msTotalSub">—</div>
    </div>
    <div class="ms-card">
      <div class="ms-label">수익일 / 손실일</div>
      <div class="ms-val" id="msWinLoss" style="color:var(--t1)">—</div>
      <div class="ms-bar-wrap">
        <div class="ms-bar" id="msWinBar" style="background:var(--emerald);--bw:0%;width:0%;"></div>
      </div>
      <div class="ms-sub" id="msWinRate">—</div>
    </div>
    <div class="ms-card">
      <div class="ms-label">일평균 손익</div>
      <div class="ms-val" id="msAvg" style="color:var(--t1)">—</div>
      <div class="ms-bar-wrap">
        <div class="ms-bar" id="msAvgBar" style="--bw:0%;width:0%;"></div>
      </div>
      <div class="ms-sub">거래일 기준</div>
    </div>
  </div>

  <!-- ── CALENDAR ── -->
  <div class="cal-panel">
    <div class="pn-hd">
      <div class="pn-hd-l">
        <div class="pn-dot" style="background:var(--lime);box-shadow:0 0 8px rgba(168,255,62,.5);"></div>
        <span class="pn-title">Daily Realized PnL</span>
        <span class="pn-badge" id="calBadge">—</span>
      </div>
      <span class="pn-badge" id="calMonthBadge"
            style="color:var(--lime);border-color:var(--lime-b);background:var(--lime-d);">—</span>
    </div>
    <div class="pn-bd">
      <div class="week-hdr">
        <span>SUN</span><span>MON</span><span>TUE</span>
        <span>WED</span><span>THU</span><span>FRI</span><span>SAT</span>
      </div>
      <div class="cal-grid" id="calGrid"></div>
      <div class="cal-week-sum" id="weekSums"></div>
      <div class="cal-legend">
        <div class="leg-item"><div class="leg-chip pos"></div><span>수익</span></div>
        <div class="leg-item"><div class="leg-chip neg"></div><span>손실</span></div>
        <div class="leg-item"><div class="leg-chip today"></div><span>오늘</span></div>
        <div class="leg-sep"></div>
        <div class="cal-total-row">
          <span class="cal-total-lbl">월 합계</span>
          <span class="cal-total-val" id="calTotalVal">—</span>
        </div>
      </div>
    </div>
  </div>

</div><!-- /page -->

<script>
'use strict';
(function(){
  const BASE = '${pageContext.request.contextPath}';
  const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const nfKR = new Intl.NumberFormat('ko-KR');
  const pfKR = new Intl.NumberFormat('ko-KR',{minimumFractionDigits:2,maximumFractionDigits:2});

  let market   = 'KR';
  let autoOn   = true;
  let autoSec  = 60;
  let autoTimer= null;
  const PERIOD = 60;
  const CIRC   = 50.3;

  /* ── 시계 ── */
  function p2(v){return String(v).padStart(2,'0');}
  (function tick(){
    const n=new Date();
    document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
    document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
    setTimeout(tick,1000);
  })();

  /* ── 상태 ── */
  function setStatus(txt,state){
    document.getElementById('statusTxt').textContent=txt;
    const d=document.getElementById('statusDot');
    d.className='status-dot'+(state==='live'?' live':state==='err'?' err':'');
  }

  /* ── 마켓 ── */
  window.setMarket=function(m){
    market=m==='US'?'US':'KR';
    document.getElementById('btnKR').className='seg-btn'+(market==='KR'?' on-kr':'');
    document.getElementById('btnUS').className='seg-btn'+(market==='US'?' on-us':'');
    refreshAll();
  };

  /* ── 자동 새로고침 ── */
  function startAuto(){
    clearInterval(autoTimer);
    autoSec=PERIOD;
    updateRing();
    autoTimer=setInterval(()=>{
      autoSec--;
      updateRing();
      if(autoSec<=0){autoSec=PERIOD;refreshAll();}
    },1000);
  }
  function stopAuto(){
    clearInterval(autoTimer);
    autoTimer=null;
    document.getElementById('cdRing').style.strokeDashoffset='0';
  }
  function updateRing(){
    const pct=autoSec/PERIOD;
    document.getElementById('cdRing').style.strokeDashoffset=String(CIRC*(1-pct));
    document.getElementById('autoBtn').textContent=autoSec+'s';
  }
  window.toggleAuto=function(){
    autoOn=!autoOn;
    document.getElementById('autoBtn').className='auto-btn'+(autoOn?' on':'');
    autoOn?startAuto():stopAuto();
  };

  /* ── 포맷 ── */
  function fmtMoney(v){
    const n=Number(v);
    if(isNaN(n)||v==null) return '—';
    if(market==='US'){
      if(Math.abs(n)>=1e6) return '$'+(n/1e6).toFixed(2)+'M';
      if(Math.abs(n)>=1e3) return '$'+(n/1e3).toFixed(1)+'K';
      return '$'+pfKR.format(n);
    }
    if(Math.abs(n)>=1e8) return (n/1e8).toFixed(2)+'억';
    if(Math.abs(n)>=1e4) return (n/1e4).toFixed(1)+'만';
    return nfKR.format(Math.round(n));
  }
  function fmtSigned(v){
    const n=Number(v);
    if(isNaN(n)||v==null) return '—';
    const prefix=n>=0?'+':'';
    if(market==='US'){
      const abs=fmtMoney(Math.abs(n));
      return n>=0?'+'+abs:'-'+abs;
    }
    return prefix+fmtMoney(n);
  }
  function colorOf(v){
    const n=Number(v);
    if(isNaN(n)||n===0) return 'var(--t1)';
    return n>0?'var(--emerald)':'var(--red)';
  }

  /* ── Summary 렌더 ── */
  function renderSummary(s){
    if(!s) return;

    /* ── 오늘 실현손익 Hero ── */
    const realized=s.todayRealizedProfitAmount??s.todayRealizedProfit??s.todayProfit??null;
    const rn=Number(realized??0);
    const heroEl=document.getElementById('heroRealizedPnl');
    heroEl.textContent=fmtSigned(realized);
    heroEl.style.color=colorOf(rn);

    const card=document.getElementById('heroPnlCard');
    if(rn>0){
      card.className='hero-pnl pos-card';
      card.style.setProperty('--hero-acc','var(--emerald)');
      card.style.setProperty('--hero-soft','rgba(0,217,126,.06)');
    } else if(rn<0){
      card.className='hero-pnl neg-card';
      card.style.setProperty('--hero-acc','var(--red)');
      card.style.setProperty('--hero-soft','rgba(255,77,106,.05)');
    }

    /* 미실현 평가손익 */
    const profit=s.totalProfitAmount??s.profitAmount??null;
    document.getElementById('heroUnrealized').textContent=fmtSigned(profit);
    document.getElementById('heroUnrealized').style.color=colorOf(Number(profit??0));

    /* 수익률 */
    const rate=s.totalProfitRate??s.profitRate??null;
    document.getElementById('heroProfitRate').textContent=
      rate!=null?(Number(rate)>=0?'+':'')+pfKR.format(Number(rate))+'%':'—';
    document.getElementById('heroProfitRate').style.color=colorOf(Number(rate??0));

    /* 보유 종목 */
    document.getElementById('heroHolding').textContent=
      nfKR.format(Number(s.holdingCount??s.holding??0))+'종목';

    /* ── 매매 흐름 ── */
    const buyA=s.todayBuyAmount??s.buyAmount??0;
    const sellA=s.todaySellAmount??s.sellAmount??0;
    const netA=Number(sellA)-Number(buyA);

    document.getElementById('flowBuy').textContent=fmtMoney(buyA);
    document.getElementById('flowSell').textContent=fmtMoney(sellA);
    const flowNetEl=document.getElementById('flowNet');
    flowNetEl.textContent=fmtSigned(netA);
    flowNetEl.style.color=colorOf(netA);

    /* ── KPI ── */
    document.getElementById('kEval').textContent=
      fmtMoney(s.totalEvaluationAmount??s.eval??0);

    const kProfitEl=document.getElementById('kProfit');
    kProfitEl.textContent=fmtSigned(profit);
    kProfitEl.style.color=colorOf(Number(profit??0));
    const kProfitCard=document.getElementById('kpiProfitCard');
    const pn=Number(profit??0);
    if(pn!==0){
      kProfitCard.style.setProperty('--acc',pn>0?'var(--emerald)':'var(--red)');
      kProfitCard.style.setProperty('--acc-soft',pn>0?'rgba(0,217,126,.06)':'rgba(255,77,106,.05)');
      kProfitEl.style.color=colorOf(pn);
    }
    document.getElementById('kProfitRate').textContent=
      rate!=null?'수익률 '+(Number(rate)>=0?'+':'')+pfKR.format(Number(rate))+'%':'—';

    document.getElementById('kHolding').textContent=
      nfKR.format(Number(s.holdingCount??s.holding??0));
    document.getElementById('kRunning').textContent=
      nfKR.format(Number(s.runningStrategyCount??s.running??0));
  }

  /* ── Calendar 렌더 ── */
  function renderCalendar(payload){
    if(!payload) return;
    const year =Number(payload.year ||new Date().getFullYear());
    const month=Number(payload.month||new Date().getMonth()+1);
    const points=Array.isArray(payload.data)?payload.data:[];

    const map=new Map();
    points.forEach(p=>{
      if(!p) return;
      let raw=String(p.tradeDate??p.date??p.day??'');
      if(raw.length===8&&!raw.includes('-'))
        raw=raw.slice(0,4)+'-'+raw.slice(4,6)+'-'+raw.slice(6);
      if(!raw) return;
      const amt=Number(p.profitAmount??p.profit??p.pnl??0);
      map.set(raw,(map.get(raw)||0)+amt);
    });

    const monthKey=year+'-'+p2(month);
    const today=new Date();
    const todayKey=today.getFullYear()+'-'+p2(today.getMonth()+1)+'-'+p2(today.getDate());

    let totalPnl=0, winDays=0, lossDays=0, tradeDays=0;
    map.forEach(v=>{
      totalPnl+=v; tradeDays++;
      if(v>0) winDays++;
      else if(v<0) lossDays++;
    });
    const avgPnl=tradeDays>0?totalPnl/tradeDays:0;
    const winRate=tradeDays>0?(winDays/tradeDays*100):0;

    /* 월간 요약 카드 */
    const msTotalEl=document.getElementById('msTotal');
    msTotalEl.textContent=fmtSigned(totalPnl);
    msTotalEl.style.color=totalPnl>=0?'var(--emerald)':'var(--red)';
    document.getElementById('msTotalBar').style.cssText=
      'background:'+(totalPnl>=0?'var(--emerald)':'var(--red)')+';--bw:100%;width:100%;animation:bar .8s ease both;';
    document.getElementById('msTotalSub').textContent=tradeDays+'일 거래';

    document.getElementById('msWinLoss').textContent=winDays+'승 '+lossDays+'패';
    document.getElementById('msWinBar').style.cssText=
      'background:var(--emerald);--bw:'+winRate.toFixed(0)+'%;width:'+winRate.toFixed(0)+'%;animation:bar .8s ease both;';
    document.getElementById('msWinRate').textContent='승률 '+winRate.toFixed(1)+'%';

    const avgEl=document.getElementById('msAvg');
    avgEl.textContent=fmtSigned(avgPnl);
    avgEl.style.color=avgPnl>=0?'var(--emerald)':'var(--red)';
    document.getElementById('msAvgBar').style.cssText=
      'background:'+(avgPnl>=0?'var(--emerald)':'var(--red)')+';--bw:60%;width:60%;animation:bar .8s ease both;';

    /* 달력 배지 */
    document.getElementById('calBadge').textContent=market+' · '+tradeDays+'일';
    document.getElementById('calMonthBadge').textContent=monthKey;
    const ctv=document.getElementById('calTotalVal');
    ctv.textContent=fmtSigned(totalPnl);
    ctv.style.color=totalPnl>=0?'var(--emerald)':totalPnl<0?'var(--red)':'var(--t3)';

    const firstDay=new Date(year,month-1,1).getDay();
    const lastDate=new Date(year,month,0).getDate();

    let cells='';
    const weekTotals=[];
    let wkSum=0;

    for(let i=0;i<firstDay;i++) cells+='<div class="cal-day empty"></div>';

    for(let d=1;d<=lastDate;d++){
      const key=monthKey+'-'+p2(d);
      const isToday=key===todayKey;
      const profit=map.has(key)?map.get(key):null;
      const dow=(firstDay+d-1)%7;

      let cls='cal-day';
      if(isToday) cls+=' today';
      else if(profit!=null&&profit>0) cls+=' pos';
      else if(profit!=null&&profit<0) cls+=' neg';

      let valHtml='<div class="day-val zero">—</div>';
      if(profit!=null){
        const vc=profit>0?'pos':profit<0?'neg':'zero';
        valHtml='<div class="day-val '+vc+'">'+fmtSigned(profit)+'</div>';
      }

      const dotHtml=profit!=null
        ?'<div class="day-dot" style="background:'+(profit>=0?'var(--emerald)':'var(--red)')+'"></div>'
        :'';

      cells+='<div class="'+cls+'" style="'+(profit!=null?'--day-bar:'+(profit>=0?'var(--emerald)':'var(--red)')+';':'')+'">'
        +'<div class="day-num">'+d+'</div>'
        +valHtml
        +'<div class="day-meta">'+dotHtml+'</div>'
        +'</div>';

      if(profit!=null) wkSum+=profit;
      if(dow===6||d===lastDate){ weekTotals.push(wkSum); wkSum=0; }
    }
    document.getElementById('calGrid').innerHTML=cells;

    let wkHtml='';
    let weekIdx=0;
    for(let d=1;d<=lastDate;d++){
      const dow=(firstDay+d-1)%7;
      if(dow===6||d===lastDate){
        const wt=weekTotals[weekIdx++]||0;
        wkHtml+='<div class="wsum'+(wt>0?' pos':wt<0?' neg':'')+'">'+(wt!==0?fmtSigned(wt):'—')+'</div>';
        if(dow!==6&&d===lastDate)
          for(let fill=dow+1;fill<7;fill++) wkHtml+='<div class="wsum"></div>';
      }
    }
    let weekSumsHtml='';
    for(let i=0;i<firstDay;i++) weekSumsHtml+='<div class="wsum"></div>';
    document.getElementById('weekSums').innerHTML=weekSumsHtml+wkHtml;
  }

  function parseYM(val){
    if(!val||val.length!==7){const n=new Date();return{year:n.getFullYear(),month:n.getMonth()+1};}
    const p=val.split('-');return{year:Number(p[0]),month:Number(p[1])};
  }

  /* ── 환율 로드 ── */
  function loadExchangeRate(){
    fetch(BASE+'/api/monitor/exchange-rate')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d||d.status!=='OK') return;
        const rate=Number(d.rate||0);
        const chg=Number(d.change||0);
        document.getElementById('fxRate').textContent=
          rate>0?new Intl.NumberFormat('ko-KR',{minimumFractionDigits:2,maximumFractionDigits:2}).format(rate)+'₩':'—';
        const chgEl=document.getElementById('fxChg');
        if(chg!==0){
          const sign=chg>=0?'+':'';
          chgEl.textContent=sign+chg.toFixed(2)+'%';
          chgEl.style.color=chg>=0?'var(--emerald)':'var(--red)';
        } else {
          chgEl.textContent='';
        }
      })
      .catch(()=>{});
  }

  /* ── 데이터 로드 ── */
  window.refreshAll=function(){
    setStatus('로딩 중…','');
    const btn=document.getElementById('refBtn');
    btn.classList.add('spinning');

    loadExchangeRate();
    const ym=parseYM(document.getElementById('monthInput').value);
    Promise.all([
      fetch(BASE+'/api/monitor/summary?market='+encodeURIComponent(market))
        .then(r=>{if(!r.ok)throw new Error('summary '+r.status);return r.json();}),
      fetch(BASE+'/api/monitor/calendar?market='+encodeURIComponent(market)
        +'&year='+ym.year+'&month='+ym.month)
        .then(r=>{if(!r.ok)throw new Error('calendar '+r.status);return r.json();})
    ]).then(([sum,cal])=>{
      renderSummary(sum);
      renderCalendar(cal);
      setStatus('갱신 '+new Date().toLocaleTimeString('ko-KR'),'live');
    }).catch(e=>{
      console.error(e);
      setStatus('로드 실패: '+e.message,'err');
    }).finally(()=>btn.classList.remove('spinning'));
  };

  /* ── 초기화 ── */
  const n=new Date();
  document.getElementById('monthInput').value=n.getFullYear()+'-'+p2(n.getMonth()+1);
  document.getElementById('monthInput').addEventListener('change',refreshAll);
  refreshAll();
  startAuto();
})();
</script>
</body>
</html>