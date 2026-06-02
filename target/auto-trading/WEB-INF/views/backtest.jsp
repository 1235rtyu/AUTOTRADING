<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>Backtest · AUTOTRADE TERMINAL</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#060709;--base:#0a0c10;--surface:#0f1117;--panel:#141720;--panel-hi:#191d28;--hover:#1e2330;
  --lime:#a8ff3e;--lime-d:rgba(168,255,62,.1);--lime-b:rgba(168,255,62,.22);--lime-glow:0 0 20px rgba(168,255,62,.4);
  --emerald:#00d97e;--emerald-d:rgba(0,217,126,.08);--emerald-b:rgba(0,217,126,.25);
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --purple:#b07fff;--purple-d:rgba(176,127,255,.08);--purple-b:rgba(176,127,255,.22);
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--topbar-h:56px;--sidebar-w:390px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);overflow-x:hidden;}
.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 90% 60% at 50% -10%,rgba(168,255,62,.07) 0%,transparent 55%),
             radial-gradient(ellipse 50% 70% at 100% 80%,rgba(0,217,126,.04) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.04) 1px,transparent 1px);background-size:28px 28px;}

/* TOPBAR */
.topbar{position:fixed;top:0;left:0;right:0;z-index:200;height:var(--topbar-h);
  display:flex;align-items:center;
  background:rgba(6,7,9,.94);backdrop-filter:blur(14px);border-bottom:1px solid var(--rim);}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime),rgba(168,255,62,.3),transparent);opacity:.5;}
.tb-logo{display:flex;align-items:center;gap:11px;padding:0 22px;height:100%;
  border-right:1px solid var(--rim);min-width:210px;text-decoration:none;}
.logo-mark{width:34px;height:34px;background:var(--lime);border-radius:8px;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;box-shadow:var(--lime-glow);}
.logo-mark svg{width:18px;height:18px;}
.logo-name{font-size:14px;font-weight:800;letter-spacing:.5px;color:var(--t1);}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-spacer{flex:1;}
.tb-nav{display:flex;align-items:center;gap:4px;padding:0 18px;}
.tb-nav-link{font-family:var(--mono);font-size:10px;letter-spacing:.5px;padding:5px 12px;
  border-radius:var(--r);border:1px solid transparent;background:transparent;color:var(--t2);
  cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-nav-link:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-nav-link.active{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-clock{padding:0 18px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:2px;}
.clock-t{font-family:var(--mono);font-size:15px;font-weight:500;letter-spacing:2px;}
.clock-d{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1px;}

/* MAIN LAYOUT */
.bt-wrap{display:flex;padding-top:var(--topbar-h);min-height:100vh;position:relative;z-index:1;}

/* SIDEBAR */
.bt-sidebar{width:var(--sidebar-w);min-width:var(--sidebar-w);
  position:sticky;top:var(--topbar-h);height:calc(100vh - var(--topbar-h));
  overflow-y:auto;overflow-x:hidden;
  background:var(--panel);border-right:1px solid var(--rim);
  display:flex;flex-direction:column;}
.bt-sidebar::-webkit-scrollbar{width:4px;}
.bt-sidebar::-webkit-scrollbar-track{background:transparent;}
.bt-sidebar::-webkit-scrollbar-thumb{background:var(--t4);border-radius:2px;}
.sidebar-body{padding:20px;flex:1;}
.sidebar-footer{padding:16px 20px;border-top:1px solid var(--rim);background:var(--panel);}

/* CONTENT AREA */
.bt-content{flex:1;min-width:0;padding:28px 28px 72px;}

/* FORM ELEMENTS */
.f-label{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:1px;
  text-transform:uppercase;margin-bottom:5px;display:block;}
.f-input{width:100%;height:32px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:11px;
  padding:0 10px;outline:none;transition:border-color .15s;}
.f-input:focus{border-color:var(--lime-b);}
.f-group{margin-bottom:12px;}

/* MARKET TABS */
.mkt-tabs{display:flex;gap:6px;margin-bottom:14px;}
.mkt-tab{flex:1;height:36px;border:1px solid var(--rim-hi);border-radius:var(--r2);
  background:transparent;color:var(--t2);font-family:var(--mono);font-size:11px;
  cursor:pointer;transition:all .15s;font-weight:600;letter-spacing:.5px;}
.mkt-tab.active{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);box-shadow:var(--lime-glow);}

/* SYMBOL SEARCH */
.sym-wrap{position:relative;}
.sym-input-row{display:flex;gap:6px;align-items:center;}
.sym-input-row .f-input{flex:1;}
.sym-loading{width:14px;height:14px;border:2px solid var(--t4);border-top-color:var(--lime);
  border-radius:50%;animation:spin .6s linear infinite;flex-shrink:0;display:none;}
@keyframes spin{to{transform:rotate(360deg);}}
.sym-dropdown{position:absolute;top:calc(100% + 2px);left:0;right:0;z-index:300;
  background:var(--panel-hi);border:1px solid var(--rim-hi);border-radius:var(--r2);
  overflow:hidden;display:none;box-shadow:0 8px 24px rgba(0,0,0,.5);}
.sym-item{display:flex;align-items:center;gap:8px;padding:9px 12px;cursor:pointer;
  border-bottom:1px solid var(--rim);transition:background .1s;}
.sym-item:last-child{border-bottom:none;}
.sym-item:hover{background:var(--hover);}
.sym-ticker{font-family:var(--mono);font-size:11px;font-weight:600;color:var(--lime);min-width:56px;}
.sym-name{font-family:var(--mono);font-size:10px;color:var(--t2);flex:1;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.sym-exch{font-family:var(--mono);font-size:9px;color:var(--t3);flex-shrink:0;}
.sym-hint{padding:8px 12px;font-family:var(--mono);font-size:10px;color:var(--t3);text-align:center;}

/* PERIOD CHIPS */
.period-chips{display:flex;gap:5px;flex-wrap:wrap;margin-bottom:10px;}
.chip{padding:4px 10px;border:1px solid var(--rim-hi);border-radius:20px;background:transparent;
  color:var(--t2);font-family:var(--mono);font-size:10px;cursor:pointer;transition:all .15s;}
.chip:hover,.chip.active{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}

/* DATE ROW */
.date-pair{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:12px;}
.date-pair .f-group{margin-bottom:0;}

/* SECTION DIVIDER */
.s-divider{margin:16px 0 10px;padding-bottom:8px;border-bottom:1px solid var(--rim);
  display:flex;align-items:center;gap:8px;}
.s-divider-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;}
.s-divider-label{font-family:var(--mono);font-size:9px;font-weight:700;letter-spacing:2px;text-transform:uppercase;}

/* PARAM GRID */
.p-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:4px;}
.p-grid .f-group{margin-bottom:0;}
.p-grid .f-input{font-size:10px;height:28px;}

