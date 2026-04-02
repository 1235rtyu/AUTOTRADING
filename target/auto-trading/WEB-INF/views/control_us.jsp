<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Control US — AUTO TRADING</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css" rel="stylesheet">
<style>
:root{
  --void:#07090f;--base:#0d0f18;--panel:#13161f;--panel-hi:#181b27;--hover:#1d2130;
  --lime:#c6ff5e;--lime-d:rgba(198,255,94,.1);--lime-b:rgba(198,255,94,.25);
  --emerald:#00e07a;--emerald-d:rgba(0,224,122,.08);--emerald-b:rgba(0,224,122,.22);
  --red:#ff5070;--red-d:rgba(255,80,112,.08);--red-b:rgba(255,80,112,.25);
  --gold:#ffc940;--gold-d:rgba(255,201,64,.08);--gold-b:rgba(255,201,64,.22);
  --blue:#5ba3ff;--blue-d:rgba(91,163,255,.08);--blue-b:rgba(91,163,255,.22);
  --rim:rgba(255,255,255,.07);--rim-hi:rgba(255,255,255,.13);
  --t1:#dde4f0;--t2:#8592ad;--t3:#444d63;--t4:#1a1e2c;
  --mono:'JetBrains Mono','Pretendard',monospace;--sans:'Pretendard',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:50px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;background:var(--void);}
body{font-family:var(--sans);font-size:13px;color:var(--t1);min-height:100vh;}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(198,255,94,.028) 1px,transparent 1px);
  background-size:32px 32px;}

