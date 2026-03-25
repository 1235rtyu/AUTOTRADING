<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Order History — AUTO TRADING</title>
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
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:52px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;background:var(--void);}
body{font-family:var(--sans);font-size:13px;color:var(--t1);min-height:100vh;}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.05) 0%,transparent 55%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.035) 1px,transparent 1px);background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes spin{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes pd{0%,100%{opacity:1;}50%{opacity:.3;}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.96);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime) 40%,rgba(168,255,62,.2) 70%,transparent);opacity:.35;}
.tb-logo{display:flex;align-items:center;gap:9px;padding:0 16px;height:100%;border-right:1px solid var(--rim);min-width:180px;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;display:flex;align-items:center;justify-content:center;box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
.tb-a{font-family:var(--mono);font-size:9px;letter-spacing:.4px;padding:4px 9px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--gold-d);border-color:var(--gold-b);color:var(--gold);}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── PAGE ── */
.page{position:relative;z-index:1;padding:12px 14px;display:flex;flex-direction:column;gap:10px;
  min-height:calc(100vh - var(--topbar-h));}

/* ── KPI GRID ── */
.kpi-grid{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:8px;animation:fu .35s .05s ease both;}
@media(max-width:1100px){.kpi-grid{grid-template-columns:repeat(3,1fr);}}
@media(max-width:640px){.kpi-grid{grid-template-columns:repeat(2,1fr);}}

.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:13px 14px;position:relative;overflow:hidden;transition:border-color .2s;}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;}
.kpi:hover{border-color:var(--rim-hi);}
.kpi-label{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:8px;
  display:flex;align-items:center;gap:5px;}
.kpi-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.kpi-val{font-family:var(--mono);font-size:20px;font-weight:600;line-height:1;letter-spacing:-.5px;}
.kpi-sub{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:5px;}

