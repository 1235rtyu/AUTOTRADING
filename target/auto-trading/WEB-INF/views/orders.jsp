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
  --void:#07090f;--base:#0d0f18;--panel:#13161f;--panel-hi:#181b27;--hover:#1d2130;
  --lime:#c6ff5e;--lime-d:rgba(198,255,94,.1);--lime-b:rgba(198,255,94,.25);
  --emerald:#00e07a;--emerald-d:rgba(0,224,122,.08);--emerald-b:rgba(0,224,122,.22);
  --red:#ff5070;--red-d:rgba(255,80,112,.08);--red-b:rgba(255,80,112,.25);
  --gold:#ffc940;--gold-d:rgba(255,201,64,.08);--gold-b:rgba(255,201,64,.22);
  --blue:#5ba3ff;--blue-d:rgba(91,163,255,.08);--blue-b:rgba(91,163,255,.22);
  --purple:#c084fc;--purple-d:rgba(192,132,252,.08);--purple-b:rgba(192,132,252,.22);
  --rim:rgba(255,255,255,.07);--rim-hi:rgba(255,255,255,.13);
  --t1:#dde4f0;--t2:#8592ad;--t3:#444d63;--t4:#1a1e2c;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
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
@keyframes fadeIn{from{opacity:0;}to{opacity:1;}}

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
.tb-a.cur{background:var(--gold-d);border-color:var(--gold-b);color:var(--gold);}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:.8px;}

/* ── PAGE ── */
.page{position:relative;z-index:1;padding:14px;display:flex;flex-direction:column;gap:12px;
  min-height:calc(100vh - var(--topbar-h));}

/* ── KPI GRID ── */
.kpi-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;animation:fu .3s .04s ease both;}
@media(max-width:1200px){.kpi-grid{grid-template-columns:repeat(3,1fr);}}
@media(max-width:700px){.kpi-grid{grid-template-columns:repeat(2,1fr);}}

.kpi-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:14px 16px;position:relative;overflow:hidden;transition:border-color .2s;}
.kpi-card::after{content:'';position:absolute;top:0;left:0;right:0;height:2px;
  background:var(--accent-line,transparent);border-radius:var(--r3) var(--r3) 0 0;}
.kpi-card:hover{border-color:var(--rim-hi);}

.kpi-label{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.2px;text-transform:uppercase;margin-bottom:10px;
  display:flex;align-items:center;gap:5px;}
.kpi-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.kpi-val{font-family:var(--mono);font-size:22px;font-weight:600;color:var(--t1);
  line-height:1;letter-spacing:-.5px;}
.kpi-sub{font-family:var(--mono);font-size:9px;color:var(--t3);margin-top:6px;}
.kpi-badge{display:inline-flex;align-items:center;font-family:var(--mono);font-size:9px;
  padding:2px 7px;border-radius:5px;margin-top:6px;}

.kc-lime{--accent-line:var(--lime);}
.kc-lime .kpi-val{color:var(--lime);}
.kc-emerald{--accent-line:var(--emerald);}
.kc-emerald .kpi-val{color:var(--emerald);}
.kc-red{--accent-line:var(--red);}
.kc-red .kpi-val{color:var(--red);}
.kc-gold{--accent-line:var(--gold);}
.kc-gold .kpi-val{color:var(--gold);}
.kc-blue{--accent-line:var(--blue);}
.kc-blue .kpi-val{color:var(--blue);}
.kc-purple{--accent-line:var(--purple);}
.kc-purple .kpi-val{color:var(--purple);}

