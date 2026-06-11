<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Balances — AUTO TRADING</title>
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
  --r:6px;--r2:10px;--r3:12px;--topbar-h:52px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{background:var(--void);min-height:100%;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.05) 0%,transparent 55%),
    radial-gradient(ellipse 40% 60% at 100% 110%,rgba(77,159,255,.04) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.032) 1px,transparent 1px);background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes spin{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes barIn{from{width:0;}to{width:var(--bw,0%);}}
@keyframes rowIn{from{opacity:0;transform:translateX(-4px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{opacity:1;}50%{opacity:.25;}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.96);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime) 40%,rgba(168,255,62,.2) 70%,transparent);opacity:.4;}
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
.tb-pill{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:9px;color:var(--emerald);
  padding:3px 9px;border-radius:20px;background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);box-shadow:0 0 10px rgba(0,217,126,.4);animation:pd 1.4s ease-in-out infinite;}
.tb-a{font-family:var(--mono);font-size:10px;letter-spacing:.4px;padding:5px 11px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-login{display:flex;align-items:center;gap:5px;padding:0 10px;border-left:1px solid var(--rim);}
.tb-login form{display:flex;align-items:center;gap:4px;}
.tb-login input{height:24px;width:100px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:9px;padding:0 7px;}
.tb-login input::placeholder{color:var(--t3);}
.tb-lbtn{height:24px;padding:0 9px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:9px;cursor:pointer;transition:all .15s;}
.tb-lbtn:hover{background:var(--lime);color:var(--void);}
.tb-login-st{display:none;align-items:center;gap:6px;font-family:var(--mono);font-size:9px;color:var(--t2);}
.tb-login-st .acc{color:var(--lime);}
.tb-lerr{font-family:var(--mono);font-size:9px;color:var(--red);display:none;margin-left:3px;}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── PAGE ── */
.page{position:relative;z-index:1;padding:12px 14px;display:flex;flex-direction:column;gap:10px;
  min-height:calc(100vh - var(--topbar-h));}

/* ── TABS ── */
.tab-bar{display:flex;align-items:center;gap:4px;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:6px;animation:fu .35s .04s ease both;}
.tab-btn{flex:1;height:32px;font-family:var(--mono);font-size:9px;letter-spacing:.8px;
  border:1px solid transparent;border-radius:var(--r2);background:transparent;
  color:var(--t3);cursor:pointer;transition:all .15s;display:flex;align-items:center;
  justify-content:center;gap:6px;}
.tab-btn:hover{color:var(--t2);}
.tab-btn.active-kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tab-btn.active-us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.tab-panel{display:none;flex-direction:column;gap:10px;}
.tab-panel.show{display:flex;animation:fu .3s ease both;}

/* ── TOOLBAR ── */
.toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);padding:9px 12px;}
.tb-sel{height:28px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;outline:none;cursor:pointer;}
.tb-sel option{background:var(--panel-hi);}
.ref-btn{height:28px;padding:0 12px;font-family:var(--mono);font-size:9px;letter-spacing:.5px;
  border:1px solid var(--lime-b);border-radius:var(--r);background:var(--lime-d);color:var(--lime);
  cursor:pointer;transition:all .12s;display:inline-flex;align-items:center;gap:5px;}
.ref-btn:hover{background:var(--lime);color:var(--void);}
.ref-btn:disabled{opacity:.45;cursor:not-allowed;}
.ref-btn svg{width:10px;height:10px;}
.ref-btn.spin svg{animation:spin .6s linear infinite;}
.tb-upd{font-family:var(--mono);font-size:8px;color:var(--t3);margin-left:auto;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--t3);display:inline-block;margin-right:4px;}
.tb-dot.live{background:var(--emerald);animation:pd 1.4s ease-in-out infinite;}

/* ── KPI GRID ── */
.kpi-grid{display:grid;gap:8px;}
.kpi-grid.cols5{grid-template-columns:repeat(5,minmax(0,1fr));}
.kpi-grid.cols4{grid-template-columns:repeat(4,minmax(0,1fr));}
@media(max-width:1100px){.kpi-grid.cols5,.kpi-grid.cols4{grid-template-columns:repeat(3,1fr);}}
@media(max-width:640px){.kpi-grid.cols5,.kpi-grid.cols4{grid-template-columns:repeat(2,1fr);}}

.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:14px 14px 12px;position:relative;overflow:hidden;transition:border-color .2s;}
.kpi:hover{border-color:var(--rim-hi);}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;background:var(--acc,transparent);}
.kpi::after{content:'';position:absolute;inset:0;pointer-events:none;
  background:radial-gradient(ellipse 60% 40% at 50% 0%,var(--acc-soft,transparent),transparent);opacity:.3;}
.kpi-label{font-family:var(--mono);font-size:7px;color:var(--t3);
  letter-spacing:1.5px;text-transform:uppercase;margin-bottom:8px;
  display:flex;align-items:center;gap:5px;position:relative;z-index:1;}
