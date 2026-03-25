<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Watchlist — AUTO TRADING</title>
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
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.05) 0%,transparent 55%),
    radial-gradient(ellipse 40% 50% at 100% 100%,rgba(0,217,126,.03) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.035) 1px,transparent 1px);
  background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.65);opacity:.25;}}
@keyframes spin{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes rowIn{from{opacity:0;transform:translateX(-6px);}to{opacity:1;transform:none;}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.96);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime) 40%,rgba(168,255,62,.2) 70%,transparent);opacity:.35;}
.tb-logo{display:flex;align-items:center;gap:9px;padding:0 16px;height:100%;
  border-right:1px solid var(--rim);min-width:180px;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;
  display:flex;align-items:center;justify-content:center;box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
.tb-a{font-family:var(--mono);font-size:9px;letter-spacing:.4px;padding:4px 9px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── PAGE ── */
.page{position:relative;z-index:1;padding:12px 14px;display:flex;flex-direction:column;gap:10px;
  min-height:calc(100vh - var(--topbar-h));}

/* ── TOOLBAR ── */
.toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:10px 12px;animation:fu .35s .05s ease both;}
.tb-label{font-family:var(--mono);font-size:7px;color:var(--t3);
  letter-spacing:1.5px;text-transform:uppercase;white-space:nowrap;}
.sym-in{height:30px;width:140px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:11px;
  letter-spacing:1px;padding:0 10px;outline:none;text-transform:uppercase;transition:border-color .15s;}
.sym-in:focus{border-color:var(--lime-b);}
.sym-in::placeholder{color:var(--t3);text-transform:none;}
/* 마켓 세그먼트 */
.mkt-seg{display:flex;gap:3px;}
.mkt-btn{height:30px;padding:0 11px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t3);
  cursor:pointer;transition:all .12s;}
.mkt-btn.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-btn.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
/* 추가 버튼 */
.add-btn{height:30px;padding:0 14px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--lime-b);border-radius:var(--r);background:var(--lime-d);color:var(--lime);
  cursor:pointer;transition:all .12s;display:inline-flex;align-items:center;gap:5px;}
.add-btn:hover{background:var(--lime);color:var(--void);}
.add-btn:disabled{opacity:.4;cursor:not-allowed;}
.add-btn:disabled:hover{background:var(--lime-d);color:var(--lime);}
.add-btn svg{width:10px;height:10px;flex-shrink:0;}
.add-btn.loading svg{animation:spin .6s linear infinite;}
/* 알림 */
.msg-chip{display:none;font-family:var(--mono);font-size:9px;padding:3px 10px;
  border-radius:5px;border:1px solid var(--rim);letter-spacing:.3px;}