/* ── CARD ── */
.card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;animation:fu .3s .1s ease both;}
.card-hd{flex-shrink:0;display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:0 14px;height:38px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.card-hd-l{display:flex;align-items:center;gap:8px;}
.hd-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.card-title{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.5px;text-transform:uppercase;}

/* ── TOOLBAR ── */
.tbl-toolbar{display:flex;align-items:center;gap:6px;flex-wrap:wrap;
  padding:10px 14px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.tbl-search{flex:1;min-width:200px;height:30px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;
  padding:0 10px;outline:none;transition:border-color .15s;}
.tbl-search:focus{border-color:var(--lime-b);}
.tbl-search::placeholder{color:var(--t3);}
.side-btns{display:flex;gap:2px;}
.side-btn{height:30px;padding:0 11px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t2);cursor:pointer;transition:all .12s;}
.side-btn.act    {background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.side-btn.act-buy {background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.side-btn.act-sell{background:var(--red-d);border-color:var(--red-b);color:var(--red);}
.tb-sel{height:30px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t2);font-family:var(--mono);font-size:10px;
  padding:0 8px;outline:none;cursor:pointer;}
.tb-sel option{background:var(--panel-hi);}

/* ── MARKET TABS ── */
.mkt-tabs{display:flex;gap:4px;}
.mkt-tab{height:30px;padding:0 12px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .12s;}
.mkt-tab.active-kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-tab.active-us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}

/* ── TABLE ── */
.tbl-wrap{overflow:auto;flex:1;
  scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.tbl-wrap::-webkit-scrollbar{width:3px;height:3px;}
.tbl-wrap::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
table{width:100%;border-collapse:collapse;font-size:11px;}
thead th{position:sticky;top:0;z-index:2;
  background:var(--panel-hi);border-bottom:1px solid var(--rim);
  font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.2px;text-transform:uppercase;padding:8px 12px;
  text-align:left;white-space:nowrap;}
tbody td{padding:10px 12px;border-bottom:1px solid var(--t4);vertical-align:middle;white-space:nowrap;}
tbody tr:last-child td{border-bottom:none;}
tbody tr:hover td{background:var(--hover);}
.td-num{text-align:right;font-family:var(--mono);}
.td-id{font-family:var(--mono);font-size:10px;color:var(--t3);}
.td-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);line-height:1.3;}
.td-nm{font-size:10px;color:var(--t2);margin-top:1px;}
.td-price{font-family:var(--mono);font-size:12px;color:var(--gold);font-weight:600;}
.td-reason{font-size:10px;color:var(--t2);max-width:220px;overflow:hidden;text-overflow:ellipsis;}
.td-time{font-family:var(--mono);font-size:10px;color:var(--t3);}
.side-badge{display:inline-flex;align-items:center;
  font-family:var(--mono);font-size:9px;font-weight:700;letter-spacing:.3px;
  padding:2px 8px;border-radius:4px;}