.kpi-dot{width:4px;height:4px;border-radius:50%;flex-shrink:0;}
.kpi-val{font-family:var(--mono);font-size:19px;font-weight:600;
  line-height:1;letter-spacing:-.5px;position:relative;z-index:1;}
.kpi-sub{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:5px;position:relative;z-index:1;}
/* 미니 게이지 */
.kpi-gauge{height:3px;background:var(--t4);border-radius:2px;margin-top:8px;overflow:hidden;position:relative;z-index:1;}
.kpi-gauge-fill{height:100%;border-radius:2px;animation:barIn .7s ease both;}

/* ── SECTION TITLE ── */
.sec-hd{display:flex;align-items:center;gap:8px;}
.sec-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.sec-title{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);
  letter-spacing:1.5px;text-transform:uppercase;}
.sec-badge{font-family:var(--mono);font-size:8px;padding:2px 8px;border-radius:5px;
  border:1px solid var(--rim);color:var(--t2);background:var(--base);}

/* ── CARD ── */
.card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);overflow:hidden;}
.card-hd{display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:0 14px;height:38px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}

/* ── TABLE ── */
.tbl-wrap{overflow:auto;scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.tbl-wrap::-webkit-scrollbar{width:3px;height:3px;}
.tbl-wrap::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
table{width:100%;border-collapse:collapse;}
thead th{position:sticky;top:0;z-index:2;
  background:var(--panel-hi);border-bottom:1px solid var(--rim);
  font-family:var(--mono);font-size:7px;font-weight:500;color:var(--t3);
  letter-spacing:1.2px;text-transform:uppercase;padding:9px 12px;
  text-align:right;white-space:nowrap;}
thead th:first-child{text-align:left;}
thead th.sort-active{color:var(--lime);}
tbody td{padding:11px 12px;border-bottom:1px solid var(--t4);
  vertical-align:middle;text-align:right;white-space:nowrap;
  font-family:var(--mono);font-size:12px;}
tbody td:first-child{text-align:left;font-family:var(--sans);font-size:13px;}
tbody tr:last-child td{border-bottom:none;}
tbody tr:hover td{background:var(--hover);}
tbody tr{animation:rowIn .2s ease both;}
.td-name{font-weight:700;color:var(--t1);line-height:1.3;}
.td-sym{font-family:var(--mono);font-size:9px;color:var(--t3);margin-top:2px;}
.td-mkt{display:inline-block;font-family:var(--mono);font-size:7px;
  padding:1px 5px;border-radius:3px;margin-top:3px;}
.td-mkt.kr{color:var(--lime);background:var(--lime-d);border:1px solid var(--lime-b);}
.td-mkt.us{color:var(--blue);background:var(--blue-d);border:1px solid var(--blue-b);}
/* 손익 셀 */
.pnl-pos{color:var(--emerald);}
.pnl-neg{color:var(--red);}
.pnl-zero{color:var(--t3);}
/* 수익률 뱃지 */
.rt-badge{display:inline-flex;align-items:center;font-family:var(--mono);font-size:9px;
  font-weight:600;padding:2px 7px;border-radius:4px;white-space:nowrap;}
.rt-pos{color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.rt-neg{color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.rt-zero{color:var(--t3);background:var(--panel-hi);border:1px solid var(--rim);}
/* 미니 PnL 바 */
.pnl-bar-cell{min-width:80px;}
.pnl-bar-wrap{height:4px;background:var(--t4);border-radius:2px;overflow:hidden;margin-top:4px;}
.pnl-bar-fill{height:100%;border-radius:2px;animation:barIn .6s ease both;}

/* ── EMPTY / STATE ── */
.state-box{display:flex;flex-direction:column;align-items:center;justify-content:center;
  gap:10px;padding:60px 20px;font-family:var(--mono);font-size:10px;color:var(--t3);
  letter-spacing:1.5px;text-align:center;}
.state-icon{font-size:28px;line-height:1;}
.state-spinner{width:18px;height:18px;border:2px solid var(--rim-hi);
  border-top-color:var(--lime);border-radius:50%;animation:spin .6s linear infinite;}

/* ── 포트폴리오 분포 바 ── */
.dist-bar{height:8px;border-radius:4px;display:flex;overflow:hidden;gap:2px;margin-top:4px;}
.dist-seg{height:100%;border-radius:2px;transition:flex .5s ease;}
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
    <div><div class="logo-name">AUTO<span>TRADE</span></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-sp"></div>
  <div class="tb-pill"><div class="tb-dot"></div><span id="hdSt">—</span></div>
  <div class="tb-nav">
    <a class="tb-a"     href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a cur" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/backtest">Backtest</a>
  </div>
  <div class="tb-login">
    <form id="lf" autocomplete="off">
      <input type="text" name="accountNo" placeholder="계좌번호" autocomplete="off" maxlength="20"/>
      <input type="password" name="accountPassword" placeholder="Password" autocomplete="new-password" maxlength="50"/>
      <button class="tb-lbtn" type="submit">Login</button>
    </form>
    <div class="tb-login-st" id="lst">
      <span class="acc" id="lacc">****</span>
      <button id="lob" type="button" class="tb-lbtn">Logout</button>
    </div>
    <div class="tb-lerr" id="lerr"></div>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- ── TABS ── -->
  <div class="tab-bar">
    <button class="tab-btn active-kr" id="tabKR" onclick="switchTab('kr')">
      🇰🇷 국내 잔고
    </button>
    <button class="tab-btn" id="tabUS" onclick="switchTab('us')">
      🇺🇸 해외 잔고
    </button>
  </div>

  <!-- ══════════════════════════
       국내 탭
  ══════════════════════════ -->
  <div class="tab-panel show" id="panelKR">

    <!-- 툴바 -->
    <div class="toolbar">
      <button class="ref-btn" id="btnKR" onclick="loadKR()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
             stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"/>
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
        </svg>
        조회
      </button>
      <span class="tb-dot" id="krDot"></span>
      <span class="tb-upd" id="krUpd">—</span>
    </div>

    <!-- KPI -->
    <div class="kpi-grid cols5" id="krKpi" style="display:none;">
      <div class="kpi" style="--acc:var(--lime);--acc-soft:rgba(168,255,62,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--lime)"></div>총 자산</div>
        <div class="kpi-val" id="krTotAsst" style="color:var(--lime)">—</div>
        <div class="kpi-sub" id="krTotSub">—</div>
        <div class="kpi-gauge"><div class="kpi-gauge-fill" id="krTotBar" style="background:var(--lime);--bw:100%;width:100%;"></div></div>
      </div>
      <div class="kpi" style="--acc:var(--blue);--acc-soft:rgba(77,159,255,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--blue)"></div>순 자산</div>
        <div class="kpi-val" id="krNass" style="color:var(--blue)">—</div>
        <div class="kpi-sub">부채 차감 후</div>
      </div>
      <div class="kpi" style="--acc:var(--gold);--acc-soft:rgba(245,200,66,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--gold)"></div>예수금</div>
        <div class="kpi-val" id="krCash" style="color:var(--gold)">—</div>
        <div class="kpi-sub" id="krCashOrd">주문가능: —</div>
        <div class="kpi-gauge"><div class="kpi-gauge-fill" id="krCashBar" style="background:var(--gold);"></div></div>
      </div>
      <div class="kpi" id="krPnlCard" style="--acc:var(--emerald);--acc-soft:rgba(0,217,126,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--emerald)"></div>평가손익</div>
        <div class="kpi-val" id="krPfls" style="color:var(--emerald)">—</div>
        <div class="kpi-sub" id="krPflsPct">—</div>
        <div class="kpi-gauge"><div class="kpi-gauge-fill" id="krPnlBar"></div></div>
      </div>
      <div class="kpi" style="--acc:var(--purple);--acc-soft:rgba(176,127,255,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--purple)"></div>매입금액</div>
        <div class="kpi-val" id="krPchs" style="color:var(--purple)">—</div>
        <div class="kpi-sub" id="krEvlu">평가: —</div>
      </div>
    </div>

    <!-- 포트폴리오 분포 바 -->
    <div class="card" id="krDistCard" style="display:none;">
      <div class="card-hd">
        <div style="display:flex;align-items:center;gap:7px;">
          <div style="width:5px;height:5px;border-radius:50%;background:var(--lime);"></div>
          <span style="font-family:var(--mono);font-size:8px;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;">포트폴리오 분포</span>
        </div>
        <span id="krDistSub" style="font-family:var(--mono);font-size:8px;color:var(--t3);">—</span>
      </div>
      <div style="padding:12px 14px;">
        <div class="dist-bar" id="krDistBar"></div>
        <div id="krDistLegend" style="display:flex;gap:12px;margin-top:8px;flex-wrap:wrap;"></div>
      </div>
    </div>

    <!-- 보유 종목 -->
    <div class="card" id="krHoldCard" style="display:none;">
      <div class="card-hd">
        <div style="display:flex;align-items:center;gap:7px;">
          <div class="sec-dot" style="background:var(--lime);"></div>
          <span class="sec-title">보유 종목</span>
          <span class="sec-badge" id="krHoldCnt">0</span>
        </div>
        <span style="font-family:var(--mono);font-size:8px;color:var(--t3);" id="krHoldTot">—</span>
      </div>
      <div class="tbl-wrap">
        <table>
          <thead>
            <tr>
              <th>종목</th>
              <th>수량</th>
              <th>평균단가</th>
              <th>현재가</th>
              <th>매입금액</th>
              <th>평가금액</th>
              <th>손익</th>
              <th>수익률</th>
            </tr>
          </thead>
          <tbody id="krTbody"></tbody>
        </table>
      </div>
    </div>

    <div class="card" id="krState">
      <div class="state-box">
        <div class="state-icon">📊</div>
        조회 버튼을 눌러 잔고를 불러오세요
      </div>
    </div>

  </div><!-- /panelKR -->

  <!-- ══════════════════════════
       해외 탭
  ══════════════════════════ -->
  <div class="tab-panel" id="panelUS">

    <!-- 툴바 -->
    <div class="toolbar">
      <select class="tb-sel" id="usExch">
        <option value="NASD">NASDAQ (NASD)</option>
        <option value="NYSE">NYSE</option>
        <option value="AMEX">AMEX</option>
      </select>
      <select class="tb-sel" id="usCcy">
        <option value="USD">USD 달러</option>
        <option value="HKD">HKD 홍콩달러</option>
        <option value="JPY">JPY 엔화</option>
        <option value="CNY">CNY 위안</option>
      </select>
      <button class="ref-btn" id="btnUS" onclick="loadUS()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
             stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"/>
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
        </svg>
        조회
      </button>
      <span class="tb-dot" id="usDot"></span>
      <span class="tb-upd" id="usUpd">—</span>
    </div>

    <!-- KPI -->
    <div class="kpi-grid cols5" id="usKpi" style="display:none;">
      <div class="kpi" style="--acc:var(--blue);--acc-soft:rgba(77,159,255,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--blue)"></div>총 자산</div>
        <div class="kpi-val" id="usTotAsst" style="color:var(--blue)">—</div>
        <div class="kpi-sub" id="usTotSub">외화 기준</div>
      </div>
      <div class="kpi" style="--acc:var(--gold);--acc-soft:rgba(245,200,66,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--gold)"></div>예수금 (주문가능)</div>
        <div class="kpi-val" id="usCash" style="color:var(--gold)">—</div>
        <div class="kpi-sub" id="usCashSub">—</div>
        <div class="kpi-gauge"><div class="kpi-gauge-fill" id="usCashBar" style="background:var(--gold);"></div></div>
      </div>
      <div class="kpi" style="--acc:var(--purple);--acc-soft:rgba(176,127,255,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--purple)"></div>매입금액</div>
        <div class="kpi-val" id="usPchs" style="color:var(--purple)">—</div>
        <div class="kpi-sub">외화 기준</div>
      </div>
      <div class="kpi" id="usPnlCard" style="--acc:var(--emerald);--acc-soft:rgba(0,217,126,.05);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--emerald)"></div>평가손익</div>
        <div class="kpi-val" id="usPfls" style="color:var(--emerald)">—</div>
        <div class="kpi-sub" id="usPflsPct">—</div>
        <div class="kpi-gauge"><div class="kpi-gauge-fill" id="usPnlBar"></div></div>
      </div>
      <div class="kpi" style="--acc:var(--lime);--acc-soft:rgba(168,255,62,.04);">
        <div class="kpi-label"><div class="kpi-dot" style="background:var(--lime)"></div>평가금액</div>
        <div class="kpi-val" id="usEvlu" style="color:var(--lime)">—</div>
        <div class="kpi-sub">보유 종목 합산</div>
      </div>
    </div>

    <!-- 포트폴리오 분포 -->
    <div class="card" id="usDistCard" style="display:none;">
      <div class="card-hd">
        <div style="display:flex;align-items:center;gap:7px;">
          <div style="width:5px;height:5px;border-radius:50%;background:var(--blue);"></div>
          <span style="font-family:var(--mono);font-size:8px;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;">포트폴리오 분포</span>
        </div>
        <span id="usDistSub" style="font-family:var(--mono);font-size:8px;color:var(--t3);">—</span>
      </div>
      <div style="padding:12px 14px;">
        <div class="dist-bar" id="usDistBar"></div>
        <div id="usDistLegend" style="display:flex;gap:12px;margin-top:8px;flex-wrap:wrap;"></div>
      </div>
    </div>

    <!-- 보유 종목 -->
    <div class="card" id="usHoldCard" style="display:none;">
      <div class="card-hd">
        <div style="display:flex;align-items:center;gap:7px;">
          <div class="sec-dot" style="background:var(--blue);"></div>
          <span class="sec-title">보유 종목</span>
          <span class="sec-badge" id="usHoldCnt">0</span>
        </div>
        <span style="font-family:var(--mono);font-size:8px;color:var(--t3);" id="usHoldTot">—</span>
      </div>
      <div class="tbl-wrap">
        <table>
          <thead>
            <tr>
              <th>종목</th>
              <th>수량</th>
              <th>평균단가</th>
              <th>현재가</th>
              <th>매입금액</th>
              <th>평가금액</th>
              <th>손익</th>
              <th>수익률</th>
            </tr>
          </thead>
          <tbody id="usTbody"></tbody>
        </table>
      </div>
    </div>

    <div class="card" id="usState">
      <div class="state-box">
        <div class="state-icon">🌐</div>
        거래소와 통화를 선택하고 조회하세요
      </div>
    </div>

  </div><!-- /panelUS -->

</div><!-- /page -->

<script>
'use strict';
(function(){
  const BASE = '${pageContext.request.contextPath}';
  const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  /* ── 시계 ── */
  function p2(v){return String(v).padStart(2,'0');}
  (function tick(){
    const n=new Date();
    document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
    document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
    setTimeout(tick,1000);
  })();

  /* ── Login ── */
  (function(){
    var _B='${pageContext.request.contextPath}';
    var f=document.getElementById('lf'),sb=document.getElementById('lst');
    var as=document.getElementById('lacc'),lb=document.getElementById('lob'),eb=document.getElementById('lerr');
    var showIn=function(m){f.style.display='none';sb.style.display='inline-flex';eb.style.display='none';as.textContent=m||'****';};
    var showOut=function(){sb.style.display='none';f.style.display='';eb.style.display='none';};
    var showErr=function(m){eb.textContent=m||'';eb.style.display=m?'inline-flex':'none';};
    fetch(_B+'/api/auth/status').then(function(r){return r.json();}).then(function(d){d&&d.loggedIn?showIn(d.accountMasked):showOut();}).catch(function(){showOut();});
    f.addEventListener('submit',function(e){
      e.preventDefault();
      var no=(f.accountNo.value||'').trim(),pw=(f.accountPassword.value||'').trim();
      if(!no||!pw)return;
      fetch(_B+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({accountNo:no,accountPassword:pw}).toString()})
        .then(function(r){return r.json();}).then(function(d){d.status==='OK'?showIn(d.accountMasked):showErr(d.message||'Login failed');})
        .catch(function(){showErr('서버 오류');});
    });
    lb.addEventListener('click',function(){fetch(_B+'/api/auth/logout',{method:'POST'}).then(function(){showOut();});});
  })();

  /* ── XSS 방지 ── */
  function esc(s){
    return String(s==null?'':s)
      .replace(/&/g,'&amp;').replace(/</g,'&lt;')
      .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#x27;');
  }

  /* ── 숫자 파싱 ── */
  function pn(v){ return parseFloat(String(v||0).replace(/,/g,''))||0; }

  /* ── 포맷 ── */
  const nfKR=new Intl.NumberFormat('ko-KR');
  const nfUS=new Intl.NumberFormat('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});

  function fmtKRW(v){
    const n=pn(v);
    if(Math.abs(n)>=1e8) return (n/1e8).toFixed(2)+'억원';
    if(Math.abs(n)>=1e4) return (n/1e4).toFixed(1)+'만원';
    return nfKR.format(Math.round(n))+'원';
  }
  function fmtKRWRaw(v){ return nfKR.format(Math.round(pn(v))); }
  function fmtFX(v,ccy){
    const n=pn(v);
    const sym={'USD':'$','HKD':'HK$','JPY':'¥','CNY':'¥'}[ccy]||ccy+' ';
    if(ccy==='JPY'||ccy==='CNY') return sym+nfKR.format(Math.round(n));
    return sym+nfUS.format(n);
  }
  function fmtPct(v){ const n=pn(v); return (n>=0?'+':'')+n.toFixed(2)+'%'; }
  function signed(v,fmt){ const n=pn(v); return (n>=0?'+':'')+fmt(n<0?-n:n); }
  function pnlCls(n){ return n>0?'pnl-pos':n<0?'pnl-neg':'pnl-zero'; }
  function rtCls(n){ return n>0?'rt-pos':n<0?'rt-neg':'rt-zero'; }

  /* ── 포트폴리오 색상 팔레트 ── */
  const PALETTE=[
    '#a8ff3e','#4d9fff','#f5c842','#00d97e',
    '#b07fff','#ff4d6a','#00bcd4','#ff9800'
  ];

  /* ── 탭 전환 ── */
  window.switchTab=function(t){
    document.getElementById('tabKR').className='tab-btn'+(t==='kr'?' active-kr':'');
    document.getElementById('tabUS').className='tab-btn'+(t==='us'?' active-us':'');
    document.getElementById('panelKR').className='tab-panel'+(t==='kr'?' show':'');
    document.getElementById('panelUS').className='tab-panel'+(t==='us'?' show':'');
  };

  /* ── 버튼 스피너 ── */
  function setBtnLoading(id,on){
    const b=document.getElementById(id);
    b.disabled=on;
    b.classList.toggle('spin',on);
  }

  /* ── 상태 도트 ── */
  function setDot(id,state){
    const d=document.getElementById(id);
    d.className='tb-dot'+(state==='live'?' live':'');
  }

  /* ── 업데이트 시각 ── */
  function setUpd(id){
    const n=new Date();
    document.getElementById(id).textContent=
      '갱신 '+p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  }

  /* ── State box ── */
  function showStateLoad(id,msg){
    const el=document.getElementById(id);
    el.style.display='';
    el.innerHTML='<div class="state-box"><div class="state-spinner"></div><div>'+esc(msg)+'</div></div>';
  }
  function showStateErr(id,msg){
    const el=document.getElementById(id);
    el.style.display='';
    el.innerHTML='<div class="state-box"><div class="state-icon">⚠️</div><div>'+esc(msg)+'</div></div>';
  }

  /* ── 포트폴리오 분포 바 렌더 ── */
  function renderDist(barId,legendId,subId,items,fmtFn){
    const total=items.reduce((s,it)=>s+it.evlu,0)||1;
    let barHtml='',legHtml='';
    items.forEach((it,i)=>{
      const pct=Math.max((it.evlu/total*100),0.5).toFixed(1);
      const col=PALETTE[i%PALETTE.length];
      barHtml+='<div class="dist-seg" style="flex:'+pct+';background:'+col+';opacity:.85;" title="'+esc(it.name)+' '+pct+'%"></div>';
      legHtml+='<div style="display:flex;align-items:center;gap:4px;font-family:var(--mono);font-size:8px;color:var(--t2);">'
        +'<div style="width:8px;height:8px;border-radius:2px;background:'+col+';flex-shrink:0;"></div>'
        +'<span>'+esc(it.name)+'</span>'
        +'<span style="color:var(--t3);">'+pct+'%</span>'
        +'</div>';
    });
    document.getElementById(barId).innerHTML=barHtml;
    document.getElementById(legendId).innerHTML=legHtml;
    document.getElementById(subId).textContent=items.length+'개 종목 · 총 '+fmtFn(total);
  }

  /* ════════════════════════════════════
     국내 잔고 로드
  ════════════════════════════════════ */
  window.loadKR=function(){
    setBtnLoading('btnKR',true);
    showStateLoad('krState','잔고 조회 중…');
    ['krKpi','krDistCard','krHoldCard'].forEach(id=>document.getElementById(id).style.display='none');

    fetch(BASE+'/api/account/balance/kr')
      .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
      .then(res=>{
        if(res.status!=='OK') throw new Error(res.message||'잔고 조회 실패');

        /* output2: 요약 */
        const o2=Array.isArray(res.output2)?res.output2[0]:(res.output2||{});
        const totAsst  = pn(o2.tot_asst_amt);
        const nass     = pn(o2.nass_tot_amt);
        const cash     = pn(o2.dnca_tot_amt||o2.dncl_amt);
        const ordPsbl  = pn(o2.ord_psbl_amt||o2.ord_psbl_cash||cash);
        const pfls     = pn(o2.evlu_pfls_amt_smtl||o2.evlu_pfls_amt);
        const pchsSmtl = pn(o2.pchs_amt_smtl)||1;
        const evluSmtl = pn(o2.evlu_amt_smtl||o2.evlu_tot_amt);
        const pflsPct  = pchsSmtl>0?(pfls/pchsSmtl*100):0;
        const cashRatio= totAsst>0?(cash/totAsst*100):0;

        document.getElementById('krTotAsst').textContent=fmtKRW(totAsst);
        document.getElementById('krTotSub').textContent ='매입 '+fmtKRW(pchsSmtl);
        document.getElementById('krNass').textContent   =fmtKRW(nass);
        document.getElementById('krCash').textContent   =fmtKRW(cash);
        document.getElementById('krCashOrd').textContent='주문가능: '+fmtKRW(ordPsbl);
        document.getElementById('krCashBar').style.cssText=
          'background:var(--gold);--bw:'+cashRatio.toFixed(0)+'%;width:'+cashRatio.toFixed(0)+'%;animation:barIn .7s ease both;';
        document.getElementById('krPchs').textContent  =fmtKRW(pchsSmtl);
        document.getElementById('krEvlu').textContent  ='평가: '+fmtKRW(evluSmtl);

        /* 손익 카드 동적 색상 */
        const pflsEl=document.getElementById('krPfls');
        const col=pfls>=0?'var(--emerald)':'var(--red)';
        pflsEl.textContent=(pfls>=0?'+':'')+fmtKRWRaw(pfls)+'원';
        pflsEl.style.color=col;
        document.getElementById('krPflsPct').textContent=fmtPct(pflsPct);
        const kpiPC=document.getElementById('krPnlCard');
        kpiPC.style.setProperty('--acc',col);
        kpiPC.style.setProperty('--acc-soft',pfls>=0?'rgba(0,217,126,.05)':'rgba(255,77,106,.05)');
        document.getElementById('krPnlBar').style.cssText=
          'background:'+col+';--bw:'+Math.min(100,Math.abs(pflsPct)).toFixed(0)+'%;width:'+Math.min(100,Math.abs(pflsPct)).toFixed(0)+'%;animation:barIn .7s ease both;';

        document.getElementById('krKpi').style.display='';

        /* output1: 보유 종목 */
        const items=Array.isArray(res.output1)?res.output1:(res.output||[]);
        if(items.length>0){
          const maxAbsPnl=Math.max(...items.map(r=>Math.abs(pn(r.evlu_pfls_amt))),1);
          const tbody=document.getElementById('krTbody');
          tbody.innerHTML='';

          const distItems=[];
          items.forEach((r,i)=>{
            const name   =esc(r.prdt_name||r.pdno||'—');
            const sym    =esc(r.pdno||'');
            const qty    =pn(r.hldg_qty);
            const avgP   =pn(r.pchs_avg_pric);
            const curP   =pn(r.prpr||r.stck_prpr);
            const pchsAmt=pn(r.pchs_amt||avgP*qty);
            const evlu   =pn(r.evlu_amt);
            const pnl    =pn(r.evlu_pfls_amt);
            const pnlPct =pn(r.evlu_pfls_rt);
            const barW   =Math.min(100,Math.abs(pnl)/maxAbsPnl*100).toFixed(0);
            const pc     =pnlCls(pnl), rc=rtCls(pnlPct);

            distItems.push({name:r.prdt_name||sym,evlu});

            tbody.innerHTML+='<tr style="animation-delay:'+i*20+'ms;">'
              +'<td><div class="td-name">'+name+'</div>'
              +'<div class="td-sym">'+sym+'</div>'
              +'<div><span class="td-mkt kr">KR</span></div></td>'
              +'<td>'+nfKR.format(qty)+'</td>'
              +'<td>'+nfKR.format(Math.round(avgP))+'</td>'
              +'<td>'+nfKR.format(Math.round(curP))+'</td>'
              +'<td>'+nfKR.format(Math.round(pchsAmt))+'</td>'
              +'<td>'+nfKR.format(Math.round(evlu))+'</td>'
              +'<td class="'+pc+'">'
              +(pnl>=0?'+':'')+nfKR.format(Math.round(pnl))
              +'<div class="pnl-bar-wrap"><div class="pnl-bar-fill" style="background:'+(pnl>=0?'var(--emerald)':'var(--red)')+';--bw:'+barW+'%;width:'+barW+'%;animation:barIn .5s ease both;"></div></div>'
              +'</td>'
              +'<td><span class="rt-badge '+rc+'">'+fmtPct(pnlPct)+'</span></td>'
              +'</tr>';
          });

          const totalEvlu=items.reduce((s,r)=>s+pn(r.evlu_amt),0);
          document.getElementById('krHoldCnt').textContent=items.length+'개';
          document.getElementById('krHoldTot').textContent='총 평가 '+fmtKRW(totalEvlu);
          renderDist('krDistBar','krDistLegend','krDistSub',distItems,fmtKRW);
          document.getElementById('krDistCard').style.display='';
          document.getElementById('krHoldCard').style.display='';
        }

        document.getElementById('krState').style.display='none';
        setDot('krDot','live');
        setUpd('krUpd');
      })
      .catch(e=>{ showStateErr('krState',e.message||'오류가 발생했습니다.'); setDot('krDot',''); })
      .finally(()=>setBtnLoading('btnKR',false));
  };

  /* ════════════════════════════════════
     해외 잔고 로드
  ════════════════════════════════════ */
  window.loadUS=function(){
    const exch=document.getElementById('usExch').value;
    const ccy =document.getElementById('usCcy').value;
    /* 입력 검증 */
    const allowedExch=['NASD','NYSE','AMEX'];
    const allowedCcy =['USD','HKD','JPY','CNY'];
    if(!allowedExch.includes(exch)||!allowedCcy.includes(ccy)) return;

    setBtnLoading('btnUS',true);
    showStateLoad('usState','잔고 조회 중…');
    ['usKpi','usDistCard','usHoldCard'].forEach(id=>document.getElementById(id).style.display='none');

    /* [FIX] balance + cash 병렬 조회 */
    Promise.all([
      fetch(BASE+'/api/account/balance/us?exch='+encodeURIComponent(exch)+'&currency='+encodeURIComponent(ccy))
        .then(r=>{if(!r.ok)throw new Error('balance HTTP '+r.status);return r.json();}),
      fetch(BASE+'/api/account/cash/us?currency='+encodeURIComponent(ccy))
        .then(r=>r.ok?r.json():null)
        .catch(()=>null)
    ])
    .then(([balRes,cashRes])=>{
      if(balRes.status!=='OK') throw new Error(balRes.message||'잔고 조회 실패');

      const o2=Array.isArray(balRes.output2)?balRes.output2[0]:(balRes.output2||{});
      const totAsst  = pn(o2.tot_asst_amt||o2.frcr_evlu_tota);
      const pchsSmtl = pn(o2.frcr_pchs_amt1||o2.pchs_amt)||1;
      const evluSmtl = pn(o2.ovrs_tot_evlu||o2.evlu_amt_smtl);
      const pfls     = pn(o2.ovrs_tot_pfls||o2.evlu_pfls_amt);
      const pflsPct  = pchsSmtl>0?(pfls/pchsSmtl*100):0;

      /* 예수금: cash API 우선, 없으면 balance output2 fallback */
      const cashFromCash = cashRes?.status==='OK' ? pn(cashRes.cash||cashRes.data?.ord_psbl_frcr_amt) : 0;
      const cashFromBal  = pn(o2.ord_psbl_frcr_amt||o2.frcr_dncl_amt_2);
      const cash = cashFromCash>0 ? cashFromCash : cashFromBal;
      const cashRatio = totAsst>0?(cash/totAsst*100):0;

      const fmt=v=>fmtFX(v,ccy);
      document.getElementById('usTotAsst').textContent=fmt(totAsst);
      document.getElementById('usTotSub').textContent =ccy+' 기준';
      document.getElementById('usCash').textContent   =fmt(cash);
      document.getElementById('usCashSub').textContent='총 자산의 '+cashRatio.toFixed(1)+'%';
      document.getElementById('usCashBar').style.cssText=
        'background:var(--gold);--bw:'+cashRatio.toFixed(0)+'%;width:'+cashRatio.toFixed(0)+'%;animation:barIn .7s ease both;';
      document.getElementById('usPchs').textContent=fmt(pchsSmtl);
      document.getElementById('usEvlu').textContent=fmt(evluSmtl);

      const pflsEl=document.getElementById('usPfls');
      const col=pfls>=0?'var(--emerald)':'var(--red)';
      pflsEl.textContent=(pfls>=0?'+':'')+fmt(Math.abs(pfls));
      pflsEl.style.color=col;
      document.getElementById('usPflsPct').textContent=fmtPct(pflsPct);
      const usPnlCard=document.getElementById('usPnlCard');
      usPnlCard.style.setProperty('--acc',col);
      usPnlCard.style.setProperty('--acc-soft',pfls>=0?'rgba(0,217,126,.05)':'rgba(255,77,106,.05)');
      document.getElementById('usPnlBar').style.cssText=
        'background:'+col+';--bw:'+Math.min(100,Math.abs(pflsPct)).toFixed(0)+'%;width:'+Math.min(100,Math.abs(pflsPct)).toFixed(0)+'%;animation:barIn .7s ease both;';

      document.getElementById('usKpi').style.display='';

      /* output1: 보유 종목 */
      const items=Array.isArray(balRes.output1)?balRes.output1:[];
      if(items.length>0){
        const maxAbsPnl=Math.max(...items.map(r=>Math.abs(pn(r.frcr_evlu_pfls_amt||r.evlu_pfls_amt))),1);
        const tbody=document.getElementById('usTbody');
        tbody.innerHTML='';

        const distItems=[];
        items.forEach((r,i)=>{
          const name   =esc(r.ovrs_item_name||r.prdt_name||r.ovrs_pdno||'—');
          const sym    =esc(r.ovrs_pdno||r.pdno||'');
          const qty    =pn(r.ovrs_cblc_qty||r.cblc_qty);
          const avgP   =pn(r.pchs_avg_pric||r.avg_unpr);
          const curP   =pn(r.now_pric2||r.ovrs_stck_prpr||r.stck_prpr);
          const pchsAmt=pn(r.frcr_pchs_amt||avgP*qty);
          const evlu   =pn(r.ovrs_stck_evlu_amt||r.evlu_amt);
          const pnl    =pn(r.frcr_evlu_pfls_amt||r.evlu_pfls_amt);
          const pnlPct =pn(r.evlu_pfls_rt);
          const barW   =Math.min(100,Math.abs(pnl)/maxAbsPnl*100).toFixed(0);
          const pc     =pnlCls(pnl), rc=rtCls(pnlPct);

          distItems.push({name:r.ovrs_item_name||sym,evlu});

          tbody.innerHTML+='<tr style="animation-delay:'+i*20+'ms;">'
            +'<td><div class="td-name">'+name+'</div>'
            +'<div class="td-sym">'+sym+'</div>'
            +'<div><span class="td-mkt us">US</span></div></td>'
            +'<td>'+nfUS.format(qty)+'</td>'
            +'<td>'+fmtFX(avgP,ccy)+'</td>'
            +'<td>'+fmtFX(curP,ccy)+'</td>'
            +'<td>'+fmtFX(pchsAmt,ccy)+'</td>'
            +'<td>'+fmtFX(evlu,ccy)+'</td>'
            +'<td class="'+pc+'">'
            +(pnl>=0?'+':'')+fmtFX(Math.abs(pnl),ccy)
            +'<div class="pnl-bar-wrap"><div class="pnl-bar-fill" style="background:'+(pnl>=0?'var(--emerald)':'var(--red)')+';--bw:'+barW+'%;width:'+barW+'%;animation:barIn .5s ease both;"></div></div>'
            +'</td>'
            +'<td><span class="rt-badge '+rc+'">'+fmtPct(pnlPct)+'</span></td>'
            +'</tr>';
        });

        const totalEvlu=items.reduce((s,r)=>s+pn(r.ovrs_stck_evlu_amt||r.evlu_amt),0);
        document.getElementById('usHoldCnt').textContent=items.length+'개';
        document.getElementById('usHoldTot').textContent='총 평가 '+fmtFX(totalEvlu,ccy);
        renderDist('usDistBar','usDistLegend','usDistSub',distItems,v=>fmtFX(v,ccy));
        document.getElementById('usDistCard').style.display='';
        document.getElementById('usHoldCard').style.display='';
      }

      document.getElementById('usState').style.display='none';
      setDot('usDot','live');
      setUpd('usUpd');
    })
    .catch(e=>{ showStateErr('usState',e.message||'오류가 발생했습니다.'); setDot('usDot',''); })
    .finally(()=>setBtnLoading('btnUS',false));
  };

})();
</script>
</body>
</html>