@keyframes sd{from{opacity:0;transform:translateY(-6px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(8px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{opacity:1;transform:scale(1);}50%{opacity:.2;transform:scale(.6);}}
@keyframes pulse-green{0%,100%{box-shadow:0 0 0 0 rgba(0,224,122,.5);}50%{box-shadow:0 0 0 5px rgba(0,224,122,0);}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;
  background:rgba(7,9,15,.97);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .3s ease both;}
.tb-logo{display:flex;align-items:center;gap:8px;padding:0 14px;height:100%;
  border-right:1px solid var(--rim);min-width:168px;}
.logo-mk{width:26px;height:26px;background:var(--lime);border-radius:6px;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;}
.logo-mk svg{width:13px;height:13px;}
.logo-name{font-size:11px;font-weight:700;letter-spacing:.5px;color:var(--t1);}
.logo-name b{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.2px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 8px;}
.tb-a{font-family:var(--mono);font-size:9px;letter-spacing:.4px;padding:4px 8px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-mkt{display:flex;align-items:center;gap:3px;padding:0 12px;border-left:1px solid var(--rim);}
.mkt-pill{font-family:var(--mono);font-size:9px;padding:3px 9px;border-radius:20px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.mkt-pill.kr{color:var(--lime);border-color:var(--lime-b);background:var(--lime-d);}
.mkt-pill.us{color:var(--blue);border-color:var(--blue-b);background:var(--blue-d);}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:.8px;}

/* ── LAYOUT ── */
.page{position:relative;z-index:1;display:grid;grid-template-columns:415px 1fr;gap:10px;padding:10px;min-height:calc(100vh - var(--topbar-h));}
@media(max-width:1100px){.page{grid-template-columns:1fr;}}
.col{display:flex;flex-direction:column;gap:10px;}

/* ── CARD ── */
.card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;}
.card.manual-card{position:relative;overflow:visible;z-index:50;}
.card.manual-card .card-bd{overflow:visible;}
.card-hd{flex-shrink:0;display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:0 12px;height:34px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.card-hd-l{display:flex;align-items:center;gap:7px;}
.hd-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.card-title{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.card-bd{padding:12px;display:flex;flex-direction:column;gap:10px;}

/* ── ENGINE STATUS ── */
.engine-box{display:flex;align-items:center;gap:12px;
  background:var(--base);border:1px solid var(--rim);border-radius:var(--r2);padding:12px 14px;}
.eng-indicator{width:38px;height:38px;border-radius:50%;border:2px solid var(--rim);
  display:flex;align-items:center;justify-content:center;flex-shrink:0;transition:all .3s;}
.eng-indicator.run{border-color:var(--emerald);background:rgba(0,224,122,.1);animation:pulse-green 2s infinite;}
.eng-indicator.stop{border-color:var(--t3);background:rgba(255,255,255,.02);}
.eng-dot{width:10px;height:10px;border-radius:50%;background:var(--t3);transition:all .3s;}
.eng-indicator.run .eng-dot{background:var(--emerald);box-shadow:0 0 8px rgba(0,224,122,.6);}
.eng-info{flex:1;min-width:0;}
.eng-state{font-family:var(--mono);font-size:13px;font-weight:600;color:var(--t1);letter-spacing:.5px;}
.eng-state.run{color:var(--emerald);}
.eng-state.stop{color:var(--t2);}
.eng-sub{font-family:var(--mono);font-size:9px;color:var(--t3);margin-top:2px;}
.eng-sub.run{color:#ff9c2a;text-shadow:0 0 8px rgba(255,156,42,.35);}
.eng-time{font-family:var(--mono);font-size:9px;color:var(--t3);text-align:right;flex-shrink:0;}

/* ── RUNNING SYMBOLS ── */
.run-chips{display:flex;flex-wrap:wrap;gap:5px;min-height:32px;align-items:flex-start;}
.run-chip{display:inline-flex;align-items:center;gap:5px;
  background:rgba(0,224,122,.06);border:1px solid rgba(0,224,122,.2);
  color:var(--emerald);border-radius:20px;padding:4px 9px;
  font-family:var(--mono);font-size:10px;font-weight:600;}
.run-chip .r-name{font-size:8px;color:rgba(0,224,122,.55);font-weight:400;}
.run-chip .r-exch{font-size:8px;color:rgba(0,224,122,.4);
  border:1px solid rgba(0,224,122,.15);border-radius:3px;padding:0 4px;}
.no-run{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:.5px;padding:4px 0;}

/* ── MANUAL INPUT ── */
.input-row{display:flex;gap:6px;align-items:center;flex-wrap:wrap;}
.manual-row{display:flex;gap:6px;align-items:center;margin-top:6px;flex-wrap:wrap;}
.manual-row .tb-sel{min-width:160px;}
.suggest-wrap{position:relative;flex:1;min-width:0;}
.tb-input{flex:1;height:32px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:11px;
  padding:0 9px;outline:none;transition:border-color .15s;}
.amount-input{flex:0 0 130px;width:130px;text-align:right;}
.tb-input:focus{border-color:var(--lime-b);}
.tb-input::placeholder{color:var(--t3);}
.sym-suggest{
  position:absolute;left:0;right:0;top:36px;z-index:120;display:none;
  background:var(--panel-hi);border:1px solid var(--rim-hi);border-radius:var(--r2);
  overflow:hidden;max-height:280px;overflow-y:auto;
  box-shadow:0 10px 22px rgba(0,0,0,.28);
}
.sym-suggest.show{display:block;}
.sym-opt{
  display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:8px 10px;border-bottom:1px solid var(--t4);cursor:pointer;
}
.sym-opt:last-child{border-bottom:none;}
.sym-opt:hover,.sym-opt.act{background:var(--hover);}
.sym-opt-l{min-width:0;}
.sym-opt-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);}
.sym-opt-name{font-size:10px;color:var(--t2);margin-top:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.sym-opt-mkt{
  font-family:var(--mono);font-size:8px;color:var(--blue);
  border:1px solid var(--blue-b);background:var(--blue-d);border-radius:12px;padding:2px 7px;
}
.tb-sel{height:32px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t2);font-family:var(--mono);font-size:10px;
  padding:0 7px;outline:none;cursor:pointer;}
.tb-sel option{background:var(--panel-hi);}
.manual-chips{display:flex;flex-wrap:wrap;gap:5px;min-height:28px;}
.m-chip{display:inline-flex;align-items:center;gap:5px;
  background:var(--panel-hi);border:1px solid var(--rim-hi);
  color:var(--t1);border-radius:20px;padding:3px 8px;
  font-family:var(--mono);font-size:10px;}
.m-chip .m-name{font-size:8px;color:var(--t2);}
.m-chip-del{width:14px;height:14px;border:1px solid var(--t3);border-radius:50%;
  background:transparent;color:var(--t3);cursor:pointer;font-size:10px;
  display:flex;align-items:center;justify-content:center;padding:0;transition:all .12s;line-height:1;}
.m-chip-del:hover{border-color:var(--red);color:var(--red);background:var(--red-d);}
.btn-row{display:flex;gap:6px;}

/* ── BUTTONS ── */
.btn{height:32px;padding:0 12px;border-radius:var(--r);border:1px solid transparent;
  cursor:pointer;font-family:var(--mono);font-size:10px;font-weight:600;
  letter-spacing:.5px;display:inline-flex;align-items:center;gap:5px;
  transition:all .14s;white-space:nowrap;}
.btn:hover{filter:brightness(1.12);}
.btn:active{transform:scale(.97);}
.btn svg{width:11px;height:11px;flex-shrink:0;}
.btn-lime{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.btn-lime:hover{background:var(--lime);color:var(--void);}
.btn-green{background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.btn-green:hover{background:var(--emerald);color:var(--void);}
.btn-red{background:var(--red-d);border-color:var(--red-b);color:var(--red);}
.btn-red:hover{background:var(--red);color:#fff;}
.btn-blue{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.btn-ghost{background:transparent;border-color:var(--rim-hi);color:var(--t2);}
.btn-ghost:hover{border-color:var(--rim-hi);color:var(--t1);background:var(--hover);}
.btn-full{width:100%;justify-content:center;}

/* ── TOP N 필터 ── */
.filter-row{display:flex;gap:6px;align-items:center;flex-wrap:wrap;}
.filter-label{font-family:var(--mono);font-size:8px;color:var(--t2);letter-spacing:1px;text-transform:uppercase;}
.filter-input{height:28px;width:64px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 7px;outline:none;}

/* ── TOP 종목 리스트 ── */
.top-scroll{overflow-y:auto;max-height:500px;padding:2px 0;
  scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.top-scroll::-webkit-scrollbar{width:3px;}
.top-scroll::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
.top-item{display:grid;grid-template-columns:26px 1fr auto;align-items:center;gap:8px;
  padding:7px 12px;border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;}
.top-item:last-child{border-bottom:none;}
.top-item:hover{background:var(--hover);}
.top-item.sel{background:rgba(91,163,255,.06);border-bottom-color:rgba(91,163,255,.15);}
.top-rank{width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;
  background:var(--t4);font-family:var(--mono);font-size:9px;font-weight:700;color:var(--t2);flex-shrink:0;}
.top-rank.r1{background:rgba(255,201,64,.15);color:var(--gold);}
.top-rank.r2{background:rgba(185,195,210,.1);color:#b9c3d2;}
.top-rank.r3{background:rgba(205,139,90,.1);color:#cd8b5a;}
.top-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);line-height:1.2;}
.top-nm{font-size:10px;color:var(--t2);margin-top:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.top-vol{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:2px;}
.top-pr{text-align:right;flex-shrink:0;}
.top-price{font-family:var(--mono);font-size:11px;font-weight:600;color:var(--t1);}
.top-rate{font-family:var(--mono);font-size:10px;margin-top:2px;}
.top-rate.up{color:var(--emerald);} .top-rate.dn{color:var(--red);} .top-rate.fl{color:var(--t2);}
.top-sel-badge{width:16px;height:16px;border-radius:50%;border:1.5px solid var(--rim-hi);
  display:flex;align-items:center;justify-content:center;flex-shrink:0;transition:all .12s;}
.top-item.sel .top-sel-badge{border-color:var(--blue);background:var(--blue-d);}
.top-item.sel .top-sel-badge::after{content:'✓';font-size:8px;color:var(--blue);font-weight:700;}

/* ── ORDER TABLE ── */
.tbl-toolbar{display:flex;align-items:center;gap:6px;flex-wrap:wrap;
  padding:8px 12px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.tbl-search{flex:1;min-width:160px;height:28px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;outline:none;}
.tbl-search:focus{border-color:var(--lime-b);}
.tbl-search::placeholder{color:var(--t3);}
.side-btns{display:flex;gap:2px;}
.side-btn{height:28px;padding:0 10px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t2);cursor:pointer;transition:all .12s;}
.side-btn.act{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.side-btn.act-buy{background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.side-btn.act-sell{background:var(--red-d);border-color:var(--red-b);color:var(--red);}
.tbl-wrap{overflow:auto;flex:1;min-height:0;
  scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.tbl-wrap::-webkit-scrollbar{width:3px;height:3px;}
.tbl-wrap::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
table{width:100%;border-collapse:collapse;font-size:11px;}
thead th{position:sticky;top:0;z-index:2;
  background:var(--panel-hi);border-bottom:1px solid var(--rim);
  font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.2px;text-transform:uppercase;padding:7px 10px;text-align:left;white-space:nowrap;}
tbody td{padding:8px 10px;border-bottom:1px solid var(--t4);vertical-align:middle;white-space:nowrap;}
tbody tr:last-child td{border-bottom:none;}
tbody tr:hover td{background:var(--hover);}
.td-num{text-align:right;font-family:var(--mono);}
.td-id{font-family:var(--mono);font-size:10px;color:var(--t2);}
.td-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);line-height:1.2;}
.td-nm{font-size:10px;color:var(--t2);margin-top:1px;}
.td-price{font-family:var(--mono);font-size:11px;color:var(--gold);font-weight:600;}
.td-reason{font-size:10px;color:var(--t2);max-width:200px;overflow:hidden;text-overflow:ellipsis;}
.td-time{font-family:var(--mono);font-size:10px;color:var(--t2);}
.side-badge{display:inline-flex;align-items:center;
  font-family:var(--mono);font-size:9px;font-weight:700;
  padding:2px 7px;border-radius:4px;}
.side-buy{color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.side-sell{color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.tbl-empty{text-align:center;padding:28px!important;font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:1px;}
.pager{display:flex;align-items:center;justify-content:flex-end;gap:6px;
  padding:8px 12px;border-top:1px solid var(--rim);background:var(--panel-hi);flex-shrink:0;}
.page-info{font-family:var(--mono);font-size:9px;color:var(--t2);}

/* ── BADGE / PILL ── */
.badge{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:5px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.badge.ok{color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.badge.warn{color:var(--gold);border-color:var(--gold-b);background:var(--gold-d);}
.badge.cnt{color:var(--blue);border-color:var(--blue-b);background:var(--blue-d);}

/* ── TOAST ── */
.toast{position:fixed;right:14px;bottom:14px;z-index:999;
  max-width:360px;background:var(--panel-hi);border:1px solid var(--rim-hi);
  color:var(--t1);border-radius:10px;padding:10px 14px;
  font-family:var(--mono);font-size:11px;letter-spacing:.3px;
  opacity:0;transform:translateY(8px);pointer-events:none;transition:.2s;}
.toast.show{opacity:1;transform:translateY(0);}
.toast.ok{border-color:var(--emerald-b);color:var(--emerald);}
.toast.err{border-color:var(--red-b);color:var(--red);}

/* ── DIVIDER ── */
.divider{height:1px;background:var(--rim);margin:2px 0;}

/* ── 빈 상태 ── */
.empty-hint{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:.5px;padding:3px 0;}
</style>
</head>
<body>
<div class="bg-grid"></div>

<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mk">
      <svg viewBox="0 0 24 24" fill="none" stroke="#07090f" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-name">AUTO<b>TRADE</b></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-sp"></div>
  <div class="tb-nav">
    <a class="tb-a" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/kr" id="navKR">Control·KR</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/us" id="navUS">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-mkt">
    <span class="mkt-pill" id="mktPill">${market}</span>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page" id="page">

  <!-- ── 좌측 컬럼 ── -->
  <div class="col">

    <!-- Engine Status -->
    <div class="card" style="animation:fu .3s .04s ease both;">
      <div class="card-hd">
        <div class="card-hd-l">
          <div class="hd-dot" style="background:var(--emerald)"></div>
          <span class="card-title">Engine Status</span>
        </div>
        <span class="badge" id="engTime">--:--:--</span>
      </div>
      <div class="card-bd">
        <div class="engine-box">
          <div class="eng-indicator stop" id="engInd">
            <div class="eng-dot"></div>
          </div>
          <div class="eng-info">
            <div class="eng-state stop" id="engState">STOPPED</div>
            <div class="eng-sub" id="engMsg">엔진이 정지되어 있습니다</div>
          </div>
          <div class="eng-time" id="engUpdated">--</div>
        </div>
      </div>
    </div>

    <!-- Running Symbols -->
    <div class="card" style="animation:fu .3s .07s ease both;">
      <div class="card-hd">
        <div class="card-hd-l">
          <div class="hd-dot" style="background:var(--lime)"></div>
          <span class="card-title">Running Symbols</span>
        </div>
        <span class="badge cnt" id="runCount">0</span>
      </div>
      <div class="card-bd">
        <div class="run-chips" id="runList">
          <span class="no-run">실행 중인 종목 없음</span>
        </div>
      </div>
    </div>

    <!-- Manual Start -->
    <div class="card manual-card" style="animation:fu .3s .1s ease both;">
      <div class="card-hd">
        <div class="card-hd-l">
          <div class="hd-dot" style="background:var(--blue)"></div>
          <span class="card-title">Manual Start</span>
        </div>
      </div>
      <div class="card-bd">
        <div class="input-row">
          <div class="suggest-wrap">
            <input class="tb-input" id="symInput" placeholder="미국 종목코드/이름 입력 (예: NVDA, 엔비디아)" maxlength="24" autocomplete="off" spellcheck="false"/>
            <div class="sym-suggest" id="symSuggest"></div>
          </div>
          <input class="tb-input amount-input" id="buyAmountInput" type="number" min="0" step="0.01" placeholder="종목당 금액"/>
          <button class="btn btn-lime" onclick="addManual()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            추가
          </button>
        </div>
        <div class="manual-row">
          <select class="tb-sel" id="wlFolderSel"></select>
          <button class="btn btn-ghost" onclick="loadWatchlistToManual()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3h18v4H3z"/><path d="M5 7v14h14V7"/></svg>
            Watchlist 불러오기
          </button>
        </div>
        <div class="manual-chips" id="manualChips">
          <span class="empty-hint">종목을 추가하세요</span>
        </div>
        <div class="divider"></div>
        <div class="btn-row">
          <button class="btn btn-green btn-full" onclick="startManual()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            선택 종목 시작
          </button>
          <button class="btn btn-red" onclick="stopAll()" style="flex-shrink:0;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>
            전체 중지
          </button>
        </div>
      </div>
    </div>

    <!-- Volume Top 40 -->
    <div class="card" style="animation:fu .3s .13s ease both;">
      <div class="card-hd">
        <div class="card-hd-l">
          <div class="hd-dot" style="background:var(--gold)"></div>
          <span class="card-title">Volume Top 40</span>
        </div>
        <button class="btn btn-ghost" style="height:24px;padding:0 9px;font-size:8px;" onclick="fetchTop()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          Refresh
        </button>
      </div>
      <div class="card-bd">
        <div class="filter-row">
          <span class="filter-label">Top N</span>
          <input class="filter-input" id="topN" type="number" min="1" max="40" value="3"/>
          <span class="filter-label">Min %</span>
          <input class="filter-input" id="minRate" type="number" step="0.1" min="0" value="0" style="width:56px;"/>
          <span class="empty-hint" style="margin-left:auto;" id="topSelCount">선택 0개</span>
        </div>
        <div class="btn-row">
          <button class="btn btn-blue btn-full" onclick="startTop()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="width:10px;height:10px;"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            Top 시작
          </button>
        </div>
      </div>
      <div class="top-scroll" id="topList">
        <div style="padding:20px;text-align:center;font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1px;">로딩 중…</div>
      </div>
    </div>

  </div><!-- /col -->

  <!-- ── 우측 컬럼: 주문 이력 ── -->
  <div class="col">
    <div class="card" style="flex:1;min-height:0;animation:fu .3s .05s ease both;">
      <div class="card-hd">
        <div class="card-hd-l">
          <div class="hd-dot" style="background:var(--gold)"></div>
          <span class="card-title">Order History</span>
        </div>
        <span class="badge" id="ordSource" style="font-size:7px;">/api/orders/us</span>
      </div>
      <div class="tbl-toolbar">
        <input class="tbl-search" id="searchInput" placeholder="종목 / 종목명 / reason 검색" oninput="applyFilter()"/>
        <div class="side-btns">
          <button class="side-btn act" id="btnAll"  onclick="setSide('ALL')">ALL</button>
          <button class="side-btn"     id="btnBUY"  onclick="setSide('BUY')">BUY</button>
          <button class="side-btn"     id="btnSELL" onclick="setSide('SELL')">SELL</button>
        </div>
        <select class="tb-sel" id="limitSel" onchange="fetchOrders(false)">
          <option value="50">50건</option>
          <option value="100">100건</option>
          <option value="200">200건</option>
        </select>
        <button class="btn btn-ghost" id="refBtn" onclick="fetchOrders(false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          Refresh
        </button>
      </div>
      <div class="tbl-wrap" id="ordWrap">
        <table>
          <thead>
            <tr>
              <th style="width:56px;">ID</th>
              <th style="width:130px;">Symbol</th>
              <th style="width:64px;">Side</th>
              <th style="width:56px;text-align:right;">Qty</th>
              <th style="width:110px;text-align:right;">Price</th>
              <th>Reason</th>
              <th style="width:150px;">Time</th>
            </tr>
          </thead>
          <tbody id="ordBody">
            <tr><td colspan="7" class="tbl-empty">로딩 중…</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager">
        <span class="page-info" id="pageInfo">0 / 0</span>
        <button class="btn btn-ghost" style="height:26px;padding:0 10px;font-size:9px;" onclick="movePage(-1)">← Prev</button>
        <button class="btn btn-ghost" style="height:26px;padding:0 10px;font-size:9px;" onclick="movePage(1)">Next →</button>
      </div>
    </div>
  </div>

</div><!-- /page -->
<div class="toast" id="toast"></div>

<script>
'use strict';

/* ── 전역 ── */
const B      = '${pageContext.request.contextPath}';
const MARKET = '${market != null ? market : "US"}';
const EXCH   = MARKET === 'US' ? 'NAS' : 'KRX';
const DAYS   = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const PAGE_SZ = 20;

let manualSyms  = [];
let topRows     = [];
let topSel      = new Set();
let rawOrders   = [];
let sideFilter  = 'ALL';
let curPage     = 1;

const symInputEl = document.getElementById('symInput');
const symSuggestEl = document.getElementById('symSuggest');
const buyAmountEl = document.getElementById('buyAmountInput');
let suggestRows = [];
let suggestIdx = -1;
let suggestTimer = null;
let suggestReqId = 0;
let suggestAbortController = null;
const SUGGEST_LIMIT = 12;

/* ── 초기화: nav active, mktPill ── */
(function(){
  const pill = document.getElementById('mktPill');
  if(MARKET==='KR'){
    pill.classList.add('kr');
    document.getElementById('navKR').classList.add('cur');
  } else {
    pill.classList.add('us');
    document.getElementById('navUS').classList.add('cur');
  }
})();

/* ── 시계 ── */
function p2(v){return String(v).padStart(2,'0');}
function tick(){
  const n=new Date();
  document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000);tick();

/* ── 유틸 ── */
function esc(s){return String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function toast(msg,type){
  const el=document.getElementById('toast');
  el.textContent=msg;el.className='toast show '+(type||'');
  clearTimeout(el._t);el._t=setTimeout(()=>el.className='toast',2600);
}
function post(path,data){
  return fetch(B+path,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data||{}).toString()}).then(r=>r.json());
}
function get(path){return fetch(B+path).then(r=>r.json());}
function fmtVol(v){
  const n=Number(v||0);
  if(n>=1e9)return(n/1e9).toFixed(1)+'B';
  if(n>=1e8)return(n/1e8).toFixed(1)+'억';
  if(n>=1e6)return(n/1e6).toFixed(1)+'M';
  if(n>=1e3)return(n/1e3).toFixed(0)+'K';
  return n.toLocaleString();
}
function fmtTime(v){
  if(Array.isArray(v)&&v.length>=6)return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+' '+p2(v[3])+':'+p2(v[4])+':'+p2(v[5]);
  if(typeof v==='string')return v.replace('T',' ').substring(0,19);
  return '-';
}
function resolveBuyAmount(){
  const raw = String((buyAmountEl && buyAmountEl.value) || '').trim();
  if(!raw) return null;
  const value = Number(raw);
  if(!Number.isFinite(value) || value <= 0){
    toast('종목당 금액은 0보다 큰 숫자로 입력하세요','err');
    return undefined;
  }
  return value;
}

function hideSuggest(){
  suggestRows = [];
  suggestIdx = -1;
  symSuggestEl.classList.remove('show');
  symSuggestEl.innerHTML = '';
}
function renderSuggest(){
  if(!suggestRows.length){hideSuggest();return;}
  symSuggestEl.innerHTML = suggestRows.map((row, idx) =>
    '<div class="sym-opt'+(idx===suggestIdx?' act':'')+'" data-idx="'+idx+'">'
      +'<div class="sym-opt-l">'
        +'<div class="sym-opt-sym">'+esc(row.symbol)+'</div>'
        +'<div class="sym-opt-name">'+esc(row.name||row.ko||row.symbol)+'</div>'
      +'</div>'
      +'<span class="sym-opt-mkt">US</span>'
    +'</div>'
  ).join('');
  symSuggestEl.classList.add('show');
}
function moveSuggest(step){
  if(!suggestRows.length) return;
  suggestIdx += step;
  if(suggestIdx < 0) suggestIdx = suggestRows.length - 1;
  if(suggestIdx >= suggestRows.length) suggestIdx = 0;
  renderSuggest();
}
function buildTypedCandidate(raw){
  const typed = String(raw || '').trim().toUpperCase();
  if(!typed) return null;
  if(!/^[A-Z][A-Z0-9.\-]{0,9}$/.test(typed)) return null;
  return {symbol: typed, name: '직접 입력 종목', market: 'US'};
}
function fetchSuggestRows(raw){
  const query = String(raw || '').trim();
  if(!query) return Promise.resolve([]);
  const reqId = ++suggestReqId;
  if (suggestAbortController) {
    suggestAbortController.abort();
  }
  suggestAbortController = new AbortController();

  const url = B
    + '/api/market/symbol-suggest?market=' + encodeURIComponent('US')
    + '&exch=' + encodeURIComponent(EXCH)
    + '&limit=' + encodeURIComponent(SUGGEST_LIMIT)
    + '&q=' + encodeURIComponent(query);

  return fetch(url, { cache: 'no-store', signal: suggestAbortController.signal })
    .then(r => r.ok ? r.json() : Promise.reject(new Error('suggest http '+r.status)))
    .then(data => {
      if (reqId !== suggestReqId) return [];
      const rows = Array.isArray(data && data.data) ? data.data : [];
      const mapped = rows
        .map(row => ({
          symbol: String(row.symbol || '').toUpperCase(),
          name: String(row.name || '').trim(),
          market: String(row.market || 'US').toUpperCase()
        }))
        .filter(row => row.symbol);
      const typed = buildTypedCandidate(query);
      if (typed && !mapped.some(row => row.symbol === typed.symbol)) {
        mapped.unshift(typed);
      }
      return mapped.slice(0, SUGGEST_LIMIT);
    })
    .catch(err => {
      if (err && err.name === 'AbortError') return [];
      const typed = buildTypedCandidate(query);
      return typed ? [typed] : [];
    });
}
function queueSuggest(raw){
  if (suggestTimer) clearTimeout(suggestTimer);
  const query = String(raw || '').trim();
  if (!query) {
    hideSuggest();
    return;
  }
  suggestTimer = setTimeout(() => {
    fetchSuggestRows(query).then(rows => {
      if (String(symInputEl.value || '').trim() !== query) return;
      suggestRows = rows;
      suggestIdx = suggestRows.length ? 0 : -1;
      renderSuggest();
    });
  }, 140);
}
function pickSuggestAt(idx){
  const row = suggestRows[idx];
  if(!row) return null;
  hideSuggest();
  return row.symbol;
}

function captureScrollState(){
  const wrap=document.getElementById('ordWrap');
  return {pageY:window.scrollY||window.pageYOffset||0,wrap:wrap,wrapY:wrap?wrap.scrollTop:0};
}
function restoreScrollState(state){
  if(!state)return;
  requestAnimationFrame(()=>{
    if(state.wrap)state.wrap.scrollTop=state.wrapY;
    window.scrollTo(0,state.pageY||0);
  });
}
/* ── 종목명 조회 (캐시) ── */
const nameCache={};
function fetchName(sym){
  if(nameCache[sym]!==undefined)return Promise.resolve(nameCache[sym]);
  return get('/api/watchlist/name?symbol='+encodeURIComponent(sym))
    .then(d=>{nameCache[sym]=d.symbolName||'';return nameCache[sym];})
    .catch(()=>{nameCache[sym]='';return '';});
}
function detectMarketForControl(item){
  const ex=String(item.exchange||'').toUpperCase();
  if(ex==='KRX'||ex==='KR') return 'KR';
  if(['NAS','NYS','AMS','US','NASD','NYSE','AMEX'].includes(ex)) return 'US';
  return /^[A-Za-z]/.test(item.symbol) ? 'US' : 'KR';
}

/* ── Engine Status ── */
function setEngine(status,msg){
  const run=String(status||'').indexOf('RUNNING')===0;
  const ind=document.getElementById('engInd');
  const st=document.getElementById('engState');
  ind.className='eng-indicator '+(run?'run':'stop');
  st.className='eng-state '+(run?'run':'stop');
  st.textContent=status||'UNKNOWN';
  const engMsg=document.getElementById('engMsg');
  engMsg.className='eng-sub'+(run?' run':'');
  const count=Number(document.getElementById('runCount')?.textContent||0);
  engMsg.textContent=msg||(run?('자동매매 실행 중 · '+count+'종목'):'엔진이 정지되어 있습니다');
  document.getElementById('engTime').textContent=p2(new Date().getHours())+':'+p2(new Date().getMinutes())+':'+p2(new Date().getSeconds());
}
function fetchStatus(){get('/api/control/status').then(d=>setEngine(d.status,'')).catch(()=>{});}

/* ── Running Symbols ── */
function renderRunning(rows){
  const arr=Array.isArray(rows)?rows:[];
  document.getElementById('runCount').textContent=arr.length;
  const engMsg=document.getElementById('engMsg');
  if(engMsg && engMsg.classList.contains('run')){
    engMsg.textContent='자동매매 실행 중 · '+arr.length+'종목';
  }
  const list=document.getElementById('runList');
  if(!arr.length){list.innerHTML='<span class="no-run">실행 중인 종목 없음</span>';return;}
  Promise.all(arr.map(r=>fetchName(r.symbol||'').then(nm=>({...r,nm})))).then(items=>{
    list.innerHTML=items.map(r=>{
      const sym=esc(r.symbol||'-');
      const nm=r.nm?'<span class="r-name">'+esc(r.nm)+'</span>':'';
      const ex='<span class="r-exch">'+esc(r.exchange||'AUTO')+'</span>';
      return '<span class="run-chip">'+sym+nm+ex+'</span>';
    }).join('');
  });
}
function fetchRunning(silent){
  return get('/api/control/running')
    .then(d=>{renderRunning(d.symbols||[]);if(d.status)setEngine(d.status,'');})
    .catch(()=>{if(!silent)toast('running 조회 실패','err');});
}

/* ── Manual Symbols ── */
function renderManual(){
  const wrap=document.getElementById('manualChips');
  if(!manualSyms.length){wrap.innerHTML='<span class="empty-hint">종목을 추가하세요</span>';return;}
  Promise.all(manualSyms.map(sym=>fetchName(sym).then(nm=>({sym,nm})))).then(items=>{
    wrap.innerHTML=items.map(({sym,nm})=>
      '<span class="m-chip">'+esc(sym)+(nm?'<span class="m-name">'+esc(nm)+'</span>':'')+
      '<button class="m-chip-del" onclick="removeManual(\''+esc(sym)+'\')">×</button></span>'
    ).join('');
  });
}
window.addManual=function(){
  const forced = arguments[0];
  const sym=((forced||symInputEl.value||'').trim().toUpperCase());
  if(!sym)return;
  if(manualSyms.includes(sym)){toast(sym+' 이미 추가됨');return;}
  manualSyms.push(sym);
  symInputEl.value='';
  hideSuggest();
  renderManual();
};
window.removeManual=function(sym){manualSyms=manualSyms.filter(v=>v!==sym);renderManual();};
window.loadWatchlistToManual=function(){
  return get('/api/watchlist')
    .then(items=>{
      const rows=Array.isArray(items)?items:[];
      const folder=(document.getElementById('wlFolderSel')||{}).value||'';
      if(folder===''){ toast('폴더를 선택하세요','err'); return; }
      const syms=rows
        .filter(it=>detectMarketForControl(it)===MARKET)
        .filter(it=>{
          const f=(it.folder||'').trim();
          return folder==='ALL' || f===folder;
        })
        .map(it=>String(it.symbol||'').trim())
        .filter(Boolean);
      manualSyms=Array.from(new Set(syms));
      renderManual();
      toast('Watchlist 불러오기 완료: '+manualSyms.length+'개','ok');
    })
    .catch(()=>toast('Watchlist 불러오기 실패','err'));
};
function renderWatchlistFolders(rows){
  const sel=document.getElementById('wlFolderSel');
  if(!sel)return;
  const folderSet=new Set();
  rows.forEach(it=>{
    const f=(it.folder||'').trim();
    if(f) folderSet.add(f);
  });
  sel.innerHTML='<option value="" selected>폴더 선택</option><option value="ALL">전체</option>';
  Array.from(folderSet).sort().forEach(f=>{
    const opt=document.createElement('option');
    opt.value=f; opt.textContent=f;
    sel.appendChild(opt);
  });
}
symInputEl.addEventListener('input',()=>{
  queueSuggest(symInputEl.value);
});
symInputEl.addEventListener('keydown',e=>{
  if(e.key==='ArrowDown'){e.preventDefault();moveSuggest(1);return;}
  if(e.key==='ArrowUp'){e.preventDefault();moveSuggest(-1);return;}
  if(e.key==='Escape'){hideSuggest();return;}
  if(e.key==='Enter'){
    e.preventDefault();
    if(suggestRows.length){
      const picked = pickSuggestAt(suggestIdx >= 0 ? suggestIdx : 0);
      if(picked){window.addManual(picked);return;}
    }
    window.addManual();
  }
});
symSuggestEl.addEventListener('mousedown',(e)=>{
  const opt = e.target.closest('.sym-opt');
  if(!opt) return;
  const idx = Number(opt.dataset.idx);
  const picked = pickSuggestAt(idx);
  if(picked) window.addManual(picked);
});
document.addEventListener('click',(e)=>{
  if(!e.target.closest('.suggest-wrap')) hideSuggest();
});

window.startManual=function(){
  if(!manualSyms.length){toast('종목을 추가하세요');return;}
  const buyAmount = resolveBuyAmount();
  if (buyAmount === undefined) return;
  Promise.all(manualSyms.map(sym=>{
    const body={symbol:sym};if(MARKET==='US')body.exchange=EXCH;
    if (buyAmount != null) body.buyAmount = buyAmount;
    return post('/api/control/start',body);
  })).then(res=>{
    const last=res[res.length-1]||{};
    setEngine(last.status,last.message||'');fetchRunning(true);
    toast('시작 완료: '+manualSyms.join(', '),'ok');
  }).catch(e=>toast('시작 실패: '+(e.message||'error'),'err'));
};

window.stopAll=function(){
  post('/api/control/stop',{}).then(d=>{
    setEngine(d.status,d.message||'');fetchRunning(true);toast('전체 중지','ok');
  }).catch(e=>toast('중지 실패: '+(e.message||'error'),'err'));
};

/* ── Volume Top ── */
function renderTop(){
  const list=document.getElementById('topList');
  if(!topRows.length){list.innerHTML='<div style="padding:20px;text-align:center;font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1px;">데이터 없음</div>';return;}
  document.getElementById('topSelCount').textContent='선택 '+topSel.size+'개';
  list.innerHTML=topRows.map((r,i)=>{
    const sym=String(r.symbol||'');
    const nm=r.name||sym;
    const price=Number(r.stck_prpr||0);
    const rate=Number(r.prdy_ctrt||0);
    const rc=i===0?'r1':i===1?'r2':i===2?'r3':'';
    const sel=topSel.has(sym)?' sel':'';
    const rateClass=rate>0?'up':rate<0?'dn':'fl';
    const rateStr=(rate>0?'+':'')+rate.toFixed(2)+'%';
    return '<div class="top-item'+sel+'" onclick="toggleTop(\''+esc(sym)+'\')">'
      +'<div class="top-rank '+rc+'">'+(i+1)+'</div>'
      +'<div style="min-width:0;">'
        +'<div class="top-sym">'+esc(sym)+'</div>'
        +'<div class="top-nm">'+esc(nm)+'</div>'
        +'<div class="top-vol">'+fmtVol(r.acml_tr_pbmn!=null?r.acml_tr_pbmn:r.acml_vol)+'</div>'
      +'</div>'
      +'<div class="top-pr">'
        +'<div class="top-price">'+(price?price.toLocaleString():'-')+'</div>'
        +'<div class="top-rate '+rateClass+'">'+rateStr+'</div>'
      +'</div>'
      +'<div class="top-sel-badge"></div>'
    +'</div>';
  }).join('');
}
window.toggleTop=function(sym){if(!sym)return;topSel.has(sym)?topSel.delete(sym):topSel.add(sym);renderTop();};
window.fetchTop=function(){
  return get('/api/market/ranking?market='+encodeURIComponent(MARKET)+'&exch='+encodeURIComponent(EXCH))
    .then(d=>{topRows=(d.data||d.output||[]).slice(0,40);renderTop();})
    .catch(()=>{topRows=[];renderTop();});
};
window.startTop=function(){
  const buyAmount = resolveBuyAmount();
  if (buyAmount === undefined) return;
  if(topSel.size>0){
    const raw=Array.from(topSel);
    const sels=raw.filter(s=>/^[A-Z][A-Z0-9.\-]{0,9}$/.test(String(s||'')));
    if(!sels.length){
      toast('선택된 종목 코드가 올바르지 않습니다','err');
      return;
    }
    if(sels.length !== raw.length){
      toast('잘못된 코드가 제외되었습니다','err');
    }
    Promise.all(sels.map(sym=>{
      const body={symbol:sym};if(MARKET==='US')body.exchange=EXCH;
      if (buyAmount != null) body.buyAmount = buyAmount;
      return post('/api/control/start',body);
    })).then(res=>{
      const last=res[res.length-1]||{};
      setEngine(last.status,last.message||'');fetchRunning(true);
      toast('선택 종목 시작: '+sels.join(', '),'ok');
    }).catch(e=>toast('실패: '+(e.message||''),'err'));
    return;
  }
  const n=document.getElementById('topN').value||3;
  const mr=document.getElementById('minRate').value||0;
  let startTopUrl='/api/control/start-top?n='+n+'&minRate='+mr+'&market='+encodeURIComponent(MARKET)+'&exch='+encodeURIComponent(EXCH);
  if (buyAmount != null) startTopUrl += '&buyAmount=' + encodeURIComponent(buyAmount);
  post(startTopUrl,{})
    .then(d=>{setEngine(d.status,d.message||'');fetchRunning(true);toast(d.message||'Top 시작','ok');})
    .catch(e=>toast('실패: '+(e.message||''),'err'));
};

/* ── Order Table ── */
function filterOrders(){
  const q=(document.getElementById('searchInput').value||'').toLowerCase();
  return rawOrders.filter(r=>{
    const sideOk=sideFilter==='ALL'||r.side===sideFilter;
    const txt=((r.symbol||'')+' '+(r.symbolName||'')+' '+(r.reason||'')).toLowerCase();
    return sideOk&&(!q||txt.includes(q));
  });
}
function renderOrders(scrollState){
  const filtered=filterOrders();
  const pageCount=Math.max(1,Math.ceil(filtered.length/PAGE_SZ));
  if(curPage>pageCount)curPage=pageCount;
  if(curPage<1)curPage=1;
  const rows=filtered.slice((curPage-1)*PAGE_SZ,curPage*PAGE_SZ);
  const body=document.getElementById('ordBody');
  if(!rows.length){body.innerHTML='<tr><td colspan="7" class="tbl-empty">No orders</td></tr>';
    restoreScrollState(scrollState);
  } else {
    // 종목명 일괄 로드 후 렌더
    const syms=[...new Set(rows.map(r=>r.symbol).filter(Boolean))];
    Promise.all(syms.map(s=>fetchName(s))).then(()=>{
      body.innerHTML=rows.map(r=>{
        const sideClass=r.side==='BUY'?'side-buy':'side-sell';
        const sym=esc(r.symbol||'-');
        const nm=nameCache[r.symbol]||r.symbolName||'';
        const symCell='<div class="td-sym">'+sym+'</div>'+(nm?'<div class="td-nm">'+esc(nm)+'</div>':'');
        return '<tr>'
          +'<td class="td-id">#'+esc(r.id)+'</td>'
          +'<td>'+symCell+'</td>'
          +'<td><span class="side-badge '+sideClass+'">'+esc(r.side||'-')+'</span></td>'
          +'<td class="td-num">'+esc(r.quantity||'-')+'</td>'
          +'<td class="td-num td-price">'+(r.price?Number(r.price).toLocaleString():'-')+'</td>'
          +'<td class="td-reason" title="'+esc(r.reason||'')+'">'+esc(r.reason||'-')+'</td>'
          +'<td class="td-time">'+esc(fmtTime(r.createdAt))+'</td>'
        +'</tr>';
      }).join('');
      restoreScrollState(scrollState);
    });
  }
  const from=filtered.length?(curPage-1)*PAGE_SZ+1:0;
  const to=Math.min(curPage*PAGE_SZ,filtered.length);
  document.getElementById('pageInfo').textContent=from+'-'+to+' / '+filtered.length;
}
window.applyFilter=function(){curPage=1;renderOrders();};
window.setSide=function(s){
  sideFilter=s;
  document.getElementById('btnAll').className='side-btn'+(s==='ALL'?' act':'');
  document.getElementById('btnBUY').className='side-btn'+(s==='BUY'?' act-buy':'');
  document.getElementById('btnSELL').className='side-btn'+(s==='SELL'?' act-sell':'');
  curPage=1;renderOrders();
};
window.movePage=function(d){curPage+=d;renderOrders();};
window.fetchOrders=function(keepPage){
  const btn=document.getElementById('refBtn');
  const scrollState=keepPage?captureScrollState():null;
  btn.textContent='…';
  const limit=document.getElementById('limitSel').value||50;
  const ep=MARKET==='US'?'/api/orders/us':'/api/orders/kr';
  get(ep+'?limit='+limit)
    .then(rows=>{rawOrders=Array.isArray(rows)?rows:[];if(!keepPage)curPage=1;renderOrders(scrollState);})
    .catch(e=>toast('주문 조회 실패','err'))
    .finally(()=>btn.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg> Refresh');
};

/* ── 초기 로드 ── */
fetchStatus();
fetchRunning(false);
fetchTop();
fetchOrders(false);
get('/api/watchlist')
  .then(items=>{const rows=Array.isArray(items)?items:[];renderWatchlistFolders(rows);})
  .catch(()=>{});

/* ── 폴링 ── */
setInterval(()=>{fetchStatus();fetchRunning(true);fetchOrders(true);},5000);
setInterval(fetchTop,60000);
</script>
</body>
</html>