.side-buy {color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.side-sell{color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.tbl-empty{text-align:center;padding:40px!important;font-family:var(--mono);font-size:10px;
  color:var(--t3);letter-spacing:1px;}

/* ── PAGER ── */
.pager{display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:9px 14px;border-top:1px solid var(--rim);background:var(--panel-hi);flex-shrink:0;}
.page-info{font-family:var(--mono);font-size:9px;color:var(--t2);}
.pager-btns{display:flex;gap:4px;}

/* ── BTN ── */
.btn{height:30px;padding:0 12px;border-radius:var(--r);border:1px solid transparent;
  cursor:pointer;font-family:var(--mono);font-size:9px;font-weight:600;letter-spacing:.5px;
  display:inline-flex;align-items:center;gap:5px;transition:all .14s;white-space:nowrap;}
.btn:hover{filter:brightness(1.12);}
.btn:active{transform:scale(.97);}
.btn svg{width:10px;height:10px;flex-shrink:0;}
.btn-ghost{background:transparent;border-color:var(--rim-hi);color:var(--t2);}
.btn-ghost:hover{border-color:var(--rim-hi);color:var(--t1);background:var(--hover);}
.btn-lime{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.btn-lime:hover{background:var(--lime);color:var(--void);}

/* ── BADGE ── */
.badge{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:5px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.badge.ok {color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.badge.warn{color:var(--gold);border-color:var(--gold-b);background:var(--gold-d);}
.badge.cnt {color:var(--blue);border-color:var(--blue-b);background:var(--blue-d);}
.badge.lime{color:var(--lime);border-color:var(--lime-b);background:var(--lime-d);}

/* ── TOAST ── */
.toast{position:fixed;right:14px;bottom:14px;z-index:999;
  max-width:360px;background:var(--panel-hi);border:1px solid var(--rim-hi);
  color:var(--t1);border-radius:10px;padding:10px 14px;
  font-family:var(--mono);font-size:11px;letter-spacing:.3px;
  opacity:0;transform:translateY(8px);pointer-events:none;transition:.2s;}
.toast.show{opacity:1;transform:translateY(0);}
.toast.ok {border-color:var(--emerald-b);color:var(--emerald);}
.toast.err{border-color:var(--red-b);color:var(--red);}
</style>
</head>
<body>
<div class="bg-grid"></div>

<!-- ── TOPBAR ── -->
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
    <a class="tb-a" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- ── KPI CARDS ── -->
  <div class="kpi-grid" id="kpiGrid">
    <div class="kpi-card kc-lime">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--lime)"></div>총 주문</div>
      <div class="kpi-val" id="kpiTotal">—</div>
      <div class="kpi-sub" id="kpiTotalSub">전체 체결 건수</div>
    </div>
    <div class="kpi-card kc-emerald">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--emerald)"></div>매수</div>
      <div class="kpi-val" id="kpiBuy">—</div>
      <div class="kpi-sub" id="kpiBuySub">BUY 주문</div>
    </div>
    <div class="kpi-card kc-red">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--red)"></div>매도</div>
      <div class="kpi-val" id="kpiSell">—</div>
      <div class="kpi-sub" id="kpiSellSub">SELL 주문</div>
    </div>
    <div class="kpi-card kc-gold">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--gold)"></div>총 매수금액</div>
      <div class="kpi-val" id="kpiBuyAmt">—</div>
      <div class="kpi-sub">매수 체결 합산</div>
    </div>
    <div class="kpi-card kc-blue">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--blue)"></div>총 매도금액</div>
      <div class="kpi-val" id="kpiSellAmt">—</div>
      <div class="kpi-sub">매도 체결 합산</div>
    </div>
    <div class="kpi-card kc-purple">
      <div class="kpi-label"><div class="kpi-dot" style="background:var(--purple)"></div>순손익 (추정)</div>
      <div class="kpi-val" id="kpiPnl">—</div>
      <div class="kpi-sub">매도-매수 금액 기준</div>
    </div>
  </div>

  <!-- ── ORDER TABLE CARD ── -->
  <div class="card" style="flex:1;">
    <div class="card-hd">
      <div class="card-hd-l">
        <div class="hd-dot" style="background:var(--gold)"></div>
        <span class="card-title">Order History</span>
        <span class="badge cnt" id="totalBadge">0</span>
      </div>
      <div style="display:flex;align-items:center;gap:6px;">
        <span class="badge" id="srcBadge" style="font-size:7px;">KR</span>
        <span class="badge" id="lastUpdated">--:--:--</span>
      </div>
    </div>

    <div class="tbl-toolbar">
      <!-- 시장 탭 -->
      <div class="mkt-tabs">
        <button class="mkt-tab active-kr" id="tabKR" onclick="setMarket('KR')">KR</button>
        <button class="mkt-tab" id="tabUS" onclick="setMarket('US')">US</button>
        <button class="mkt-tab" id="tabALL" onclick="setMarket('ALL')">ALL</button>
      </div>

      <input class="tbl-search" id="searchInput" placeholder="종목코드 / 종목명 / reason 검색..." oninput="applyFilter()"/>

      <!-- 매수/매도 필터 -->
      <div class="side-btns">
        <button class="side-btn act"     id="btnAll"  onclick="setSide('ALL')">ALL</button>
        <button class="side-btn"         id="btnBUY"  onclick="setSide('BUY')">BUY</button>
        <button class="side-btn"         id="btnSELL" onclick="setSide('SELL')">SELL</button>
      </div>

      <button class="btn btn-ghost" id="refBtn" onclick="loadOrders()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
        Refresh
      </button>
    </div>

    <div class="tbl-wrap" id="tblWrap">
      <table>
        <thead>
          <tr>
            <th style="width:52px;">ID</th>
            <th style="width:130px;">종목</th>
            <th style="width:64px;">구분</th>
            <th style="width:52px;text-align:right;">수량</th>
            <th style="width:120px;text-align:right;">체결가</th>
            <th style="width:130px;text-align:right;">체결금액</th>
            <th>사유</th>
            <th style="width:150px;">시각</th>
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
        <button class="btn btn-ghost" onclick="movePage(-1)">← Prev</button>
        <button class="btn btn-ghost" onclick="movePage(1)">Next →</button>
      </div>
    </div>
  </div>

</div><!-- /page -->
<div class="toast" id="toast"></div>

<script>
'use strict';