/* BUTTONS */
.btn{width:100%;height:36px;border-radius:var(--r);border:1px solid;font-family:var(--mono);
  font-size:11px;font-weight:600;letter-spacing:.5px;cursor:pointer;transition:all .15s;}
.btn:disabled{opacity:.35;cursor:not-allowed;}
.btn-collect{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);margin-bottom:7px;}
.btn-collect:hover:not(:disabled){background:var(--blue);color:var(--void);}
.btn-run{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.btn-run:hover:not(:disabled){background:var(--lime);color:var(--void);box-shadow:var(--lime-glow);}
.btn-reset{height:26px;padding:0;border:1px solid var(--rim-hi);border-radius:var(--r);
  background:transparent;color:var(--t3);font-family:var(--mono);font-size:9px;
  cursor:pointer;transition:all .15s;letter-spacing:.5px;width:auto;padding:0 10px;margin-bottom:12px;}
.btn-reset:hover{border-color:var(--rim-hi);color:var(--t2);}

/* PROGRESS */
.progress-wrap{margin-top:10px;display:none;}
.pb-outer{height:3px;background:var(--t4);border-radius:2px;overflow:hidden;margin-bottom:5px;}
.pb-inner{height:100%;background:var(--blue);border-radius:2px;transition:width .3s;}
.pb-msg{font-family:var(--mono);font-size:9px;color:var(--t2);}

/* ── RESULTS ── */
.results-header{margin-bottom:20px;}
.results-meta{font-family:var(--mono);font-size:10px;color:var(--t3);margin-bottom:16px;
  padding:8px 12px;background:var(--panel);border:1px solid var(--rim);border-radius:var(--r);}
.results-meta span{color:var(--t2);}

/* KPI GRID */
.kpi-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;}
.kpi-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:16px 18px;position:relative;overflow:hidden;transition:border-color .15s;}
.kpi-card::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;}
.kpi-card.lime::before{background:var(--lime);}
.kpi-card.emerald::before{background:var(--emerald);}
.kpi-card.red::before{background:var(--red);}
.kpi-card.gold::before{background:var(--gold);}
.kpi-card.blue::before{background:var(--blue);}
.kpi-label{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:1.5px;
  text-transform:uppercase;margin-bottom:8px;}
.kpi-val{font-family:var(--mono);font-size:22px;font-weight:600;line-height:1;margin-bottom:4px;}
.kpi-val.pos{color:var(--emerald);}
.kpi-val.neg{color:var(--red);}
.kpi-val.neu{color:var(--t1);}
.kpi-val.gold-c{color:var(--gold);}
.kpi-sub{font-family:var(--mono);font-size:10px;color:var(--t3);}
.kpi-sub b{color:var(--t2);}

/* EQUITY CHART */
.chart-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:16px;margin-bottom:20px;}
.chart-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.chart-title{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:2px;text-transform:uppercase;}
.chart-stat{font-family:var(--mono);font-size:11px;}
canvas#equityChart{width:100%;height:110px;display:block;border-radius:var(--r);}

/* STAT TABLES */
.stat-row{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px;}
.stat-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);padding:14px 16px;}
.stat-title{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:2px;
  text-transform:uppercase;margin-bottom:10px;padding-bottom:6px;border-bottom:1px solid var(--rim);}
.tbl{width:100%;border-collapse:collapse;font-family:var(--mono);font-size:11px;}
.tbl th{color:var(--t2);font-size:9px;letter-spacing:.8px;text-transform:uppercase;
  padding:5px 8px;border-bottom:1px solid var(--rim);font-weight:400;}
.tbl th.r,.tbl td.r{text-align:right;}
.tbl td{padding:6px 8px;border-bottom:1px solid rgba(255,255,255,.025);}
.tbl tr:last-child td{border-bottom:none;}
.tbl tr:hover td{background:var(--hover);}
.tbl td.pos{color:var(--emerald);}
.tbl td.neg{color:var(--red);}

/* BADGES */
.mode-badge{display:inline-block;padding:2px 7px;border-radius:10px;font-size:9px;font-weight:700;letter-spacing:.5px;}
.mb-pullback{background:rgba(77,159,255,.15);color:var(--blue);}
.mb-breakout{background:rgba(168,255,62,.12);color:var(--lime);}
.mb-volume{background:rgba(245,200,66,.12);color:var(--gold);}
.mb-early{background:rgba(176,127,255,.12);color:var(--purple);}
.mb-unknown{background:var(--t4);color:var(--t2);}
.exit-badge{display:inline-block;padding:2px 7px;border-radius:10px;font-size:9px;font-weight:600;}
.eb-tp{background:rgba(0,217,126,.12);color:var(--emerald);}
.eb-sl{background:rgba(255,77,106,.12);color:var(--red);}
.eb-trail{background:rgba(168,255,62,.1);color:var(--lime);}
.eb-time{background:rgba(245,200,66,.1);color:var(--gold);}
.eb-other{background:var(--t4);color:var(--t2);}

/* TRADE TABLE */
.trades-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);padding:16px;}
.trades-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.trades-title{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:2px;text-transform:uppercase;}
.trades-count{font-family:var(--mono);font-size:10px;color:var(--t3);}
.trade-scroll{overflow-x:auto;}