/* ── CARD ── */
.card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;animation:fu .35s .1s ease both;flex:1;}
.card-hd{flex-shrink:0;display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:0 14px;height:38px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.card-hd-l{display:flex;align-items:center;gap:8px;}
.hd-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.card-title{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.hd-badge{font-family:var(--mono);font-size:8px;padding:2px 8px;border-radius:5px;
  border:1px solid var(--blue-b);color:var(--blue);background:var(--blue-d);}
.hd-upd{font-family:var(--mono);font-size:8px;color:var(--t3);}

/* ── TOOLBAR ── */
.tbl-toolbar{display:flex;align-items:center;gap:6px;flex-wrap:wrap;
  padding:9px 12px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}

/* 마켓 탭 */
.mkt-tabs{display:flex;gap:3px;}
.mkt-tab{height:28px;padding:0 11px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-tab.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-tab.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.mkt-tab.all{background:var(--purple-d);border-color:var(--purple-b);color:var(--purple);}

/* 검색 */
.tbl-search{flex:1;min-width:180px;height:28px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;
  padding:0 10px;outline:none;transition:border-color .15s;}
.tbl-search:focus{border-color:var(--lime-b);}
.tbl-search::placeholder{color:var(--t3);}

/* 사이드 필터 */
.side-btns{display:flex;gap:3px;}
.side-btn{height:28px;padding:0 10px;font-family:var(--mono);font-size:9px;letter-spacing:.3px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.side-btn.all {background:var(--panel-hi);border-color:var(--rim-hi);color:var(--t2);}
.side-btn.buy {background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.side-btn.sell{background:var(--red-d);border-color:var(--red-b);color:var(--red);}

.ref-btn{height:28px;padding:0 11px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t2);
  cursor:pointer;transition:all .12s;display:inline-flex;align-items:center;gap:5px;}
.ref-btn:hover{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.ref-btn svg{width:10px;height:10px;flex-shrink:0;}
.ref-btn.loading svg{animation:spin .6s linear infinite;}

/* ── TABLE ── */
.tbl-wrap{overflow:auto;flex:1;
  scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.tbl-wrap::-webkit-scrollbar{width:3px;height:3px;}
.tbl-wrap::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}

table{width:100%;border-collapse:collapse;}
thead th{position:sticky;top:0;z-index:2;
  background:var(--panel-hi);border-bottom:1px solid var(--rim);
  font-family:var(--mono);font-size:7px;font-weight:500;color:var(--t3);
  letter-spacing:1.2px;text-transform:uppercase;padding:8px 12px;
  text-align:left;white-space:nowrap;}
tbody td{padding:10px 12px;border-bottom:1px solid var(--t4);vertical-align:middle;white-space:nowrap;}
tbody tr:last-child td{border-bottom:none;}
tbody tr:hover td{background:var(--hover);}
tbody tr:hover .td-id{color:var(--t2);}

.td-id{font-family:var(--mono);font-size:9px;color:var(--t3);transition:color .1s;}
.td-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);line-height:1.3;}
.td-nm {font-size:10px;color:var(--t2);margin-top:2px;}
.td-price{font-family:var(--mono);font-size:12px;color:var(--gold);font-weight:600;}
.td-amt{font-family:var(--mono);font-size:11px;color:var(--t1);}
.td-reason{font-size:10px;color:var(--t2);max-width:200px;overflow:hidden;text-overflow:ellipsis;}
.td-time{font-family:var(--mono);font-size:9px;color:var(--t3);}
.td-r{text-align:right;}

.side-badge{display:inline-flex;align-items:center;
  font-family:var(--mono);font-size:8px;font-weight:700;letter-spacing:.3px;
  padding:2px 8px;border-radius:4px;white-space:nowrap;}
.sb-buy {color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.sb-sell{color:var(--red);    background:var(--red-d);    border:1px solid var(--red-b);}

.tbl-empty{text-align:center;padding:48px!important;
  font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:1.5px;}

/* ── PAGER ── */
.pager{display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:8px 12px;border-top:1px solid var(--rim);background:var(--panel-hi);flex-shrink:0;}
.page-info{font-family:var(--mono);font-size:9px;color:var(--t2);}
.pager-btns{display:flex;gap:4px;}
.pg-btn{height:26px;padding:0 10px;font-family:var(--mono);font-size:9px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .12s;}
.pg-btn:hover{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.pg-btn:disabled{opacity:.3;cursor:default;}
.pg-btn:disabled:hover{border-color:var(--rim-hi);color:var(--t2);background:transparent;}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mk">
      <svg viewBox="0 0 24 24" fill="none" stroke="#060709" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-name">AUTO<span>TRADE</span></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-sp"></div>
  <div class="tb-nav">
    <a class="tb-a"     href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- KPI GRID -->
  <div class="kpi-grid" id="kpiGrid">
    <div class="kpi" id="kpiTotalCard" style="--ac:var(--lime);">
      <div class="kpi::before" style="background:var(--lime)"></div>
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--lime)"></div>총 주문</div>
      <div class="kpi-val" id="kpiTotal" style="color:var(--lime)">—</div>
      <div class="kpi-sub" id="kpiTotalSub">전체 체결 건수</div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--emerald)"></div>매수</div>
      <div class="kpi-val" id="kpiBuy" style="color:var(--emerald)">—</div>
      <div class="kpi-sub" id="kpiBuySub">BUY 주문</div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--red)"></div>매도</div>
      <div class="kpi-val" id="kpiSell" style="color:var(--red)">—</div>
      <div class="kpi-sub" id="kpiSellSub">SELL 주문</div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--gold)"></div>총 매수금액</div>
      <div class="kpi-val" id="kpiBuyAmt" style="color:var(--gold)">—</div>
      <div class="kpi-sub">매수 체결 합산</div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--blue)"></div>총 매도금액</div>
      <div class="kpi-val" id="kpiSellAmt" style="color:var(--blue)">—</div>
      <div class="kpi-sub">매도 체결 합산</div>
    </div>
    <!-- [FIX] PnL 카드: 동적 색상을 CSS 변수가 아닌 inline style로 직접 변경 -->
    <div class="kpi" id="kpiPnlCard">
      <div class="kpi-label"><div class="kpi-dot" id="kpiPnlDot" style="background:var(--purple)"></div>순손익 (추정)</div>
      <div class="kpi-val" id="kpiPnl" style="color:var(--purple)">—</div>
      <div class="kpi-sub" id="kpiPnlSub">매도 − 매수 금액</div>
    </div>
  </div>

  <!-- ORDER TABLE -->
  <div class="card">
    <div class="card-hd">
      <div class="card-hd-l">
        <div class="hd-dot" style="background:var(--gold)"></div>
        <span class="card-title">Order History</span>
        <span class="hd-badge" id="totalBadge">0</span>
      </div>
      <div style="display:flex;align-items:center;gap:8px;">
        <span class="hd-upd">업데이트 <span id="lastUpdated">—</span></span>
      </div>
    </div>

    <div class="tbl-toolbar">
      <div class="mkt-tabs">
        <button class="mkt-tab kr" id="tabKR"  onclick="setMarket('KR')">🇰🇷 KR</button>
        <button class="mkt-tab"    id="tabUS"  onclick="setMarket('US')">🇺🇸 US</button>
        <button class="mkt-tab"    id="tabALL" onclick="setMarket('ALL')">ALL</button>
      </div>

      <input class="tbl-search" id="searchInput" placeholder="종목코드 / 종목명 / reason 검색…" oninput="applyFilter()">

      <div class="side-btns">
        <button class="side-btn all"  id="btnAll"  onclick="setSide('ALL')">ALL</button>
        <button class="side-btn"      id="btnBUY"  onclick="setSide('BUY')">BUY</button>
        <button class="side-btn"      id="btnSELL" onclick="setSide('SELL')">SELL</button>
      </div>

      <button class="ref-btn" id="refBtn" onclick="loadOrders()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
             stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"/>
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
        </svg>
        Refresh
      </button>
    </div>

    <div class="tbl-wrap">
      <table>
        <thead>
          <tr>
            <th style="width:50px;">ID</th>
            <th style="width:140px;">종목</th>
            <th style="width:62px;">구분</th>
            <th style="width:50px;" class="td-r">수량</th>
            <th style="width:120px;" class="td-r">체결가</th>
            <th style="width:130px;" class="td-r">체결금액</th>
            <th>사유</th>
            <th style="width:148px;">시각</th>
          </tr>
        </thead>
        <tbody id="ordBody">
          <tr><td colspan="8" class="tbl-empty">로딩 중…</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <span class="page-info" id="pageInfo">0 / 0</span>
      <div class="pager-btns">
        <button class="pg-btn" id="btnPrev" onclick="movePage(-1)">← Prev</button>
        <button class="pg-btn" id="btnNext" onclick="movePage(1)">Next →</button>
      </div>
    </div>
  </div>