.msg-chip.ok  {display:inline-flex;color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.msg-chip.err {display:inline-flex;color:var(--red);border-color:var(--red-b);background:var(--red-d);}
.msg-chip.info{display:inline-flex;color:var(--gold);border-color:var(--gold-b);background:var(--gold-d);}

/* ── 통계 배지 ── */
.stat-row{display:flex;align-items:center;gap:6px;flex-wrap:wrap;animation:fu .35s .1s ease both;}
.stat-badge{font-family:var(--mono);font-size:9px;padding:4px 10px;border-radius:6px;
  border:1px solid var(--rim);background:var(--panel);color:var(--t2);white-space:nowrap;}
.stat-badge b{color:var(--t1);}
.stat-badge.kr b{color:var(--lime);}
.stat-badge.us b{color:var(--blue);}
/* 검색 */
.search-in{height:28px;width:200px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;
  padding:0 10px;outline:none;transition:border-color .15s;margin-left:auto;}
.search-in:focus{border-color:var(--lime-b);}
.search-in::placeholder{color:var(--t3);}

/* ── GRID ── */
.panels{display:grid;grid-template-columns:1fr 1fr;gap:10px;animation:fu .35s .15s ease both;}
@media(max-width:900px){.panels{grid-template-columns:1fr;}}

/* ── PANEL ── */
.pn{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;}
.pn-hd{flex-shrink:0;display:flex;align-items:center;justify-content:space-between;
  padding:0 14px;height:38px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.pn-hd-l{display:flex;align-items:center;gap:7px;}
.pn-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;animation:pd 2s ease-in-out infinite;}
.pn-title{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.5px;text-transform:uppercase;}
.pn-count{font-family:var(--mono);font-size:8px;padding:2px 8px;border-radius:5px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.pn-count.kr{color:var(--lime);border-color:var(--lime-b);background:var(--lime-d);}
.pn-count.us{color:var(--blue);border-color:var(--blue-b);background:var(--blue-d);}

/* ── TABLE ── */
.tbl-wrap{overflow:auto;flex:1;max-height:500px;
  scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.tbl-wrap::-webkit-scrollbar{width:3px;}
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
tbody tr{animation:rowIn .2s ease both;}

.td-idx {font-family:var(--mono);font-size:9px;color:var(--t3);width:28px;}
.td-sym {font-family:var(--mono);font-size:11px;font-weight:700;color:var(--t1);}
.td-sym-kr{color:var(--lime);}
.td-sym-us{color:var(--blue);}
.td-name{font-size:11px;color:var(--t2);max-width:160px;overflow:hidden;text-overflow:ellipsis;}
.td-name.loading{color:var(--t3);font-style:italic;}
.td-date{font-family:var(--mono);font-size:9px;color:var(--t3);}

/* 삭제 버튼 */
.del-btn{height:24px;padding:0 9px;font-family:var(--mono);font-size:8px;letter-spacing:.3px;
  border:1px solid var(--red-b);border-radius:4px;background:var(--red-d);color:var(--red);
  cursor:pointer;transition:all .12s;}
.del-btn:hover{background:var(--red);color:#fff;}
.del-btn:disabled{opacity:.35;cursor:not-allowed;}

.tbl-empty{text-align:center;padding:40px!important;
  font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:1.5px;}
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
    <a class="tb-a"     href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- TOOLBAR -->
  <div class="toolbar">
    <span class="tb-label">Add Symbol</span>

    <!-- [FIX] 마켓 먼저 선택 → 입력 placeholder 자동 변경 -->
    <div class="mkt-seg">
      <button class="mkt-btn kr" id="mktKR" onclick="setAddMkt('KR')">🇰🇷 KR</button>
      <button class="mkt-btn"    id="mktUS" onclick="setAddMkt('US')">🇺🇸 US</button>
    </div>

    <input class="sym-in" id="symIn" placeholder="005930" maxlength="12" autocomplete="off" spellcheck="false">

    <button class="add-btn" id="addBtn" onclick="addSymbol()">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
      Add
    </button>

    <span class="msg-chip" id="msgChip"></span>
  </div>

  <!-- 통계 + 검색 -->
  <div class="stat-row">
    <span class="stat-badge">Total <b id="statTotal">0</b></span>
    <span class="stat-badge kr">KR <b id="statKR">0</b></span>
    <span class="stat-badge us">US <b id="statUS">0</b></span>
    <input class="search-in" id="searchIn" placeholder="종목코드 / 종목명 검색…" oninput="applySearch()">
  </div>

  <!-- PANELS -->
  <div class="panels">
    <section class="pn">
      <div class="pn-hd">
        <div class="pn-hd-l">
          <div class="pn-dot" style="background:var(--lime);box-shadow:0 0 6px rgba(168,255,62,.5);"></div>
          <span class="pn-title">한국 종목</span>
        </div>
        <span class="pn-count kr" id="krCount">0</span>
      </div>
      <div class="tbl-wrap">
        <table>
          <thead>
            <tr>
              <th style="width:28px;">#</th>
              <th>Symbol</th>
              <th>종목명</th>
              <th>등록일시</th>
              <th style="width:52px;"></th>
            </tr>
          </thead>
          <tbody id="krBody"><tr><td colspan="5" class="tbl-empty">로딩 중…</td></tr></tbody>
        </table>
      </div>
    </section>

    <section class="pn">
      <div class="pn-hd">
        <div class="pn-hd-l">
          <div class="pn-dot" style="background:var(--blue);box-shadow:0 0 6px rgba(77,159,255,.5);"></div>
          <span class="pn-title">미국 종목</span>
        </div>
        <span class="pn-count us" id="usCount">0</span>
      </div>
      <div class="tbl-wrap">
        <table>
          <thead>
            <tr>
              <th style="width:28px;">#</th>
              <th>Symbol</th>
              <th>종목명</th>
              <th>등록일시</th>
              <th style="width:52px;"></th>
            </tr>
          </thead>
          <tbody id="usBody"><tr><td colspan="5" class="tbl-empty">로딩 중…</td></tr></tbody>
        </table>
      </div>
    </section>
  </div>

</div>

<script>
'use strict';
(function(){
  const BASE = '${pageContext.request.contextPath}';
  const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  let allItems   = [];   /* 전체 캐시 */
  let nameCache  = Object.create(null); /* symbol → name */
  let fetchingSet = new Set();          /* [FIX] 중복 name 요청 방지 */
  let addMkt     = 'KR'; /* 추가할 마켓 */
  let searchQ    = '';

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
      .replace(/&/g,'&amp;').replace(/</g,'&lt;')
      .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#x27;');
  }

  /* ── 입력 검증 ── */
  const KR_PATTERN = /^[0-9]{6}$/;
  const US_PATTERN = /^[A-Z][A-Z0-9.\-]{0,9}$/;
  function validateSymbol(sym, mkt){
    if(!sym) return false;
    return mkt==='KR' ? KR_PATTERN.test(sym) : US_PATTERN.test(sym);
  }

  /* ── 마켓 판별 ── */
  function detectMkt(item){
    const ex=String(item.exchange||'').toUpperCase();
    if(ex==='KRX'||ex==='KR') return 'KR';
    if(['NAS','NYS','AMS','US'].includes(ex)) return 'US';
    return /^[A-Za-z]/.test(item.symbol) ? 'US' : 'KR';
  }

  /* ── 알림 ── */
  let msgTimer=null;
  function showMsg(txt, type){
    const el=document.getElementById('msgChip');
    el.textContent=txt;
    el.className='msg-chip '+(type==='ok'?'ok':type==='err'?'err':'info');
    clearTimeout(msgTimer);
    msgTimer=setTimeout(()=>el.className='msg-chip',3000);
  }

  /* ── 마켓 선택 ── */
  window.setAddMkt=function(m){
    addMkt=m;
    document.getElementById('mktKR').className='mkt-btn'+(m==='KR'?' kr':'');
    document.getElementById('mktUS').className='mkt-btn'+(m==='US'?' us':'');
    const inp=document.getElementById('symIn');
    inp.placeholder=m==='KR'?'005930':'AAPL';
    inp.value='';
    inp.focus();
  };

  /* ── 검색 ── */
  window.applySearch=function(){
    searchQ=document.getElementById('searchIn').value.toLowerCase().trim();
    renderAll(allItems);
  };

  /* ── 종목명 배치 로드: [FIX] 중복 방지 + 한 번에 처리 ── */
  function loadNames(symbols){
    const missing=[...new Set(symbols)].filter(s=>s&&nameCache[s]===undefined&&!fetchingSet.has(s));
    if(!missing.length) return Promise.resolve();
    missing.forEach(s=>fetchingSet.add(s));
    return Promise.all(missing.map(s=>
      fetch(BASE+'/api/watchlist/name?symbol='+encodeURIComponent(s))
        .then(r=>r.ok?r.json():null)
        .then(d=>{
          nameCache[s]=(d&&(d.symbolName||d.name)||'');
        })
        .catch(()=>{ nameCache[s]=''; })
        .finally(()=>fetchingSet.delete(s))
    ));
  }

  /* ── 테이블 렌더 ── */
  function renderRows(bodyId, items, mktCls){
    const body=document.getElementById(bodyId);

    /* 검색 필터 */
    const filtered=searchQ
      ? items.filter(it=>{
          const sym=(it.symbol||'').toLowerCase();
          const nm=(nameCache[it.symbol]||'').toLowerCase();
          return sym.includes(searchQ)||nm.includes(searchQ);
        })
      : items;

    if(!filtered.length){
      body.innerHTML='<tr><td colspan="5" class="tbl-empty">종목이 없습니다</td></tr>';
      return;
    }

    body.innerHTML=filtered.map((it,i)=>{
      const sym=esc(it.symbol||'—');
      const nm =esc(nameCache[it.symbol]||'');
      const dt =esc(formatDate(it.createdAt||it.addedAt||''));
      return '<tr style="animation-delay:'+i*15+'ms">'
        +'<td class="td-idx">'+(i+1)+'</td>'
        +'<td class="td-sym td-sym-'+mktCls+'">'+sym+'</td>'
        +'<td class="td-name'+(nm?'':' loading')+'" id="nm-'+esc(it.symbol)+'">'+
            (nm||'<span style="color:var(--t3);font-size:9px;">…</span>')+'</td>'
        +'<td class="td-date">'+dt+'</td>'
        +'<td><button class="del-btn" data-id="'+esc(it.id)+'">삭제</button></td>'
        +'</tr>';
    }).join('');
  }

  /* 이름 로드 후 DOM 업데이트 */
  function fillNamesInDOM(items){
    const syms=items.map(it=>it.symbol).filter(Boolean);
    loadNames(syms).then(()=>{
      syms.forEach(s=>{
        const el=document.getElementById('nm-'+s);
        if(el&&nameCache[s]){
          el.textContent=nameCache[s];
          el.classList.remove('loading');
        }
      });
    });
  }

  /* ── 날짜 포맷 ── */
  function formatDate(v){
    if(!v) return '—';
    if(Array.isArray(v)&&v.length>=3)
      return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+(v.length>=6?' '+p2(v[3])+':'+p2(v[4]):'');
    return String(v).replace('T',' ').substring(0,16);
  }

  /* ── 전체 렌더 ── */
  function renderAll(items){
    const krItems=items.filter(it=>detectMkt(it)==='KR');
    const usItems=items.filter(it=>detectMkt(it)==='US');

    document.getElementById('statTotal').textContent=items.length;
    document.getElementById('statKR').textContent=krItems.length;
    document.getElementById('statUS').textContent=usItems.length;
    document.getElementById('krCount').textContent=krItems.length+'개';
    document.getElementById('usCount').textContent=usItems.length+'개';

    renderRows('krBody', krItems, 'kr');
    renderRows('usBody', usItems, 'us');

    /* 종목명 비동기 보완 */
    fillNamesInDOM([...krItems,...usItems]);
  }

  /* ── 워치리스트 로드 ── */
  function load(){
    fetch(BASE+'/api/watchlist')
      .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
      .then(rows=>{
        allItems=Array.isArray(rows)?rows:[];
        /* 응답에 name/symbolName 있으면 선반영 */
        allItems.forEach(it=>{
          if(it.symbol&&(it.name||it.symbolName)){
            nameCache[it.symbol]=it.name||it.symbolName||'';
          }
        });
        renderAll(allItems);
      })
      .catch(e=>{
        document.getElementById('krBody').innerHTML='<tr><td colspan="5" class="tbl-empty">로드 실패: '+esc(e.message)+'</td></tr>';
        document.getElementById('usBody').innerHTML='<tr><td colspan="5" class="tbl-empty">로드 실패</td></tr>';
      });
  }

  /* ── 심볼 추가 ── */
  window.addSymbol=function(){
    const inp=document.getElementById('symIn');
    const raw=(inp.value||'').trim().toUpperCase().replace(/[^A-Z0-9.\-]/g,'');
    if(!raw){ showMsg('종목코드를 입력하세요.','err'); inp.focus(); return; }

    /* [FIX] 클라이언트 입력 검증 */
    if(!validateSymbol(raw,addMkt)){
      if(addMkt==='KR') showMsg('KR 종목 코드는 6자리 숫자입니다. (예: 005930)','err');
      else showMsg('US 종목 코드 형식이 올바르지 않습니다. (예: AAPL)','err');
      inp.focus(); return;
    }

    /* 중복 확인 */
    const dup=allItems.some(it=>String(it.symbol||'').toUpperCase()===raw);
    if(dup){ showMsg(raw+' 은(는) 이미 등록되어 있습니다.','info'); return; }

    const btn=document.getElementById('addBtn');
    btn.disabled=true; btn.classList.add('loading');

    const exchange=addMkt==='KR'?'KRX':'NAS';
    fetch(BASE+'/api/watchlist',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'symbol='+encodeURIComponent(raw)+'&exchange='+encodeURIComponent(exchange)
    })
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json().catch(()=>({}));})
    .then(d=>{
      if(d.status==='DUPLICATE'){showMsg((d.message||raw+' 이미 등록됨'),'info');return;}
      if(d.status&&d.status!=='OK'){showMsg(d.message||'추가 실패','err');return;}
      inp.value='';
      showMsg(raw+' 추가 완료 ✓','ok');
      load();
    })
    .catch(e=>showMsg('추가 실패: '+esc(e.message),'err'))
    .finally(()=>{btn.disabled=false;btn.classList.remove('loading');});
  };

  /* ── 삭제 이벤트 위임 ── */
  function onDelete(e){
    const btn=e.target.closest('.del-btn');
    if(!btn) return;
    const id=btn.dataset.id;
    if(!id||isNaN(Number(id))||Number(id)<=0) return; /* [FIX] id 검증 */
    btn.disabled=true; btn.textContent='…';
    fetch(BASE+'/api/watchlist/delete',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'id='+encodeURIComponent(id)
    })
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json().catch(()=>({}));})
    .then(d=>{
      if(d.status==='OK'||!d.status){showMsg('삭제 완료','ok');load();}
      else{showMsg(d.message||'삭제 실패','err');btn.disabled=false;btn.textContent='삭제';}
    })
    .catch(e=>{showMsg('삭제 실패: '+esc(e.message),'err');btn.disabled=false;btn.textContent='삭제';});
  }
  document.getElementById('krBody').addEventListener('click',onDelete);
  document.getElementById('usBody').addEventListener('click',onDelete);

  /* ── Enter 키 추가 ── */
  document.getElementById('symIn').addEventListener('keydown',e=>{if(e.key==='Enter')addSymbol();});

  /* ── 초기화 ── */
  load();
})();
</script>
</body>
</html>