const B      = '${pageContext.request.contextPath}';
const DAYS   = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
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
setInterval(tick,1000);tick();

/* ── 유틸 ── */
function esc(s){return String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function toast(msg,type){
  const el=document.getElementById('toast');
  el.textContent=msg;el.className='toast show '+(type||'');
  clearTimeout(el._t);el._t=setTimeout(()=>el.className='toast',2800);
}
function fmtAmt(v){
  const n=Number(String(v||0).replace(/,/g,''))||0;
  if(Math.abs(n)>=1e8) return (n/1e8).toFixed(1)+'억';
  if(Math.abs(n)>=1e4) return (n/1e4).toFixed(1)+'만';
  return n.toLocaleString();
}
function fmtTime(v){
  if(Array.isArray(v)&&v.length>=6)return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+' '+p2(v[3])+':'+p2(v[4])+':'+p2(v[5]);
  if(typeof v==='string')return v.replace('T',' ').substring(0,19);
  return '-';
}

/* ── 종목명: response의 symbolName 필드 직접 사용, 없으면 watchlist API fallback ── */
const nameCache={};
function fetchNameFallback(sym){
  if(nameCache[sym]!==undefined) return Promise.resolve(nameCache[sym]);
  return fetch(B+'/api/watchlist/name?symbol='+encodeURIComponent(sym))
    .then(r=>r.json()).then(d=>{ nameCache[sym]=d.symbolName||''; return nameCache[sym]; })
    .catch(()=>{ nameCache[sym]=''; return ''; });
}

/* ── KPI 계산 & 렌더 ── */
function calcKpi(orders){
  const buy  = orders.filter(r=>r.side==='BUY');
  const sell = orders.filter(r=>r.side==='SELL');
  const buyAmt  = buy.reduce((s,r)=>s+(Number(r.price||0)*Number(r.quantity||0)),0);
  const sellAmt = sell.reduce((s,r)=>s+(Number(r.price||0)*Number(r.quantity||0)),0);
  const pnl = sellAmt - buyAmt;

  document.getElementById('kpiTotal').textContent   = orders.length.toLocaleString();
  document.getElementById('kpiBuy').textContent     = buy.length.toLocaleString();
  document.getElementById('kpiSell').textContent    = sell.length.toLocaleString();
  document.getElementById('kpiBuyAmt').textContent  = fmtAmt(buyAmt);
  document.getElementById('kpiSellAmt').textContent = fmtAmt(sellAmt);

  const pnlEl = document.getElementById('kpiPnl');
  pnlEl.textContent = (pnl>=0?'+':'')+fmtAmt(pnl);
  pnlEl.style.color = pnl>0?'var(--emerald)':pnl<0?'var(--red)':'var(--t2)';

  // 카드 색상도 pnl에 맞게
  const pnlCard = pnlEl.closest('.kpi-card');
  pnlCard.className = 'kpi-card '+(pnl>0?'kc-emerald':pnl<0?'kc-red':'kc-purple');

  document.getElementById('kpiTotalSub').textContent = mktFilter+' 시장 기준';
  document.getElementById('kpiBuySub').textContent   = buy.length+'건 / '+fmtAmt(buyAmt)+'원';
  document.getElementById('kpiSellSub').textContent  = sell.length+'건 / '+fmtAmt(sellAmt)+'원';
}

/* ── 필터 적용 ── */
function filterOrders(){
  const q=(document.getElementById('searchInput').value||'').toLowerCase();
  return rawOrders.filter(r=>{
    const sideOk = sideFilter==='ALL'||r.side===sideFilter;
    const txt = ((r.symbol||'')+' '+(r.symbolName||nameCache[r.symbol]||'')+' '+(r.reason||'')).toLowerCase();
    return sideOk&&(!q||txt.includes(q));
  });
}

/* ── 테이블 렌더 ── */
function renderTable(){
  const filtered = filterOrders();
  const pageCount = Math.max(1, Math.ceil(filtered.length/PAGE_SZ));
  if(curPage>pageCount) curPage=pageCount;
  if(curPage<1) curPage=1;
  const rows = filtered.slice((curPage-1)*PAGE_SZ, curPage*PAGE_SZ);

  document.getElementById('totalBadge').textContent = filtered.length;
  document.getElementById('pageInfo').textContent =
    (filtered.length?(curPage-1)*PAGE_SZ+1:0)+'-'+
    Math.min(curPage*PAGE_SZ,filtered.length)+' / '+filtered.length;

  const body = document.getElementById('ordBody');
  if(!rows.length){
    body.innerHTML='<tr><td colspan="8" class="tbl-empty">조건에 맞는 주문이 없습니다</td></tr>';
    return;
  }

  // symbolName이 응답에 있으면 캐시에 저장
  rows.forEach(r=>{ if(r.symbol && r.symbolName) nameCache[r.symbol]=r.symbolName; });

  // 캐시에 없는 심볼만 watchlist API로 보완
  const missing=[...new Set(rows.map(r=>r.symbol).filter(s=>s&&!nameCache[s]))];
  Promise.all(missing.map(s=>fetchNameFallback(s))).then(()=>{
    body.innerHTML = rows.map(r=>{
      const side = r.side==='BUY'?'side-buy':'side-sell';
      const sym  = esc(r.symbol||'-');
      const nm   = nameCache[r.symbol] || r.symbolName || '';
      const amt  = (Number(r.price||0)*Number(r.quantity||0));
      const amtStr = amt>0 ? amt.toLocaleString() : '-';
      return '<tr>'
        +'<td class="td-id">#'+esc(r.id)+'</td>'
        +'<td><div class="td-sym">'+sym+'</div>'+(nm?'<div class="td-nm">'+esc(nm)+'</div>':'')+'</td>'
        +'<td><span class="side-badge '+side+'">'+esc(r.side||'-')+'</span></td>'
        +'<td class="td-num">'+esc(r.quantity||'-')+'</td>'
        +'<td class="td-num td-price">'+(r.price?Number(r.price).toLocaleString():'-')+'</td>'
        +'<td class="td-num" style="color:var(--t1);">'+amtStr+'</td>'
        +'<td class="td-reason" title="'+esc(r.reason||'')+'">'+esc(r.reason||'-')+'</td>'
        +'<td class="td-time">'+esc(fmtTime(r.createdAt))+'</td>'
      +'</tr>';
    }).join('');
  });
}

/* ── 시장 / 사이드 전환 ── */
window.setMarket = function(m){
  mktFilter=m;
  document.getElementById('tabKR').className  = 'mkt-tab'+(m==='KR'?' active-kr':'');
  document.getElementById('tabUS').className  = 'mkt-tab'+(m==='US'?' active-us':'');
  document.getElementById('tabALL').className = 'mkt-tab'+(m==='ALL'?' active-kr':'');
  document.getElementById('srcBadge').textContent = m;
  loadOrders();
};
window.setSide = function(s){
  sideFilter=s;
  document.getElementById('btnAll').className  = 'side-btn'+(s==='ALL'?' act':'');
  document.getElementById('btnBUY').className  = 'side-btn'+(s==='BUY'?' act-buy':'');
  document.getElementById('btnSELL').className = 'side-btn'+(s==='SELL'?' act-sell':'');
  curPage=1; renderTable();
};
window.applyFilter = function(){ curPage=1; renderTable(); };
window.movePage    = function(d){ curPage+=d; renderTable(); };

/* ── 주문 로드 ── */
window.loadOrders = function(){
  const btn = document.getElementById('refBtn');
  btn.innerHTML='<span style="display:inline-block;width:10px;height:10px;border:1.5px solid var(--rim-hi);border-top-color:var(--lime);border-radius:50%;animation:sd .5s linear infinite;vertical-align:middle;"></span>';

  let ep;
  if(mktFilter==='US')       ep = '/api/orders/us';
  else if(mktFilter==='ALL') ep = '/api/orders';
  else                        ep = '/api/orders/kr';

  fetch(B+ep)
    .then(r=>r.json())
    .then(rows=>{
      rawOrders = Array.isArray(rows)?rows:[];
      curPage   = 1;
      calcKpi(rawOrders);
      renderTable();
      const now=new Date();
      document.getElementById('lastUpdated').textContent=p2(now.getHours())+':'+p2(now.getMinutes())+':'+p2(now.getSeconds());
    })
    .catch(()=>toast('주문 조회 실패','err'))
    .finally(()=>{
      btn.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:10px;height:10px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg> Refresh';
    });
};

/* ── 초기 로드 ── */
loadOrders();
</script>
</body>
</html>