</div>

<script>
'use strict';

const B       = '${pageContext.request.contextPath}';
const DAYS    = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const PAGE_SZ = 30;

let rawOrders  = [];
let sideFilter = 'ALL';
let mktFilter  = 'KR';
let curPage    = 1;

/* ── 시계 ── */
function p2(v){return String(v).padStart(2,'0');}
function tick(){
  const n=new Date();
  document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000); tick();

/* ── XSS 방지 ── */
function esc(s){
  return String(s==null?'':s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

/* ── 통화 포맷: KR=원, US=달러 구분 ── */
const nfKR = new Intl.NumberFormat('ko-KR');
const nfUS = new Intl.NumberFormat('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});

function fmtPrice(v){
  const n=Number(String(v||0).replace(/,/g,''))||0;
  if(mktFilter==='US') return '$'+nfUS.format(n);
  return nfKR.format(Math.round(n));
}

function fmtAmt(v){
  const n=Number(String(v||0).replace(/,/g,''))||0;
  if(mktFilter==='US'){
    if(Math.abs(n)>=1e6) return '$'+(n/1e6).toFixed(2)+'M';
    if(Math.abs(n)>=1e3) return '$'+(n/1e3).toFixed(1)+'K';
    return '$'+nfUS.format(n);
  }
  if(Math.abs(n)>=1e8) return (n/1e8).toFixed(2)+'억';
  if(Math.abs(n)>=1e4) return (n/1e4).toFixed(1)+'만';
  return nfKR.format(Math.round(n));
}

function fmtSigned(v){
  const n=Number(String(v||0).replace(/,/g,''))||0;
  const s=fmtAmt(Math.abs(n));
  return (n>=0?'+':'-')+(mktFilter==='US'?s:s);
}

/* ── 시간 포맷 ── */
function fmtTime(v){
  if(Array.isArray(v)&&v.length>=6)
    return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+' '+p2(v[3])+':'+p2(v[4])+':'+p2(v[5]);
  if(typeof v==='string')
    return v.replace('T',' ').substring(0,19);
  return '—';
}

/* ── 종목명 캐시: 응답에서 먼저 가져오고, 없으면 API 1회 호출 후 재사용 ── */
const nameCache = Object.create(null);
const nameFetchingSet = new Set(); /* [FIX] 중복 요청 방지 */

function ensureNames(symbols){
  /* [FIX] 이미 캐시에 있거나 요청 중인 심볼 제외 */
  const missing = [...new Set(symbols)].filter(s=>s && nameCache[s]===undefined && !nameFetchingSet.has(s));
  if(!missing.length) return Promise.resolve();
  missing.forEach(s=>nameFetchingSet.add(s));
  return Promise.all(missing.map(s=>
    fetch(B+'/api/watchlist/name?symbol='+encodeURIComponent(s))
      .then(r=>r.json())
      .then(d=>{ nameCache[s]=d.symbolName||''; })
      .catch(()=>{ nameCache[s]=''; })
      .finally(()=>nameFetchingSet.delete(s))
  ));
}

/* ── KPI ── */
function calcKpi(orders){
  const buy  = orders.filter(r=>r.side==='BUY');
  const sell = orders.filter(r=>r.side==='SELL');
  const buyAmt  = buy .reduce((s,r)=>s+(Number(r.price||0)*Number(r.quantity||0)),0);
  const sellAmt = sell.reduce((s,r)=>s+(Number(r.price||0)*Number(r.quantity||0)),0);
  const pnl = sellAmt-buyAmt;

  document.getElementById('kpiTotal').textContent   = orders.length.toLocaleString();
  document.getElementById('kpiBuy').textContent     = buy.length.toLocaleString();
  document.getElementById('kpiSell').textContent    = sell.length.toLocaleString();
  document.getElementById('kpiBuyAmt').textContent  = fmtAmt(buyAmt);
  document.getElementById('kpiSellAmt').textContent = fmtAmt(sellAmt);
  document.getElementById('kpiTotalSub').textContent= mktFilter+' · '+orders.length+'건';
  document.getElementById('kpiBuySub').textContent  = buy.length+'건 · '+fmtAmt(buyAmt);
  document.getElementById('kpiSellSub').textContent = sell.length+'건 · '+fmtAmt(sellAmt);

  const pnlEl  = document.getElementById('kpiPnl');
  const pnlDot = document.getElementById('kpiPnlDot');
  const pnlSub = document.getElementById('kpiPnlSub');

  /* [FIX] inline style로 직접 변경해야 CSS 변수 계층 문제 없이 반영됨 */
  const col = pnl>0?'var(--emerald)':pnl<0?'var(--red)':'var(--purple)';
  pnlEl.style.color  = col;
  pnlDot.style.background = col;
  pnlEl.textContent  = (pnl>=0?'+':'')+fmtAmt(pnl);
  pnlSub.textContent = '매도 − 매수 (추정 · '+mktFilter+')';
}

/* ── 필터 ── */
function filterOrders(){
  const q=(document.getElementById('searchInput').value||'').toLowerCase().trim();
  return rawOrders.filter(r=>{
    if(sideFilter!=='ALL'&&r.side!==sideFilter) return false;
    if(!q) return true;
    const txt=((r.symbol||'')+' '+(r.symbolName||nameCache[r.symbol]||'')+' '+(r.reason||'')).toLowerCase();
    return txt.includes(q);
  });
}

/* ── 테이블 렌더 ── */
function renderTable(){
  const filtered  = filterOrders();
  const pageCount = Math.max(1,Math.ceil(filtered.length/PAGE_SZ));
  curPage = Math.min(Math.max(curPage,1),pageCount);
  const rows = filtered.slice((curPage-1)*PAGE_SZ, curPage*PAGE_SZ);

  document.getElementById('totalBadge').textContent=filtered.length;
  document.getElementById('pageInfo').textContent=
    (filtered.length?(curPage-1)*PAGE_SZ+1:0)+' – '+Math.min(curPage*PAGE_SZ,filtered.length)+' / '+filtered.length;
  document.getElementById('btnPrev').disabled = curPage<=1;
  document.getElementById('btnNext').disabled = curPage>=pageCount;

  const body=document.getElementById('ordBody');
  if(!rows.length){
    body.innerHTML='<tr><td colspan="8" class="tbl-empty">조건에 맞는 주문이 없습니다</td></tr>';
    return;
  }

  /* 응답에 symbolName 있으면 캐시 선반영 */
  rows.forEach(r=>{if(r.symbol&&r.symbolName) nameCache[r.symbol]=r.symbolName;});

  /* 캐시 미스 심볼만 보완 요청 */
  const missing=rows.map(r=>r.symbol).filter(s=>s&&nameCache[s]===undefined);
  ensureNames(missing).then(()=>{
    body.innerHTML=rows.map(r=>{
      const sym = esc(r.symbol||'—');
      const nm  = esc(nameCache[r.symbol]||r.symbolName||'');
      const isBuy = r.side==='BUY';
      const price = r.price?fmtPrice(r.price):'—';
      const amt   = (Number(r.price||0)*Number(r.quantity||0));
      const amtStr= amt>0?fmtAmt(amt):'—';
      return '<tr>'
        +'<td class="td-id">#'+esc(r.id)+'</td>'
        +'<td><div class="td-sym">'+sym+'</div>'+(nm?'<div class="td-nm">'+nm+'</div>':'')+'</td>'
        +'<td><span class="side-badge '+(isBuy?'sb-buy':'sb-sell')+'">'+esc(r.side||'—')+'</span></td>'
        +'<td class="td-r" style="font-family:var(--mono);">'+esc(r.quantity||'—')+'</td>'
        +'<td class="td-r td-price">'+price+'</td>'
        +'<td class="td-r td-amt">'+amtStr+'</td>'
        +'<td class="td-reason" title="'+esc(r.reason||'')+'">'+esc(r.reason||'—')+'</td>'
        +'<td class="td-time">'+esc(fmtTime(r.createdAt))+'</td>'
        +'</tr>';
    }).join('');
  });
}

/* ── 마켓·사이드 전환 ── */
window.setMarket=function(m){
  mktFilter=m;
  document.getElementById('tabKR').className ='mkt-tab'+(m==='KR'?' kr':'');
  document.getElementById('tabUS').className ='mkt-tab'+(m==='US'?' us':'');
  document.getElementById('tabALL').className='mkt-tab'+(m==='ALL'?' all':'');
  loadOrders();
};

window.setSide=function(s){
  sideFilter=s;
  document.getElementById('btnAll').className ='side-btn'+(s==='ALL'?' all':'');
  document.getElementById('btnBUY').className ='side-btn'+(s==='BUY'?' buy':'');
  document.getElementById('btnSELL').className='side-btn'+(s==='SELL'?' sell':'');
  curPage=1; renderTable();
};

window.applyFilter=function(){curPage=1;renderTable();};
window.movePage   =function(d){curPage+=d;renderTable();};

/* ── 주문 로드 ── */
window.loadOrders=function(){
  const btn=document.getElementById('refBtn');
  btn.classList.add('loading');

  const ep = mktFilter==='US'?'/api/orders/us':mktFilter==='ALL'?'/api/orders':'/api/orders/kr';

  fetch(B+ep)
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
    .then(rows=>{
      rawOrders=Array.isArray(rows)?rows:[];
      curPage=1;
      calcKpi(rawOrders);
      renderTable();
      const n=new Date();
      document.getElementById('lastUpdated').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
    })
    .catch(e=>{
      console.error(e);
      document.getElementById('ordBody').innerHTML='<tr><td colspan="8" class="tbl-empty">주문 조회 실패: '+esc(e.message)+'</td></tr>';
    })
    .finally(()=>btn.classList.remove('loading'));
};

loadOrders();
</script>
</body>
</html>