/* PAGINATION */
.pagination{display:flex;align-items:center;gap:6px;margin-top:12px;font-family:var(--mono);font-size:10px;}
.pg-btn{height:26px;padding:0 10px;border:1px solid var(--rim-hi);border-radius:var(--r);
  background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;}
.pg-btn:hover:not(:disabled){border-color:var(--lime-b);color:var(--lime);}
.pg-btn:disabled{opacity:.3;cursor:not-allowed;}
.pg-info{color:var(--t2);padding:0 6px;}

/* EMPTY STATE */
.empty-state{display:flex;flex-direction:column;align-items:center;justify-content:center;
  min-height:500px;color:var(--t3);}
.empty-icon{font-size:48px;margin-bottom:16px;opacity:.6;}
.empty-title{font-family:var(--mono);font-size:13px;color:var(--t2);margin-bottom:6px;}
.empty-sub{font-family:var(--mono);font-size:10px;color:var(--t3);text-align:center;max-width:300px;line-height:1.7;}

/* TOAST */
.toast{position:fixed;bottom:24px;right:24px;z-index:9999;padding:10px 18px;
  border-radius:var(--r);font-family:var(--mono);font-size:11px;animation:fadeUp .2s ease;}
.toast.ok{background:#1a2e15;border:1px solid var(--emerald);color:var(--emerald);}
.toast.err{background:#2a1118;border:1px solid var(--red);color:var(--red);}
@keyframes fadeUp{from{opacity:0;transform:translateY(8px);}to{opacity:1;transform:none;}}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<!-- TOPBAR -->
<nav class="topbar">
  <a class="tb-logo" href="<%= request.getContextPath() %>/">
    <div class="logo-mark">
      <svg viewBox="0 0 24 24" fill="none" stroke="#060709" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-name">AUTO<span>TRADE</span></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </a>
  <div class="tb-spacer"></div>
  <div class="tb-nav">
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/control/kr">Control·KR</a>
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/control/us">Control·US</a>
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/history/orders">Orders</a>
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/balances">Balances</a>
    <a class="tb-nav-link" href="<%= request.getContextPath() %>/watchlist">Watchlist</a>
    <a class="tb-nav-link active" href="<%= request.getContextPath() %>/backtest">Backtest</a>
  </div>
  <div class="tb-clock">
    <div class="clock-t" id="clkTime">--:--:--</div>
    <div class="clock-d" id="clkDate">----.--.--</div>
  </div>
</nav>

<div class="bt-wrap">

  <!-- ── LEFT SIDEBAR ── -->
  <aside class="bt-sidebar">
    <div class="sidebar-body">

      <!-- Market -->
      <div class="mkt-tabs">
        <button class="mkt-tab active" id="mktKrx" onclick="setMarket('KRX')">🇰🇷 국장 KRX</button>
        <button class="mkt-tab"        id="mktUs"  onclick="setMarket('US')">🇺🇸 미장 US</button>
      </div>
      <input type="hidden" id="marketVal" value="KRX">

      <!-- Symbol -->
      <div class="f-group">
        <label class="f-label" id="symLabel">종목코드</label>
        <div class="sym-wrap">
          <div class="sym-input-row">
            <input type="text" class="f-input" id="symbolInput"
                   placeholder="예) 005930"
                   oninput="this.value=this.value.toUpperCase();onSymbolInput()"
                   autocomplete="off">
            <div class="sym-loading" id="symLoading"></div>
          </div>
          <div class="sym-dropdown" id="symDropdown"></div>
        </div>
      </div>

      <!-- Period chips -->
      <div class="f-group">
        <label class="f-label">기간</label>
        <div class="period-chips">
          <span class="chip" onclick="setPeriod(7)">1주</span>
          <span class="chip" onclick="setPeriod(14)">2주</span>
          <span class="chip" onclick="setPeriod(30)">1개월</span>
          <span class="chip" onclick="setPeriod(90)">3개월</span>
        </div>
        <div style="margin-top:6px;font-family:var(--mono);font-size:9px;color:var(--t3);line-height:1.5;">
          미장 1분봉은 Yahoo Finance 제한으로 최근 약 29일만 수집됩니다.
        </div>
        <div class="date-pair">
          <div class="f-group">
            <label class="f-label">시작</label>
            <input type="date" class="f-input" id="startDate">
          </div>
          <div class="f-group">
            <label class="f-label">종료</label>
            <input type="date" class="f-input" id="endDate">
          </div>
        </div>
      </div>

      <!-- Amount -->
      <div class="f-group">
        <label class="f-label" id="amtLabel">주문금액 (원)</label>
        <input type="number" class="f-input" id="buyAmount" value="600000" step="100000" min="10000">
      </div>

      <!-- ── 진입 조건 ── -->
      <div class="s-divider">
        <div class="s-divider-dot" style="background:var(--lime);"></div>
        <span class="s-divider-label" style="color:var(--lime);">진입 조건</span>
      </div>
      <div class="p-grid">
        <div class="f-group">
          <label class="f-label">PB Score ≥</label>
          <input type="number" class="f-input" id="p_pullbackMinScore" value="72" step="1" min="0" max="100">
        </div>
        <div class="f-group">
          <label class="f-label">BO Score ≥</label>
          <input type="number" class="f-input" id="p_breakoutMinScore" value="78" step="1" min="0" max="100">
        </div>
        <div class="f-group">
          <label class="f-label">VWAP Gap BO %</label>
          <input type="number" class="f-input" id="p_vwapMaxGapBreakoutPct" value="2.2" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">VWAP Gap PB %</label>
          <input type="number" class="f-input" id="p_vwapMaxGapPullbackPct" value="1.0" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">PB Upper %</label>
          <input type="number" class="f-input" id="p_pullbackUpperPct" value="1.5" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">PB Lower %</label>
          <input type="number" class="f-input" id="p_pullbackLowerPct" value="2.5" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Volume Mult ×</label>
          <input type="number" class="f-input" id="p_volumeMult" value="1.5" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label" id="toKrxLbl">Turnover KRX (원)</label>
          <input type="number" class="f-input" id="p_minTurnoverKrx" value="50000000" step="1000000" min="0">
        </div>
        <div class="f-group" style="grid-column:span 2;">
          <label class="f-label">Turnover US (USD)</label>
          <input type="number" class="f-input" id="p_minTurnoverUs" value="10000" step="1000" min="0">
        </div>
      </div>

      <!-- ── 청산 조건 ── -->
      <div class="s-divider">
        <div class="s-divider-dot" style="background:var(--gold);"></div>
        <span class="s-divider-label" style="color:var(--gold);">청산 조건</span>
      </div>
      <div class="p-grid">
        <div class="f-group">
          <label class="f-label">Stop Loss %</label>
          <input type="number" class="f-input" id="p_stopLossPct" value="1.3" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Take Profit %</label>
          <input type="number" class="f-input" id="p_takeProfitPct" value="2.8" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Trail Start %</label>
          <input type="number" class="f-input" id="p_trailStartPct" value="2.3" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Trail Drop %</label>
          <input type="number" class="f-input" id="p_trailDropPct" value="1.5" step="0.1" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">VWAP Grace (초)</label>
          <input type="number" class="f-input" id="p_vwapBreakGraceSec" value="360" step="60" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Soft Stop (초)</label>
          <input type="number" class="f-input" id="p_softTimeStopSec" value="1200" step="60" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Mid Stop (초)</label>
          <input type="number" class="f-input" id="p_midTimeStopSec" value="2400" step="60" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Hard Stop (초)</label>
          <input type="number" class="f-input" id="p_hardTimeStopSec" value="5400" step="60" min="0">
        </div>
      </div>

      <!-- ── 운영 조건 ── -->
      <div class="s-divider">
        <div class="s-divider-dot" style="background:var(--blue);"></div>
        <span class="s-divider-label" style="color:var(--blue);">운영 조건</span>
      </div>
      <div class="p-grid">
        <div class="f-group">
          <label class="f-label">Max Daily Entry</label>
          <input type="number" class="f-input" id="p_maxDailyEntryCount" value="2" step="1" min="1">
        </div>
        <div class="f-group">
          <label class="f-label">Max Same Pattern</label>
          <input type="number" class="f-input" id="p_maxSamePatternEntry" value="1" step="1" min="1">
        </div>
        <div class="f-group">
          <label class="f-label">Slippage %</label>
          <input type="number" class="f-input" id="p_slippagePct" value="0.0" step="0.01" min="0">
        </div>
        <div class="f-group">
          <label class="f-label">Fee % (RT)</label>
          <input type="number" class="f-input" id="p_feePct" value="0.015" step="0.001" min="0">
        </div>
      </div>

      <button class="btn-reset" onclick="resetParams()">↺ 기본값 초기화</button>

    </div><!-- sidebar-body -->

    <div class="sidebar-footer">
      <button class="btn btn-collect" id="btnCollect" onclick="collectBars()">▼ 분봉 데이터 가져오기</button>
      <button class="btn btn-run"     id="btnRun"     onclick="runBacktest()">▶ 백테스트 실행</button>
      <div class="progress-wrap" id="progressWrap">
        <div class="pb-outer"><div class="pb-inner" id="progressBar" style="width:0%"></div></div>
        <div class="pb-msg" id="progressMsg"></div>
      </div>
    </div>
  </aside>

  <!-- ── RIGHT CONTENT ── -->
  <div class="bt-content" id="btContent">
    <div class="empty-state">
      <div class="empty-icon">📊</div>
      <div class="empty-title">백테스트를 실행하세요</div>
      <div class="empty-sub">좌측에서 시장·종목·기간·전략 파라미터를 설정하고<br>분봉 수집 후 백테스트를 실행하면 결과가 여기 표시됩니다.</div>
    </div>
  </div>

</div><!-- bt-wrap -->

<script>
const ctx = '<%= request.getContextPath() %>';
let pollTimer = null;

/* ── Clock ── */
function tickClock() {
  const now = new Date();
  document.getElementById('clkTime').textContent =
    now.toLocaleTimeString('ko-KR',{hour12:false,hour:'2-digit',minute:'2-digit',second:'2-digit'});
  document.getElementById('clkDate').textContent =
    now.toLocaleDateString('ko-KR',{year:'numeric',month:'2-digit',day:'2-digit'}).replace(/\. /g,'-').replace('.','-');
}
tickClock(); setInterval(tickClock, 1000);

/* ── Market ── */
function setMarket(mkt) {
  document.getElementById('marketVal').value = mkt;
  document.getElementById('mktKrx').classList.toggle('active', mkt === 'KRX');
  document.getElementById('mktUs').classList.toggle('active',  mkt === 'US');
  document.getElementById('symbolInput').placeholder = mkt === 'KRX' ? '예) 005930' : '예) TSLA';
  document.getElementById('amtLabel').textContent  = mkt === 'KRX' ? '주문금액 (원)' : '주문금액 (USD)';
  hideDropdown();
}

/* ── Period ── */
function setPeriod(days) {
  document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
  const end   = new Date();
  const start = new Date(); start.setDate(start.getDate() - days);
  document.getElementById('endDate').value   = fmtDate(end);
  document.getElementById('startDate').value = fmtDate(start);
}
function fmtDate(d) { return d.toISOString().slice(0, 10); }

(function(){
  const today = new Date(), month = new Date();
  month.setMonth(month.getMonth() - 1);
  document.getElementById('endDate').value   = fmtDate(today);
  document.getElementById('startDate').value = fmtDate(month);
})();

/* ── Symbol Autocomplete ── */
let searchTimer = null;
function onSymbolInput() {
  const q   = document.getElementById('symbolInput').value.trim();
  const mkt = document.getElementById('marketVal').value;
  clearTimeout(searchTimer);
  hideDropdown();
  if (q.length < 1 || mkt !== 'US') return;
  document.getElementById('symLoading').style.display = 'block';
  searchTimer = setTimeout(() => fetchSuggestions(q), 300);
}
function fetchSuggestions(q) {
  fetch(ctx + '/backtest/searchSymbol?q=' + encodeURIComponent(q) + '&market=US')
    .then(r => r.json())
    .then(items => {
      document.getElementById('symLoading').style.display = 'none';
      showDropdown(items);
    })
    .catch(() => { document.getElementById('symLoading').style.display = 'none'; });
}
function showDropdown(items) {
  const dd = document.getElementById('symDropdown');
  if (!items || !items.length) {
    dd.innerHTML = '<div class="sym-hint">결과 없음</div>';
    dd.style.display = 'block';
    return;
  }
  dd.innerHTML = items.map(item =>
    '<div class="sym-item" onclick="selectSymbol(\'' + item.symbol + '\')">' +
    '<span class="sym-ticker">' + item.symbol + '</span>' +
    '<span class="sym-name">' + (item.name || '') + '</span>' +
    '<span class="sym-exch">' + (item.exchange || '') + '</span>' +
    '</div>'
  ).join('');
  dd.style.display = 'block';
}
function hideDropdown() { document.getElementById('symDropdown').style.display = 'none'; }
function selectSymbol(sym) {
  document.getElementById('symbolInput').value = sym;
  hideDropdown();
}
document.addEventListener('click', function(e) {
  if (!e.target.closest('.sym-wrap')) hideDropdown();
});

/* ── Params ── */
const PARAM_DEFAULTS = {
  pullbackMinScore:72, breakoutMinScore:78,
  vwapMaxGapBreakoutPct:2.2, vwapMaxGapPullbackPct:1.0,
  pullbackUpperPct:1.5, pullbackLowerPct:2.5,
  volumeMult:1.5, minTurnoverKrx:50000000, minTurnoverUs:10000,
  stopLossPct:1.3, takeProfitPct:2.8, trailStartPct:2.3, trailDropPct:1.5,
  vwapBreakGraceSec:360, softTimeStopSec:1200, midTimeStopSec:2400, hardTimeStopSec:5400,
  maxDailyEntryCount:2, maxSamePatternEntry:1,
  slippagePct:0.0, feePct:0.015
};
function resetParams() {
  Object.entries(PARAM_DEFAULTS).forEach(function(kv) {
    var el = document.getElementById('p_' + kv[0]);
    if (el) el.value = kv[1];
  });
  toast('파라미터 초기화 완료', 'ok');
}
function collectParamValues() {
  var p = {};
  Object.keys(PARAM_DEFAULTS).forEach(function(k) {
    var el = document.getElementById('p_' + k);
    if (el) p[k] = el.value;
  });
  return p;
}

/* ── Build request ── */
function buildReq() {
  const symbol = document.getElementById('symbolInput').value.trim();
  const start  = document.getElementById('startDate').value;
  const end    = document.getElementById('endDate').value;
  const market = document.getElementById('marketVal').value;
  const buyAmt = parseFloat(document.getElementById('buyAmount').value) || 600000;
  if (!symbol) { toast('종목코드를 입력하세요.', 'err'); return null; }
  if (!start || !end) { toast('기간을 선택하세요.', 'err'); return null; }
  return Object.assign({ market, symbol, startDate: start, endDate: end, buyAmount: String(buyAmt) }, collectParamValues());
}

/* ── Collect ── */
function collectBars() {
  const req = buildReq();
  if (!req) return;
  setButtons(true);
  showProgress(0, '수집 잡 시작 중...');
  fetch(ctx + '/backtest/collectBars', {
    method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(req)
  })
  .then(r => r.json())
  .then(d => {
    if (d.status !== 'STARTED') { toast(d.message || '오류', 'err'); setButtons(false); hideProgress(); return; }
    pollCollect(d.jobId);
  })
  .catch(e => { toast('요청 실패: ' + e.message, 'err'); setButtons(false); hideProgress(); });
}
function pollCollect(jobId) {
  clearInterval(pollTimer);
  pollTimer = setInterval(function() {
    fetch(ctx + '/backtest/collectStatus/' + jobId)
      .then(r => r.json())
      .then(s => {
        showProgress(s.progress || 0, s.message || '');
        if (s.state === 'DONE' || s.state === 'ERROR') {
          clearInterval(pollTimer); setButtons(false);
          if (s.state === 'DONE') { toast((s.inserted || 0) + '개 봉 수집 완료', 'ok'); hideProgress(); }
          else { toast('수집 오류: ' + (s.message || ''), 'err'); hideProgress(); }
        }
      });
  }, 2000);
}

/* ── Backtest ── */
function runBacktest() {
  const req = buildReq();
  if (!req) return;
  setButtons(true);
  document.getElementById('btContent').innerHTML =
    '<div class="empty-state"><div class="empty-icon">⏳</div>' +
    '<div class="empty-title">백테스트 실행 중...</div>' +
    '<div class="empty-sub">전략 엔진이 모든 분봉을 시뮬레이션하고 있습니다.</div></div>';

  fetch(ctx + '/backtest/run', {
    method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(req)
  })
  .then(r => r.json())
  .then(d => {
    setButtons(false);
    if (d.status === 'ERROR') { toast(d.message, 'err'); renderEmpty(d.message); return; }
    renderResult(d);
  })
  .catch(e => { setButtons(false); toast('요청 실패: ' + e.message, 'err'); renderEmpty(e.message); });
}

/* ── Render Result ── */
var allTrades = [], tradePage = 1;
const PAGE_SIZE = 25;

function renderResult(d) {
  allTrades = d.trades || [];
  tradePage = 1;
  document.getElementById('btContent').innerHTML =
    renderMeta(d) + renderWarnings(d.warnings || []) + renderKpis(d) + renderChart() +
    renderStatRow(d.modeStats || [], d.exitStats || [], d.rejectReasonSummary || []) +
    renderTradesCard();
  drawEquityChart(allTrades);
  renderTradeList();
}

function renderWarnings(items) {
  if (!items.length) return '';
  return '<div style="margin-bottom:12px;padding:10px 12px;border:1px solid rgba(255,185,56,.35);' +
    'background:rgba(255,185,56,.07);color:var(--gold);font-family:var(--mono);font-size:10px;line-height:1.6;">' +
    items.map(function(item) { return '· ' + item; }).join('<br>') + '</div>';
}

function renderMeta(d) {
  const mkt = d.market === 'KRX' ? '🇰🇷 KRX' : '🇺🇸 US';
  const amt = d.market === 'KRX'
    ? Number(d.buyAmount).toLocaleString() + '원'
    : '$' + Number(d.buyAmount).toLocaleString();
  return '<div class="results-meta">' + mkt + ' · <span>' + d.symbol + '</span>' +
    ' · ' + d.startDate + ' ~ ' + d.endDate +
    ' · <span>' + Number(d.totalBars).toLocaleString() + '봉</span>' +
    ' · 주문 <span>' + amt + '</span></div>';
}

function renderKpis(d) {
  const wr  = (d.winRate * 100).toFixed(1);
  const avg = (d.avgPnlPct * 100).toFixed(2);
  const cum = (d.cumulativePnlPct * 100).toFixed(2);
  const dd  = (d.maxDrawdown * 100).toFixed(2);
  const pf  = d.profitFactor >= 9998 ? '∞' : d.profitFactor.toFixed(2);
  const exp = (d.expectancy * 100).toFixed(2);
  const wrCls  = d.winRate >= 0.5 ? 'pos' : 'neg';
  const cumCls = d.cumulativePnlPct >= 0 ? 'pos' : 'neg';
  const avgCls = d.avgPnlPct >= 0 ? 'pos' : 'neg';
  return '<div class="kpi-grid">' +
    kpiCard('lime', '총 거래수', d.totalTrades + '<span style="font-size:13px;color:var(--t2)"> 건</span>', 'neu',
      '<b>' + d.wins + '</b>W / <b>' + (d.totalTrades - d.wins) + '</b>L') +
    kpiCard(d.winRate >= 0.5 ? 'emerald' : 'red', '승률', wr + '%', wrCls,
      '기대값 <b style="color:' + (d.expectancy >= 0 ? 'var(--emerald)' : 'var(--red)') + '">' + exp + '%</b>') +
    kpiCard(d.avgPnlPct >= 0 ? 'emerald' : 'red', '평균 수익률', avg + '%', avgCls,
      '누적 <b class="' + cumCls + '">' + cum + '%</b>') +
    kpiCard('gold', 'Max Drawdown', '-' + dd + '%', 'neg',
      'Profit Factor <b style="color:var(--gold)">' + pf + '</b>') +
    '</div>';
}

function kpiCard(color, label, value, valueCls, sub) {
  return '<div class="kpi-card ' + color + '">' +
    '<div class="kpi-label">' + label + '</div>' +
    '<div class="kpi-val ' + valueCls + '">' + value + '</div>' +
    '<div class="kpi-sub">' + sub + '</div>' +
    '</div>';
}

function renderChart() {
  return '<div class="chart-card"><div class="chart-head">' +
    '<span class="chart-title">손익 곡선 (누적 수익)</span>' +
    '<span class="chart-stat" id="chartStat"></span></div>' +
    '<canvas id="equityChart"></canvas></div>';
}

function drawEquityChart(trades) {
  const canvas = document.getElementById('equityChart');
  if (!canvas || !trades.length) return;

  const W = canvas.offsetWidth;
  const H = 110;
  canvas.width  = W * window.devicePixelRatio;
  canvas.height = H * window.devicePixelRatio;
  canvas.style.width  = W + 'px';
  canvas.style.height = H + 'px';

  const c = canvas.getContext('2d');
  c.scale(window.devicePixelRatio, window.devicePixelRatio);

  // Build equity array
  const eq = [1.0];
  trades.forEach(function(t) { eq.push(eq[eq.length-1] * (1 + t.pnlPct)); });

  const minEq = Math.min.apply(null, eq);
  const maxEq = Math.max.apply(null, eq);
  const range  = maxEq - minEq || 0.001;
  const pad = 8;

  function xOf(i) { return (i / (eq.length - 1)) * (W - pad*2) + pad; }
  function yOf(v) { return H - pad - ((v - minEq) / range) * (H - pad*2); }

  const isUp = eq[eq.length-1] >= 1.0;
  const lineColor = isUp ? '#a8ff3e' : '#ff4d6a';
  const fillColor0 = isUp ? 'rgba(168,255,62,0.18)' : 'rgba(255,77,106,0.18)';
  const fillColor1 = 'rgba(0,0,0,0)';

  // Fill gradient
  const grad = c.createLinearGradient(0, 0, 0, H);
  grad.addColorStop(0, fillColor0);
  grad.addColorStop(1, fillColor1);

  c.beginPath();
  eq.forEach(function(v, i) {
    if (i === 0) c.moveTo(xOf(i), yOf(v)); else c.lineTo(xOf(i), yOf(v));
  });
  c.lineTo(xOf(eq.length-1), H);
  c.lineTo(xOf(0), H);
  c.closePath();
  c.fillStyle = grad;
  c.fill();

  // Line
  c.beginPath();
  eq.forEach(function(v, i) {
    if (i === 0) c.moveTo(xOf(i), yOf(v)); else c.lineTo(xOf(i), yOf(v));
  });
  c.strokeStyle = lineColor;
  c.lineWidth = 2;
  c.lineJoin = 'round';
  c.stroke();

  // Baseline at 1.0 (if in range)
  if (minEq <= 1.0 && maxEq >= 1.0) {
    const baseY = yOf(1.0);
    c.beginPath();
    c.moveTo(pad, baseY); c.lineTo(W - pad, baseY);
    c.strokeStyle = 'rgba(255,255,255,0.12)';
    c.lineWidth = 1;
    c.setLineDash([4, 6]);
    c.stroke();
    c.setLineDash([]);
  }

  // Summary stat
  const finalEq = eq[eq.length - 1];
  const el = document.getElementById('chartStat');
  if (el) {
    const cumPct = ((finalEq - 1) * 100).toFixed(2);
    el.style.color = isUp ? 'var(--emerald)' : 'var(--red)';
    el.textContent = (isUp ? '+' : '') + cumPct + '%';
  }
}

function renderStatRow(modes, exits, rejects) {
  return '<div class="stat-row">' + renderModeStats(modes) + renderExitStats(exits) + '</div>' +
    renderRejectSummary(rejects);
}

function rejectLabel(r) {
  var map = {
    'NOT_ENOUGH_HISTORY':      '히스토리 부족',
    'TIME_WINDOW_BLOCKED':     '시간창 차단 (9:15 전/14:49 후)',
    'MARKET_FILTER_BLOCKED':   '시장 약세 차단',
    'CHEAP_STOCK_BLOCKED':     '저가주 차단',
    'TURNOVER_FILTER_BLOCKED': '거래대금비율 미달',
    'ABSOLUTE_LIQUIDITY_BLOCKED': '절대 유동성 미달',
    'LOW_VOLUME_SKIP':         '거래량 너무 적음 (<5%)',
    'BELOW_VWAP':              'VWAP 하방',
    'VWAP_SLOPE_DOWN':         'VWAP 하락 기울기',
    'VWAP_TOO_FAR_EXTREME':    'VWAP 과열 (>8%)',
    'NO_ENTRY_MODE':           '진입 패턴 없음 (PULLBACK/BREAKOUT 조건 불충족)',
    'VWAP_GAP_TOO_LARGE':      'VWAP 이격 초과',
    'VOL_RATIO_LOW':           '거래량 비율 미달',
    'TURNOVER_RATIO_LOW':      '거래대금 비율 미달',
    'SCORE_LOW':               '점수 미달',
    'PULLBACK_COND_FAIL':      'PULLBACK 조건 불충족',
    'BREAKOUT_RETEST_FAIL':    'BREAKOUT 재테스트 실패',
    'BREAKOUT_NO_MULTITREND':  'BREAKOUT 다중 상승추세 부재',
    'MOMENTUM_NO_MULTITREND':  'MOMENTUM 다중 상승추세 부재',
    'FILTER_LOW':              '복합 필터 미달',
    'NO_DATA':                 '데이터 없음',
    'UNKNOWN':                 '알 수 없음'
  };
  return map[r] || r;
}

function renderRejectSummary(rejects) {
  if (!rejects || !rejects.length) return '';
  var total = rejects.reduce(function(s, r) { return s + r.count; }, 0);
  var rows = rejects.map(function(r) {
    var pct = total > 0 ? (r.count / total * 100).toFixed(1) : '0.0';
    var barW = total > 0 ? Math.round(r.count / total * 100) : 0;
    return '<tr>' +
      '<td style="color:var(--t1);font-size:11px">' + rejectLabel(r.reason) + '</td>' +
      '<td class="r" style="font-family:var(--mono);font-size:11px;color:var(--t2)">' + r.count + '</td>' +
      '<td class="r" style="font-family:var(--mono);font-size:11px;color:var(--t3)">' + pct + '%</td>' +
      '<td style="width:80px;padding-left:8px"><div style="height:6px;background:var(--t4);border-radius:3px">' +
        '<div style="height:100%;width:' + barW + '%;background:var(--gold);border-radius:3px"></div></div></td>' +
      '</tr>';
  }).join('');
  return '<div class="trades-card" style="margin-top:16px">' +
    '<div class="trades-head"><span class="trades-title">신호 거절 사유 분석</span>' +
    '<span style="font-family:var(--mono);font-size:10px;color:var(--t3)">평가봉 ' + total + '개</span></div>' +
    '<div class="trade-scroll">' +
    '<table class="tbl"><thead><tr><th>거절 사유</th><th class="r">횟수</th><th class="r">비율</th><th></th></tr></thead>' +
    '<tbody>' + rows + '</tbody></table></div></div>';
}

function renderModeStats(modes) {
  if (!modes.length) return '<div class="stat-card"><div class="stat-title">모드별 성과</div><div style="font-family:var(--mono);font-size:10px;color:var(--t3);padding:10px 0;">데이터 없음</div></div>';
  const rows = modes.map(function(m) {
    const w = (m.winRate * 100).toFixed(0);
    const a = (m.avgPnlPct * 100).toFixed(2);
    return '<tr><td>' + modeBadge(m.label) + '</td>' +
      '<td class="r">' + m.count + '</td>' +
      '<td class="r ' + (m.winRate >= 0.5 ? 'pos' : 'neg') + '">' + w + '%</td>' +
      '<td class="r ' + (m.avgPnlPct >= 0 ? 'pos' : 'neg') + '">' + a + '%</td></tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-title">모드별 성과</div>' +
    '<table class="tbl"><thead><tr><th>모드</th><th class="r">횟수</th><th class="r">승률</th><th class="r">평균</th></tr></thead>' +
    '<tbody>' + rows + '</tbody></table></div>';
}

function renderExitStats(exits) {
  if (!exits.length) return '<div class="stat-card"><div class="stat-title">청산 사유</div><div style="font-family:var(--mono);font-size:10px;color:var(--t3);padding:10px 0;">데이터 없음</div></div>';
  const rows = exits.map(function(e) {
    const w = (e.winRate * 100).toFixed(0);
    const a = (e.avgPnlPct * 100).toFixed(2);
    return '<tr><td>' + exitBadge(e.label) + '</td>' +
      '<td class="r">' + e.count + '</td>' +
      '<td class="r ' + (e.winRate >= 0.5 ? 'pos' : 'neg') + '">' + w + '%</td>' +
      '<td class="r ' + (e.avgPnlPct >= 0 ? 'pos' : 'neg') + '">' + a + '%</td></tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-title">청산 사유별</div>' +
    '<table class="tbl"><thead><tr><th>사유</th><th class="r">횟수</th><th class="r">승률</th><th class="r">평균</th></tr></thead>' +
    '<tbody>' + rows + '</tbody></table></div>';
}

function renderTradesCard() {
  return '<div class="trades-card">' +
    '<div class="trades-head"><span class="trades-title">거래 내역</span>' +
    '<span class="trades-count" id="tradesCount"></span></div>' +
    '<div class="trade-scroll" id="tradeTableWrap"></div>' +
    '<div id="pgWrap"></div></div>';
}

function renderTradeList() {
  const cnt = document.getElementById('tradesCount');
  if (cnt) cnt.textContent = allTrades.length + '건';
  const wrap = document.getElementById('tradeTableWrap');
  if (!wrap) return;

  const start = (tradePage - 1) * PAGE_SIZE;
  const page  = allTrades.slice(start, start + PAGE_SIZE);

  if (!page.length) {
    wrap.innerHTML = '<div style="padding:30px;font-family:var(--mono);font-size:11px;color:var(--t3);text-align:center;">거래 없음 — 파라미터를 완화하거나 기간을 늘려보세요.</div>';
  } else {
    const rows = page.map(function(t, i) {
      const p = t.pnlPct;
      const cls = p >= 0 ? 'pos' : 'neg';
      return '<tr>' +
        '<td style="color:var(--t3)">' + (start+i+1) + '</td>' +
        '<td style="color:var(--t2);white-space:nowrap">' + fmtTs(t.entryTime) + '</td>' +
        '<td style="color:var(--t2);white-space:nowrap">' + fmtTs(t.exitTime) + '</td>' +
        '<td>' + modeBadge(t.entryMode) + '</td>' +
        '<td class="r">' + fmtPrice(t.entryPrice) + '</td>' +
        '<td class="r">' + fmtPrice(t.exitPrice) + '</td>' +
        '<td class="r ' + cls + '">' + (p >= 0 ? '+' : '') + (p*100).toFixed(2) + '%</td>' +
        '<td class="r" style="color:var(--t2)">' + (t.signalScore || '-') + '</td>' +
        '<td>' + exitBadge(t.exitReason) + '</td>' +
        '<td class="r" style="color:var(--t3)">' + fmtHold(t.holdSeconds) + '</td>' +
        '</tr>';
    }).join('');
    wrap.innerHTML = '<table class="tbl">' +
      '<thead><tr><th>#</th><th>진입</th><th>청산</th><th>모드</th>' +
      '<th class="r">진입가</th><th class="r">청산가</th><th class="r">수익률</th>' +
      '<th class="r">Score</th><th>사유</th><th class="r">보유</th></tr></thead>' +
      '<tbody>' + rows + '</tbody></table>';
  }

  const pg = document.getElementById('pgWrap');
  if (pg) {
    const pages = Math.ceil(allTrades.length / PAGE_SIZE);
    if (pages > 1) {
      pg.innerHTML = '<div class="pagination">' +
        '<button class="pg-btn" onclick="gotoPage(1)" ' + (tradePage==1?'disabled':'') + '>«</button>' +
        '<button class="pg-btn" onclick="gotoPage(' + (tradePage-1) + ')" ' + (tradePage==1?'disabled':'') + '>‹</button>' +
        '<span class="pg-info">' + tradePage + ' / ' + pages + '</span>' +
        '<button class="pg-btn" onclick="gotoPage(' + (tradePage+1) + ')" ' + (tradePage==pages?'disabled':'') + '>›</button>' +
        '<button class="pg-btn" onclick="gotoPage(' + pages + ')" ' + (tradePage==pages?'disabled':'') + '>»</button>' +
        '</div>';
    } else { pg.innerHTML = ''; }
  }
}

function gotoPage(p) { tradePage = p; renderTradeList(); }

/* ── Format Helpers ── */
function fmtPrice(v) {
  if (!v) return '-';
  return v >= 100 ? v.toLocaleString('ko-KR', {maximumFractionDigits:0}) : v.toFixed(3);
}
function fmtTs(ts) { return ts ? ts.replace('T',' ').substring(0,16) : '-'; }
function fmtHold(sec) {
  if (!sec) return '-';
  const m = Math.floor(sec/60), s = sec%60;
  return m + 'm' + (s > 0 ? s + 's' : '');
}
function modeBadge(m) {
  const map = {PULLBACK:'mb-pullback',BREAKOUT:'mb-breakout',VOLUME_BREAKOUT:'mb-volume',EARLY_MOMENTUM:'mb-early'};
  return '<span class="mode-badge ' + (map[m]||'mb-unknown') + '">' + (m||'?') + '</span>';
}
function exitBadge(r) {
  if (!r) return '-';
  var cls='eb-other', lbl=r.replace(/_/g,' ');
  if (r.startsWith('TAKE_PROFIT')) { cls='eb-tp'; lbl='익절'; }
  else if (r.startsWith('TRAIL')||r==='BREAKEVEN_GUARD') { cls='eb-trail'; lbl='트레일'; }
  else if (r.startsWith('STOP_LOSS')||r==='EMERGENCY_STOP'||r==='FAILED_BREAKOUT'||
           r==='FAILED_PULLBACK'||r==='EARLY_MOMENTUM_DEAD'||r==='VWAP_BREAK') { cls='eb-sl'; }
  else if (r.startsWith('TIME_STOP')||r==='EOD_FORCE_SELL') { cls='eb-time'; }
  return '<span class="exit-badge ' + cls + '">' + lbl + '</span>';
}
function renderEmpty(msg) {
  document.getElementById('btContent').innerHTML =
    '<div class="empty-state"><div class="empty-icon">❌</div>' +
    '<div class="empty-title">오류 발생</div>' +
    '<div class="empty-sub">' + (msg||'알 수 없는 오류') + '</div></div>';
}

/* ── UI Helpers ── */
function setButtons(loading) {
  document.getElementById('btnCollect').disabled = loading;
  document.getElementById('btnRun').disabled     = loading;
}
function showProgress(p, msg) {
  document.getElementById('progressWrap').style.display = 'block';
  document.getElementById('progressBar').style.width    = p + '%';
  document.getElementById('progressMsg').textContent    = msg;
}
function hideProgress() {
  setTimeout(function() { document.getElementById('progressWrap').style.display = 'none'; }, 1500);
}
function toast(msg, type) {
  const el = document.createElement('div');
  el.className = 'toast ' + (type === 'err' ? 'err' : 'ok');
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(function() { el.remove(); }, 3500);
}
</script>
</body>
</html>
