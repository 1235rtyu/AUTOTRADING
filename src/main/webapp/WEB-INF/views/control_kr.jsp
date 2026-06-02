<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Control KR — AUTO TRADING</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#07090f;--base:#0d0f18;--panel:#13161f;--panel-hi:#181b27;--hover:#1d2130;
  --lime:#c6ff5e;--lime-d:rgba(198,255,94,.1);--lime-b:rgba(198,255,94,.25);
  --emerald:#00e07a;--emerald-d:rgba(0,224,122,.08);--emerald-b:rgba(0,224,122,.22);
  --red:#ff5070;--red-d:rgba(255,80,112,.08);--red-b:rgba(255,80,112,.25);
  --gold:#ffc940;--gold-d:rgba(255,201,64,.08);--gold-b:rgba(255,201,64,.22);
  --blue:#5ba3ff;--blue-d:rgba(91,163,255,.08);--blue-b:rgba(91,163,255,.22);
  --rim:rgba(255,255,255,.07);--rim-hi:rgba(255,255,255,.18);
  --t1:#ffffff;--t2:#d8e2f0;--t3:#aabacf;--t4:#1a1e2c;
  --mono:'맑은 고딕','Malgun Gothic','Apple SD Gothic Neo',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:50px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;background:var(--void);overflow:hidden;}
body{font-family:var(--mono);font-size:13px;color:var(--t1);}

.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(198,255,94,.018) 1px,transparent 1px),
                   radial-gradient(rgba(91,163,255,.012) 1px,transparent 1px);
  background-size:32px 32px,64px 64px;background-position:0 0,16px 16px;}
.bg-grid::after{content:'';position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 130% 55% at 50% -5%,rgba(91,163,255,.06) 0%,transparent 60%),
             radial-gradient(ellipse 55% 45% at 95% 110%,rgba(198,255,94,.03) 0%,transparent 55%);}

@keyframes sd{from{opacity:0;transform:translateY(-6px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(8px);}to{opacity:1;transform:none;}}
@keyframes pulse-green{0%,100%{box-shadow:0 0 0 0 rgba(0,224,122,.5);}50%{box-shadow:0 0 0 5px rgba(0,224,122,0);}}
@keyframes chip-glow{0%,100%{border-color:rgba(0,224,122,.22);}55%{border-color:rgba(0,224,122,.55);box-shadow:0 0 10px rgba(0,224,122,.18);}}

/* ── TOPBAR ── */
.topbar{position:sticky;top:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;
  background:linear-gradient(180deg,rgba(7,9,15,.99),rgba(9,11,19,.97));
  box-shadow:0 1px 0 rgba(255,255,255,.04),0 6px 28px rgba(0,0,0,.5);animation:sd .3s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;pointer-events:none;
  background:linear-gradient(90deg,transparent,rgba(255,255,255,.05) 15%,rgba(198,255,94,.5) 50%,rgba(255,255,255,.05) 85%,transparent);}
.tb-logo{display:flex;align-items:center;gap:8px;padding:0 14px;height:100%;border-right:1px solid var(--rim);}
.logo-mk{width:26px;height:26px;background:linear-gradient(135deg,#d4ff70,#a8e040);border-radius:6px;
  display:flex;align-items:center;justify-content:center;box-shadow:0 0 12px rgba(198,255,94,.35);}
.logo-mk svg{width:13px;height:13px;}
.logo-name{font-size:11px;font-weight:700;letter-spacing:.5px;color:var(--t1);}
.logo-name b{color:var(--lime);}
.logo-ver{font-size:7px;color:var(--t3);letter-spacing:1.2px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 8px;}
.tb-a{font-size:9px;letter-spacing:.4px;padding:4px 8px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-mkt{display:flex;align-items:center;gap:3px;padding:0 12px;border-left:1px solid var(--rim);}
.mkt-pill{font-size:9px;padding:3px 9px;border-radius:20px;border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.mkt-pill.kr{color:var(--lime);border-color:var(--lime-b);background:var(--lime-d);}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-size:13px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-size:7px;color:var(--t3);letter-spacing:.8px;}

/* ══════════════════════════════
   PAGE LAYOUT
   [PNL 3칸 — 전체 너비]
   [left 280px | center 220px | right 1fr]
══════════════════════════════ */
.page{
  position:relative;z-index:1;
  display:grid;
  grid-template-rows:auto 1fr;
  gap:8px;padding:8px;
  height:calc(100vh - var(--topbar-h));
  overflow:hidden;
}

/* Row 1: PNL */
.pnl-row{
  display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;
  animation:fu .3s .05s ease both;
}

/* Row 2: 3컬럼 */
.main-area{
  display:grid;
  grid-template-columns:380px 420px 1fr;
  gap:8px;
  min-height:0;
}

/* 좌 컬럼: Engine · Running · Manual 세로 */
.left-col{display:grid;grid-template-rows:auto auto 1fr;gap:8px;min-height:0;}

/* 중앙 컬럼: Top 80 세로 리스트 */
.center-col{display:flex;flex-direction:column;min-height:0;}

/* 우 컬럼: Order History 전체 높이 */
.right-col{display:flex;flex-direction:column;min-height:0;}

/* ── CARD base ── */
.card{
  background:linear-gradient(148deg,rgba(21,24,36,.97) 0%,rgba(14,16,25,.95) 100%);
  border:1px solid rgba(255,255,255,.08);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;position:relative;
  box-shadow:0 2px 20px rgba(0,0,0,.4),0 1px 0 rgba(255,255,255,.03) inset;
  transition:border-color .25s,box-shadow .25s;
}
.card::before{content:'';position:absolute;top:0;left:16px;right:16px;height:1px;z-index:1;pointer-events:none;
  background:linear-gradient(90deg,transparent,rgba(255,255,255,.14),transparent);}
.card-hd{
  flex-shrink:0;display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:0 12px;height:32px;border-bottom:1px solid rgba(255,255,255,.06);
  background:linear-gradient(90deg,rgba(255,255,255,.04) 0%,rgba(255,255,255,.015) 60%,transparent 100%);
}
.card-hd-l{display:flex;align-items:center;gap:7px;}
.hd-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;box-shadow:0 0 7px 1px currentColor;}
.card-title{font-size:11px;font-weight:600;color:var(--t2);letter-spacing:1px;text-transform:uppercase;}
.card-bd{padding:10px 12px;display:flex;flex-direction:column;gap:8px;}

/* ── P&L ── */
.pnl-box{
  background:linear-gradient(148deg,rgba(21,24,36,.97),rgba(14,16,25,.95));
  border:1px solid rgba(255,255,255,.08);border-radius:var(--r2);
  padding:10px 14px;display:flex;align-items:center;gap:12px;
  box-shadow:0 2px 16px rgba(0,0,0,.4);
}
.pnl-label{font-size:8px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;white-space:nowrap;}
.pnl-date{font-size:7px;color:var(--t3);margin-top:1px;}
.pnl-amount{font-size:16px;font-weight:600;margin-left:auto;letter-spacing:.5px;}
.pnl-amount.pos{color:var(--emerald);}
.pnl-amount.neg{color:var(--red);}
.pnl-amount.zero{color:var(--t3);}
.pnl-sub{font-size:8px;color:var(--t3);margin-top:2px;text-align:right;}

/* ── Engine ── */
.engine-box{
  display:flex;align-items:center;gap:10px;
  background:linear-gradient(135deg,rgba(13,15,24,.92),rgba(9,11,18,.96));
  border:1px solid rgba(255,255,255,.08);border-radius:var(--r2);padding:10px 12px;
  transition:all .4s;
}
.engine-box.is-run{
  background:linear-gradient(135deg,rgba(0,224,122,.1) 0%,rgba(0,224,122,.03) 40%,rgba(10,12,20,.95) 70%);
  border-color:rgba(0,224,122,.3);box-shadow:inset 0 0 24px rgba(0,224,122,.07),0 0 20px rgba(0,224,122,.1);
}
.eng-indicator{width:32px;height:32px;border-radius:50%;border:2px solid var(--rim);
  display:flex;align-items:center;justify-content:center;flex-shrink:0;transition:all .3s;}
.eng-indicator.run{border-color:var(--emerald);background:rgba(0,224,122,.1);animation:pulse-green 2s infinite;}
.eng-indicator.stop{border-color:var(--t3);background:rgba(255,255,255,.02);}
.eng-dot{width:9px;height:9px;border-radius:50%;background:var(--t3);transition:all .3s;}
.eng-indicator.run .eng-dot{background:var(--emerald);box-shadow:0 0 8px rgba(0,224,122,.6);}
.eng-info{flex:1;min-width:0;}
.eng-state{font-size:16px;font-weight:700;letter-spacing:.5px;}
.eng-state.run{color:var(--emerald);text-shadow:0 0 22px rgba(0,224,122,.5);}
.eng-state.stop{color:var(--t2);}
.eng-sub{font-size:13px;color:var(--t3);margin-top:3px;}
.eng-sub.run{color:#ff9c2a;}

/* ── Running Symbols ── */
.run-chips{display:flex;flex-wrap:wrap;gap:4px;min-height:28px;align-items:flex-start;}
.run-chip{
  display:inline-flex;align-items:center;gap:5px;
  background:linear-gradient(135deg,rgba(0,224,122,.14),rgba(0,224,122,.06));
  border:1px solid rgba(0,224,122,.28);color:var(--emerald);border-radius:20px;padding:4px 10px;
  font-size:13px;font-weight:600;animation:chip-glow 3s ease-in-out infinite;
}
.run-chip .r-name{font-size:11px;color:rgba(0,224,122,.75);font-weight:400;}
.no-run{font-size:13px;color:var(--t3);}

/* ── Manual Start ── */
.card.manual-card{overflow:visible;z-index:50;}
.card.manual-card .card-bd{overflow:visible;}
.suggest-wrap{position:relative;flex:1;min-width:0;}
.tb-input{
  flex:1;height:32px;background:rgba(5,7,14,.65);border:1px solid rgba(255,255,255,.15);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:13px;
  padding:0 9px;outline:none;transition:border-color .2s;box-shadow:inset 0 2px 6px rgba(0,0,0,.3);
}
.tb-input:focus{border-color:rgba(198,255,94,.45);box-shadow:inset 0 2px 6px rgba(0,0,0,.2),0 0 0 2px rgba(198,255,94,.07);}
.tb-input::placeholder{color:var(--t3);}
.amount-input{flex:0 0 110px;text-align:right;}
.tb-sel{
  height:32px;background:rgba(5,7,14,.65);border:1px solid rgba(255,255,255,.15);
  border-radius:var(--r);color:var(--t2);font-family:var(--mono);font-size:13px;padding:0 7px;outline:none;cursor:pointer;
}
.tb-sel option{background:var(--panel-hi);}
.sym-suggest{
  position:absolute;left:0;right:0;top:34px;z-index:200;display:none;
  background:var(--panel-hi);border:1px solid var(--rim-hi);border-radius:var(--r2);
  overflow:hidden;max-height:200px;overflow-y:auto;box-shadow:0 10px 22px rgba(0,0,0,.4);
}
.sym-suggest.show{display:block;}
.sym-opt{
  display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:7px 10px;border-bottom:1px solid var(--t4);cursor:pointer;
}
.sym-opt:last-child{border-bottom:none;}
.sym-opt:hover,.sym-opt.act{background:var(--hover);}
.sym-opt-sym{font-size:11px;font-weight:700;color:var(--t1);}
.sym-opt-name{font-size:9px;color:var(--t2);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.sym-opt-mkt{font-size:8px;color:var(--lime);border:1px solid var(--lime-b);background:var(--lime-d);border-radius:12px;padding:1px 6px;}
.manual-chips{display:flex;flex-wrap:wrap;gap:4px;min-height:22px;}
.m-chip{
  display:inline-flex;align-items:center;gap:5px;background:var(--panel-hi);border:1px solid var(--rim-hi);
  color:var(--t1);border-radius:20px;padding:3px 10px;font-size:12px;
}
.m-chip .m-name{font-size:11px;color:var(--t2);}
.m-chip-del{
  width:15px;height:15px;border:1px solid var(--t3);border-radius:50%;
  background:transparent;color:var(--t3);cursor:pointer;font-size:11px;
  display:flex;align-items:center;justify-content:center;padding:0;transition:all .12s;line-height:1;
}
.m-chip-del:hover{border-color:var(--red);color:var(--red);}
.input-row{display:flex;gap:5px;align-items:center;}
.manual-row{display:flex;gap:5px;align-items:center;margin-top:4px;}
.btn-row{display:flex;gap:5px;margin-top:4px;}
.empty-hint{font-size:12px;color:var(--t3);}

/* ── BUTTONS ── */
.btn{
  height:32px;padding:0 13px;border-radius:var(--r);border:1px solid transparent;
  cursor:pointer;font-family:var(--mono);font-size:12px;font-weight:600;
  letter-spacing:.3px;display:inline-flex;align-items:center;gap:5px;transition:all .15s;white-space:nowrap;
}
.btn:active{transform:scale(.97);}
.btn svg{width:11px;height:11px;flex-shrink:0;}
.btn-lime{background:linear-gradient(135deg,rgba(198,255,94,.15),rgba(198,255,94,.06));border-color:rgba(198,255,94,.32);color:var(--lime);}
.btn-lime:hover{background:linear-gradient(135deg,#c6ff5e,#a8dc40);color:var(--void);border-color:transparent;box-shadow:0 4px 20px rgba(198,255,94,.3);}
.btn-green{background:linear-gradient(135deg,rgba(0,224,122,.18),rgba(0,224,122,.07));border-color:rgba(0,224,122,.38);color:var(--emerald);}
.btn-green:hover{background:linear-gradient(135deg,#00e07a,#00c060);color:var(--void);border-color:transparent;box-shadow:0 4px 22px rgba(0,224,122,.4);}
.btn-red{background:linear-gradient(135deg,rgba(255,80,112,.15),rgba(255,80,112,.06));border-color:rgba(255,80,112,.35);color:var(--red);}
.btn-red:hover{background:linear-gradient(135deg,#ff5070,#dd3060);color:#fff;border-color:transparent;box-shadow:0 4px 20px rgba(255,80,112,.38);}
.btn-blue{background:linear-gradient(135deg,rgba(91,163,255,.16),rgba(91,163,255,.07));border-color:rgba(91,163,255,.32);color:var(--blue);}
.btn-blue:hover{background:linear-gradient(135deg,#5ba3ff,#3d88e8);color:#fff;border-color:transparent;box-shadow:0 4px 20px rgba(91,163,255,.38);}
.btn-ghost{background:transparent;border-color:var(--rim-hi);color:var(--t2);}
.btn-ghost:hover{background:rgba(255,255,255,.07);border-color:rgba(255,255,255,.22);color:var(--t1);}
.btn-full{width:100%;justify-content:center;}
.btn-sm{height:28px;padding:0 10px;font-size:11px;}

/* ── BADGE ── */
.badge{font-size:11px;padding:2px 8px;border-radius:5px;border:1px solid var(--rim-hi);color:var(--t2);background:var(--base);}
.badge.ok{color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.badge.cnt{color:var(--blue);border-color:var(--blue-b);background:var(--blue-d);font-size:12px;}
.side-badge{display:inline-flex;align-items:center;font-size:8px;font-weight:700;padding:2px 6px;border-radius:4px;}
.side-buy{color:var(--emerald);background:linear-gradient(135deg,rgba(0,224,122,.15),rgba(0,224,122,.06));border:1px solid rgba(0,224,122,.32);}
.side-sell{color:var(--red);background:linear-gradient(135deg,rgba(255,80,112,.15),rgba(255,80,112,.06));border:1px solid rgba(255,80,112,.32);}
.status-accepted{color:var(--emerald);border:1px solid rgba(0,224,122,.32);background:rgba(0,224,122,.1);font-size:8px;font-weight:700;padding:2px 6px;border-radius:4px;display:inline-flex;align-items:center;}
.status-rejected{color:var(--red);border:1px solid rgba(255,80,112,.32);background:rgba(255,80,112,.1);font-size:8px;font-weight:700;padding:2px 6px;border-radius:4px;display:inline-flex;align-items:center;}

/* ══════════════════════════════
   CENTER COL — Volume Top 80 세로 리스트
══════════════════════════════ */
.ticker-card{flex:1;min-height:0;display:flex;flex-direction:column;animation:fu .3s .06s ease both;}

/* 컨트롤바 */
.ticker-controls{
  display:flex;flex-direction:column;gap:5px;
  padding:7px 10px;border-bottom:1px solid rgba(255,255,255,.06);
  flex-shrink:0;
}
.ticker-ctrl-row{display:flex;align-items:center;gap:5px;}
.filter-label{font-size:11px;color:var(--t2);letter-spacing:.5px;white-space:nowrap;}
.filter-input{height:28px;width:52px;background:rgba(5,7,14,.65);border:1px solid rgba(255,255,255,.15);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:12px;padding:0 6px;outline:none;}
.top-sel-info{font-size:12px;color:var(--t2);font-weight:600;}

/* 세로 스크롤 리스트 */
.ticker-list-wrap{flex:1;min-height:0;overflow-y:auto;padding:4px 0;
  scrollbar-width:thin;scrollbar-color:rgba(255,255,255,.12) transparent;}
.ticker-list-wrap::-webkit-scrollbar{width:3px;}
.ticker-list-wrap::-webkit-scrollbar-thumb{background:rgba(255,255,255,.15);border-radius:2px;}

/* 종목 행 */
.t-row{
  display:grid;
  grid-template-columns:22px 60px 1fr 66px 52px;
  align-items:center;gap:6px;
  padding:7px 10px;
  cursor:pointer;transition:background .12s;border-bottom:1px solid rgba(255,255,255,.03);
}
.t-row:hover{background:rgba(255,255,255,.04);}
.t-row.sel{background:rgba(91,163,255,.1);border-bottom-color:rgba(91,163,255,.08);}
.t-row:last-child{border-bottom:none;}
.t-rank{font-size:11px;color:var(--t3);text-align:right;}
.t-rank.r1{color:var(--gold);}
.t-rank.r2{color:#d0dae8;}
.t-rank.r3{color:#e0a878;}
.t-sym{font-size:11px;font-weight:700;color:var(--t1);letter-spacing:.3px;}
.t-nm{font-size:10px;color:var(--t2);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.t-price{font-size:11px;font-weight:600;color:var(--t1);text-align:right;}
.t-rate{font-size:11px;font-weight:600;text-align:right;}
.t-rate.up{color:var(--emerald);}
.t-rate.dn{color:var(--red);}
.t-rate.fl{color:var(--t2);}

/* ══════════════════════════════
   RIGHT COL — Order History 풀 높이 테이블
══════════════════════════════ */
.ord-section{flex:1;min-height:0;display:flex;flex-direction:column;animation:fu .3s .09s ease both;}
.ord-toolbar{
  display:flex;align-items:center;gap:5px;flex-wrap:wrap;
  padding:7px 12px;border-bottom:1px solid rgba(255,255,255,.06);
  background:linear-gradient(180deg,rgba(24,27,40,.92),rgba(19,22,31,.88));
  flex-shrink:0;
}
.tbl-search{
  flex:1;min-width:140px;height:27px;background:rgba(5,7,14,.65);border:1px solid rgba(255,255,255,.1);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;outline:none;
}
.tbl-search:focus{border-color:rgba(198,255,94,.45);}
.tbl-search::placeholder{color:var(--t3);}
.side-btns{display:flex;gap:2px;}
.side-btn{
  height:27px;padding:0 9px;font-family:var(--mono);font-size:8px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:var(--r);background:transparent;color:var(--t2);cursor:pointer;transition:all .12s;
}
.side-btn.act{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.side-btn.act-buy{background:var(--emerald-d);border-color:var(--emerald-b);color:var(--emerald);}
.side-btn.act-sell{background:var(--red-d);border-color:var(--red-b);color:var(--red);}

/* 테이블 스크롤 영역 */
.ord-scroll{flex:1;min-height:0;overflow-y:auto;
  scrollbar-width:thin;scrollbar-color:rgba(255,255,255,.15) transparent;}
.ord-scroll::-webkit-scrollbar{width:4px;}
.ord-scroll::-webkit-scrollbar-thumb{background:rgba(255,255,255,.18);border-radius:2px;}

/* 테이블 */
.ord-table{width:100%;border-collapse:collapse;}
.ord-table thead{position:sticky;top:0;z-index:10;
  background:linear-gradient(180deg,rgba(19,22,31,.99),rgba(15,18,26,.97));
}
.ord-table th{
  padding:6px 10px;text-align:left;font-size:7.5px;font-weight:500;
  color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;
  border-bottom:1px solid rgba(255,255,255,.07);white-space:nowrap;
}
.ord-table th.r{text-align:right;}
.ord-table td{
  padding:6px 10px;font-size:10px;color:var(--t2);
  border-bottom:1px solid rgba(255,255,255,.03);vertical-align:middle;white-space:nowrap;
}
.ord-table td.r{text-align:right;}
.ord-table tbody tr{transition:background .1s;cursor:default;}
.ord-table tbody tr:hover{background:rgba(255,255,255,.03);}
.ord-table tbody tr.row-buy td:first-child{border-left:2px solid rgba(0,224,122,.4);}
.ord-table tbody tr.row-sell td:first-child{border-left:2px solid rgba(255,80,112,.4);}
.ord-table tbody tr:last-child td{border-bottom:none;}
.td-sym{font-size:11px;font-weight:700;color:var(--t1);letter-spacing:.3px;white-space:nowrap;}
.td-nm{font-size:9px;color:var(--t3);margin-top:1px;white-space:normal;word-break:keep-all;line-height:1.3;}
.td-price{font-size:11px;font-weight:600;color:var(--gold);}
.td-qty{color:var(--t1);}
.td-id{font-size:8px;color:rgba(108,125,150,.45);}
.td-time{font-size:9px;color:var(--t3);}
.ord-empty-row td{text-align:center;padding:32px;font-size:10px;color:var(--t3);letter-spacing:1px;}
.ord-footer{
  display:flex;align-items:center;gap:6px;
  padding:6px 12px;border-top:1px solid rgba(255,255,255,.05);
  background:linear-gradient(0deg,rgba(14,16,23,.98),rgba(19,22,31,.88));flex-shrink:0;
}
.page-info{font-size:9px;color:var(--t2);}

/* ── TOAST ── */
.toast{
  position:fixed;right:14px;bottom:14px;z-index:999;
  max-width:360px;background:var(--panel-hi);border:1px solid var(--rim-hi);
  color:var(--t1);border-radius:10px;padding:10px 14px;
  font-size:11px;letter-spacing:.3px;opacity:0;transform:translateY(8px);
  pointer-events:none;transition:.2s;
}
.toast.show{opacity:1;transform:translateY(0);}
.toast.ok{border-color:var(--emerald-b);color:var(--emerald);}
.toast.err{border-color:var(--red-b);color:var(--red);}
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
    <a class="tb-a cur" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-mkt"><span class="mkt-pill kr">KR</span></div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page" id="page">

  <!-- ══ Row 1: P&L 3박스 ══ -->
  <div class="pnl-row">
    <div class="pnl-box">
      <div><div class="pnl-label">그저께</div><div class="pnl-date" id="pnlDate0">--</div></div>
      <div style="text-align:right;margin-left:auto;">
        <div class="pnl-amount zero" id="pnlAmt0">--</div>
        <div class="pnl-sub" id="pnlSub0">로딩 중…</div>
      </div>
    </div>
    <div class="pnl-box">
      <div><div class="pnl-label">어제</div><div class="pnl-date" id="pnlDate1">--</div></div>
      <div style="text-align:right;margin-left:auto;">
        <div class="pnl-amount zero" id="pnlAmt1">--</div>
        <div class="pnl-sub" id="pnlSub1">로딩 중…</div>
      </div>
    </div>
    <div class="pnl-box" style="border:1px solid rgba(198,255,94,.15);background:linear-gradient(148deg,rgba(25,30,42,.97),rgba(16,20,30,.95));">
      <div><div class="pnl-label" style="color:var(--lime);">오늘</div><div class="pnl-date" id="pnlDate2">--</div></div>
      <div style="text-align:right;margin-left:auto;">
        <div class="pnl-amount zero" id="pnlAmt2">--</div>
        <div class="pnl-sub" id="pnlSub2">로딩 중…</div>
      </div>
    </div>
  </div>

  <!-- ══ Row 2: 3컬럼 ══ -->
  <div class="main-area">

    <!-- 좌: Engine · Running · Manual -->
    <div class="left-col">

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
          <div class="engine-box" id="engBox">
            <div class="eng-indicator stop" id="engInd"><div class="eng-dot"></div></div>
            <div class="eng-info">
              <div class="eng-state stop" id="engState">STOPPED</div>
              <div class="eng-sub" id="engMsg">엔진이 정지되어 있습니다</div>
            </div>
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
        <div class="card-bd" style="overflow-y:auto;flex:1;min-height:0;max-height:100px;">
          <div class="run-chips" id="runList">
            <span class="no-run">실행 중인 종목 없음</span>
          </div>
        </div>
      </div>

      <!-- Manual Start -->
      <div class="card manual-card" style="animation:fu .3s .1s ease both;flex:1;">
        <div class="card-hd">
          <div class="card-hd-l">
            <div class="hd-dot" style="background:var(--blue)"></div>
            <span class="card-title">Manual Start</span>
          </div>
        </div>
        <div class="card-bd" style="flex:1;">
          <div class="input-row">
            <div class="suggest-wrap">
              <input class="tb-input" id="symInput" placeholder="종목코드 또는 이름" maxlength="24" autocomplete="off" spellcheck="false"/>
              <div class="sym-suggest" id="symSuggest"></div>
            </div>
          </div>
          <div style="margin-top:4px;">
            <input class="tb-input" id="buyAmountInput" type="number" min="0" step="1" placeholder="종목당 금액" style="width:100%;"/>
          </div>
          <div class="manual-row">
            <select class="tb-sel" id="wlFolderSel" style="flex:1;"></select>
            <button class="btn btn-ghost btn-sm" onclick="loadWatchlistToManual()">WL</button>
            <button class="btn btn-lime btn-sm" onclick="addManual()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              추가
            </button>
          </div>
          <div class="manual-chips" id="manualChips" style="margin-top:4px;"><span class="empty-hint">추가된 종목 없음</span></div>
          <div class="btn-row" style="margin-top:auto;padding-top:8px;flex-direction:column;">
            <button class="btn btn-green btn-full" onclick="startManual()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"/></svg>
              선택 종목 시작
            </button>
            <button class="btn btn-red btn-full" onclick="stopAll()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>
              전체 중지
            </button>
          </div>
        </div>
      </div>

    </div><!-- /left-col -->

    <!-- 중앙: Volume Top 80 세로 리스트 -->
    <div class="center-col">
      <div class="card ticker-card">
        <div class="card-hd">
          <div class="card-hd-l">
            <div class="hd-dot" style="background:var(--gold)"></div>
            <span class="card-title">거래대금 Top 80</span>
          </div>
          <span class="top-sel-info" id="topSelCount">선택 0</span>
        </div>

        <!-- 컨트롤 -->
        <div class="ticker-controls">
          <div class="ticker-ctrl-row">
            <span class="filter-label">Top N</span>
            <input class="filter-input" id="topN" type="number" min="1" max="80" value="3"/>
            <input class="tb-input" id="buyAmountTop" type="number" min="0" step="1" placeholder="종목당 금액" style="flex:1;height:26px;font-size:10px;"/>
          </div>
          <div class="ticker-ctrl-row">
            <button class="btn btn-blue btn-sm btn-full" onclick="startTop()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polygon points="5 3 19 12 5 21 5 3"/></svg>
              Top 시작
            </button>
            <button class="btn btn-ghost btn-sm" onclick="fetchTop()" style="flex-shrink:0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            </button>
          </div>
        </div>

        <!-- 세로 리스트 -->
        <div class="ticker-list-wrap" id="topList">
          <div style="padding:16px 10px;font-size:9px;color:var(--t3);">로딩 중…</div>
        </div>
      </div>
    </div><!-- /center-col -->

    <!-- 우: Order History 테이블 -->
    <div class="right-col">
      <div class="card ord-section">
        <div class="card-hd">
          <div class="card-hd-l">
            <div class="hd-dot" style="background:var(--gold)"></div>
            <span class="card-title">Order History — KR</span>
          </div>
          <span class="badge" style="font-size:7px;">/api/orders/kr</span>
        </div>
        <div class="ord-toolbar">
          <input class="tbl-search" id="searchInput" placeholder="종목코드 / 종목명 / reason 검색" oninput="applyFilter()"/>
          <div class="side-btns">
            <button class="side-btn act" id="btnAll"  onclick="setSide('ALL')">ALL</button>
            <button class="side-btn"     id="btnBUY"  onclick="setSide('BUY')">BUY</button>
            <button class="side-btn"     id="btnSELL" onclick="setSide('SELL')">SELL</button>
          </div>
          <select class="tb-sel btn-sm" id="limitSel" onchange="fetchOrders(false)">
            <option value="30" selected>30건</option>
            <option value="60">60건</option>
            <option value="120">120건</option>
          </select>
          <button class="btn btn-ghost btn-sm" id="refBtn" onclick="fetchOrders(false)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            새로고침
          </button>
          <span class="page-info" id="pageInfo" style="margin-left:auto;">0건</span>
        </div>

        <div class="ord-scroll">
          <table class="ord-table" id="ordTable">
            <thead>
              <tr>
                <th style="width:140px;">종목</th>
                <th style="width:50px;">구분</th>
                <th>사유</th>
                <th class="r" style="width:80px;">가격</th>
                <th class="r" style="width:40px;">수량</th>
                <th class="r" style="width:130px;">시간</th>
              </tr>
            </thead>
            <tbody id="ordBody">
              <tr class="ord-empty-row"><td colspan="6">로딩 중…</td></tr>
            </tbody>
          </table>
        </div>

        <div class="ord-footer">
          <span class="page-info" id="pageInfo2">0 / 0</span>
          <div style="margin-left:auto;display:flex;gap:5px;">
            <button class="btn btn-ghost btn-sm" onclick="movePage(-1)">← Prev</button>
            <button class="btn btn-ghost btn-sm" onclick="movePage(1)">Next →</button>
          </div>
        </div>
      </div>
    </div><!-- /right-col -->

  </div><!-- /main-area -->
</div><!-- /page -->

<div class="toast" id="toast"></div>

<script>
'use strict';

const B      = '${pageContext.request.contextPath}';
const MARKET = 'KR';
const EXCH   = 'KRX';
const DAYS   = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const PAGE_SZ = 50;

let manualSyms = [];
let topRows    = [];
let topSel     = new Set();
let rawOrders  = [];
let sideFilter = 'ALL';
let curPage    = 1;

const symInputEl   = document.getElementById('symInput');
const symSuggestEl = document.getElementById('symSuggest');
let suggestRows = [], suggestIdx = -1, suggestTimer = null, suggestReqId = 0, suggestAbortController = null;
const SUGGEST_LIMIT = 12;

/* ── 시계 ── */
function p2(v){return String(v).padStart(2,'0');}
function tick(){
  const n=new Date();
  document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000); tick();

/* ── 유틸 ── */
function esc(s){return String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function toast(msg,type){
  const el=document.getElementById('toast');
  el.textContent=msg; el.className='toast show '+(type||'');
  clearTimeout(el._t); el._t=setTimeout(()=>el.className='toast',2800);
}
function post(path,data){
  return fetch(B+path,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data||{}).toString()}).then(r=>r.json());
}
function get(path){return fetch(B+path).then(r=>r.json());}
function fmtVol(v){
  const n=Number(v||0);
  if(n>=1e12)return(n/1e12).toFixed(1)+'조';
  if(n>=1e8)return(n/1e8).toFixed(0)+'억';
  if(n>=1e4)return(n/1e4).toFixed(0)+'만';
  return n.toLocaleString();
}
function fmtWon(v){
  const n=Number(v||0);const abs=Math.abs(n);const sign=n<0?'-':'';
  if(abs>=1e8)return sign+(abs/1e8).toFixed(1)+'억';
  if(abs>=1e4)return sign+(abs/1e4).toFixed(0)+'만';
  return sign+abs.toLocaleString()+'원';
}
function fmtTime(v){
  if(Array.isArray(v)&&v.length>=6)return v[0]+'-'+p2(v[1])+'-'+p2(v[2])+' '+p2(v[3])+':'+p2(v[4])+':'+p2(v[5]);
  if(typeof v==='string')return v.replace('T',' ').substring(0,19);
  return '-';
}
function fmtDate(s){
  if(!s||s.length<10)return s||'--';
  const [y,m,d]=s.split('-'); return m+'/'+d;
}

/* ── Engine Status ── */
function setEngine(status,msg){
  const run=String(status||'').indexOf('RUNNING')===0;
  const ind=document.getElementById('engInd');
  const st=document.getElementById('engState');
  const box=document.getElementById('engBox');
  ind.className='eng-indicator '+(run?'run':'stop');
  st.className='eng-state '+(run?'run':'stop');
  st.textContent=run?'RUNNING':(status||'UNKNOWN');
  if(box)box.className='engine-box'+(run?' is-run':'');
  const sub=document.getElementById('engMsg');
  sub.className='eng-sub'+(run?' run':'');
  const cnt=Number(document.getElementById('runCount')?.textContent||0);
  sub.textContent=msg||(run?'자동매매 실행 중 · '+cnt+'종목':'엔진이 정지되어 있습니다');
  document.getElementById('engTime').textContent=p2(new Date().getHours())+':'+p2(new Date().getMinutes())+':'+p2(new Date().getSeconds());
}
function fetchStatus(){get('/api/control/status').then(d=>setEngine(d.status,'')).catch(()=>{});}

/* ── Running Symbols ── */
const nameCache={};
function fetchName(sym){
  if(nameCache[sym]!==undefined)return Promise.resolve(nameCache[sym]);
  return get('/api/watchlist/name?symbol='+encodeURIComponent(sym))
    .then(d=>{nameCache[sym]=d.symbolName||'';return nameCache[sym];})
    .catch(()=>{nameCache[sym]='';return '';});
}
function renderRunning(rows){
  const arr=Array.isArray(rows)?rows:[];
  document.getElementById('runCount').textContent=arr.length;
  const sub=document.getElementById('engMsg');
  if(sub&&sub.classList.contains('run')) sub.textContent='자동매매 실행 중 · '+arr.length+'종목';
  const list=document.getElementById('runList');
  if(!arr.length){list.innerHTML='<span class="no-run">실행 중인 종목 없음</span>';return;}
  Promise.all(arr.map(r=>fetchName(r.symbol||'').then(nm=>({...r,nm})))).then(items=>{
    list.innerHTML=items.map(r=>{
      const sym=esc(r.symbol||'-');
      const nm=r.nm?'<span class="r-name">'+esc(r.nm)+'</span>':'';
      return '<span class="run-chip">'+sym+nm+'</span>';
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
  if(!manualSyms.length){wrap.innerHTML='<span class="empty-hint">추가된 종목 없음</span>';return;}
  Promise.all(manualSyms.map(sym=>fetchName(sym).then(nm=>({sym,nm})))).then(items=>{
    wrap.innerHTML=items.map(({sym,nm})=>
      '<span class="m-chip">'+esc(sym)+(nm?'<span class="m-name">'+esc(nm)+'</span>':'')+
      '<button class="m-chip-del" onclick="removeManual(\''+esc(sym)+'\')">×</button></span>'
    ).join('');
  });
}
window.addManual=function(forced){
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
      const syms=rows
        .filter(it=>(it.exchange||'').toUpperCase()==='KRX'||/^[0-9]/.test(it.symbol||''))
        .filter(it=>!folder||folder==='ALL'||(it.folder||'').trim()===folder)
        .map(it=>String(it.symbol||'').trim()).filter(Boolean);
      manualSyms=Array.from(new Set(syms));
      renderManual();
      toast('Watchlist '+manualSyms.length+'개 로드','ok');
    })
    .catch(()=>toast('Watchlist 불러오기 실패','err'));
};
function renderWatchlistFolders(rows){
  const sel=document.getElementById('wlFolderSel');
  if(!sel)return;
  const folderSet=new Set();
  rows.forEach(it=>{const f=(it.folder||'').trim();if(f)folderSet.add(f);});
  sel.innerHTML='<option value="">폴더 선택</option><option value="ALL">전체</option>';
  Array.from(folderSet).sort().forEach(f=>{
    const opt=document.createElement('option');opt.value=f;opt.textContent=f;sel.appendChild(opt);
  });
}
function resolveBuyAmount(inputId){
  const el=document.getElementById(inputId||'buyAmountInput');
  const raw=String((el&&el.value)||'').trim();
  if(!raw)return null;
  const v=Number(raw);
  if(!Number.isFinite(v)||v<=0){toast('종목당 금액은 0보다 큰 숫자로 입력하세요','err');return undefined;}
  return v;
}

/* ── Suggest ── */
function hideSuggest(){suggestRows=[];suggestIdx=-1;symSuggestEl.classList.remove('show');symSuggestEl.innerHTML='';}
function renderSuggest(){
  if(!suggestRows.length){hideSuggest();return;}
  symSuggestEl.innerHTML=suggestRows.map((row,idx)=>
    '<div class="sym-opt'+(idx===suggestIdx?' act':'')+'" data-idx="'+idx+'">'
      +'<div><div class="sym-opt-sym">'+esc(row.symbol)+'</div><div class="sym-opt-name">'+esc(row.name||row.symbol)+'</div></div>'
      +'<span class="sym-opt-mkt">KR</span>'
    +'</div>'
  ).join('');
  symSuggestEl.classList.add('show');
}
function moveSuggest(step){
  if(!suggestRows.length)return;
  suggestIdx+=step;
  if(suggestIdx<0)suggestIdx=suggestRows.length-1;
  if(suggestIdx>=suggestRows.length)suggestIdx=0;
  renderSuggest();
}
function buildTypedCandidate(raw){
  const typed=String(raw||'').trim().toUpperCase();
  if(!typed)return null;
  if(!/^[0-9]{6}$/.test(typed))return null;
  return {symbol:typed,name:'직접 입력 종목',market:'KR'};
}
function queueSuggest(raw){
  if(suggestTimer)clearTimeout(suggestTimer);
  const query=String(raw||'').trim();
  if(!query){hideSuggest();return;}
  suggestTimer=setTimeout(()=>{
    const reqId=++suggestReqId;
    if(suggestAbortController)suggestAbortController.abort();
    suggestAbortController=new AbortController();
    const url=B+'/api/market/symbol-suggest?market=KR&exch=KRX&limit='+SUGGEST_LIMIT+'&q='+encodeURIComponent(query);
    fetch(url,{cache:'no-store',signal:suggestAbortController.signal})
      .then(r=>r.ok?r.json():Promise.reject())
      .then(data=>{
        if(reqId!==suggestReqId)return;
        const rows=(Array.isArray(data&&data.data)?data.data:[])
          .map(row=>({symbol:String(row.symbol||'').toUpperCase(),name:String(row.name||'').trim(),market:'KR'}))
          .filter(r=>r.symbol);
        const typed=buildTypedCandidate(query);
        if(typed&&!rows.some(r=>r.symbol===typed.symbol))rows.unshift(typed);
        suggestRows=rows.slice(0,SUGGEST_LIMIT);
        suggestIdx=suggestRows.length?0:-1;
        renderSuggest();
      }).catch(()=>{
        const typed=buildTypedCandidate(query);
        suggestRows=typed?[typed]:[];
        suggestIdx=suggestRows.length?0:-1;
        renderSuggest();
      });
  },140);
}
function pickSuggestAt(idx){const row=suggestRows[idx];if(!row)return null;hideSuggest();return row.symbol;}
symInputEl.addEventListener('input',()=>queueSuggest(symInputEl.value));
symInputEl.addEventListener('keydown',e=>{
  if(e.key==='ArrowDown'){e.preventDefault();moveSuggest(1);return;}
  if(e.key==='ArrowUp'){e.preventDefault();moveSuggest(-1);return;}
  if(e.key==='Escape'){hideSuggest();return;}
  if(e.key==='Enter'){e.preventDefault();
    if(suggestRows.length){const p=pickSuggestAt(suggestIdx>=0?suggestIdx:0);if(p){window.addManual(p);return;}}
    window.addManual();}
});
symSuggestEl.addEventListener('mousedown',e=>{
  const opt=e.target.closest('.sym-opt');if(!opt)return;
  const p=pickSuggestAt(Number(opt.dataset.idx));if(p)window.addManual(p);
});
document.addEventListener('click',e=>{if(!e.target.closest('.suggest-wrap'))hideSuggest();});

/* ── Start / Stop ── */
window.startManual=function(){
  if(!manualSyms.length){toast('종목을 추가하세요');return;}
  const buyAmount=resolveBuyAmount('buyAmountInput');
  if(buyAmount===undefined)return;
  Promise.all(manualSyms.map(sym=>{
    const body={symbol:sym};
    if(buyAmount!=null)body.buyAmount=buyAmount;
    return post('/api/control/start',body);
  })).then(res=>{
    const last=res[res.length-1]||{};
    setEngine(last.status,last.message||'');fetchRunning(true);
    toast('시작: '+manualSyms.join(', '),'ok');
  }).catch(e=>toast('시작 실패: '+(e.message||'error'),'err'));
};
window.stopAll=function(){
  post('/api/control/stop',{}).then(d=>{
    setEngine(d.status,d.message||'');fetchRunning(true);toast('전체 중지','ok');
  }).catch(e=>toast('중지 실패: '+(e.message||'error'),'err'));
};

/* ── P&L ── */
function renderPnl(pts){
  pts.forEach((p,i)=>{
    const pnl=Number(p.pnl||0);
    const amtEl=document.getElementById('pnlAmt'+i);
    const dateEl=document.getElementById('pnlDate'+i);
    const subEl=document.getElementById('pnlSub'+i);
    if(amtEl){amtEl.textContent=pnl===0?'±0':fmtWon(pnl);amtEl.className='pnl-amount '+(pnl>0?'pos':pnl<0?'neg':'zero');}
    if(dateEl)dateEl.textContent=fmtDate(p.date);
    if(subEl)subEl.textContent=pnl===0?'거래 없음':(pnl>0?'수익':'손실');
  });
}
function fetchPnl(){
  get('/api/pnl/recent?market=KR&days=3')
    .then(pts=>{if(Array.isArray(pts)&&pts.length===3)renderPnl(pts);})
    .catch(()=>{});
}

/* ── Volume Top 80 — 세로 리스트 ── */
function renderTop(){
  const wrap=document.getElementById('topList');
  document.getElementById('topSelCount').textContent='선택 '+topSel.size;
  if(!topRows.length){
    wrap.innerHTML='<div style="padding:16px 10px;font-size:9px;color:var(--t3);">데이터 없음</div>';
    return;
  }
  wrap.innerHTML=topRows.map((r,i)=>{
    const sym=String(r.symbol||'');
    const nm=r.name||sym;
    const price=Number(r.stck_prpr||0);
    const rate=Number(r.prdy_ctrt||0);
    const rc=i===0?'r1':i===1?'r2':i===2?'r3':'';
    const sel=topSel.has(sym)?' sel':'';
    const rateClass=rate>0?'up':rate<0?'dn':'fl';
    const rateStr=(rate>0?'+':'')+rate.toFixed(2)+'%';
    return '<div class="t-row'+sel+'" onclick="toggleTop(\''+esc(sym)+'\')" title="'+esc(nm)+'">'
      +'<span class="t-rank '+rc+'">'+(i+1)+'</span>'
      +'<span class="t-sym">'+esc(sym)+'</span>'
      +'<span class="t-nm">'+esc(nm)+'</span>'
      +'<span class="t-price">'+(price?price.toLocaleString():'-')+'</span>'
      +'<span class="t-rate '+rateClass+'">'+rateStr+'</span>'
    +'</div>';
  }).join('');
}
window.toggleTop=function(sym){if(!sym)return;topSel.has(sym)?topSel.delete(sym):topSel.add(sym);renderTop();};
window.fetchTop=function(){
  return get('/api/market/ranking?market=KR&exch=KRX')
    .then(d=>{topRows=(d.data||d.output||[]).slice(0,80);renderTop();})
    .catch(()=>{topRows=[];renderTop();});
};
window.startTop=function(){
  const buyAmount=resolveBuyAmount('buyAmountTop');
  if(buyAmount===undefined)return;
  if(topSel.size>0){
    const sels=Array.from(topSel).filter(s=>/^[0-9]{6}$/.test(String(s||'')));
    if(!sels.length){toast('선택된 종목 코드가 올바르지 않습니다','err');return;}
    Promise.all(sels.map(sym=>{
      const body={symbol:sym};
      if(buyAmount!=null)body.buyAmount=buyAmount;
      return post('/api/control/start',body);
    })).then(res=>{
      const last=res[res.length-1]||{};
      setEngine(last.status,last.message||'');fetchRunning(true);
      toast('선택 종목 시작: '+sels.join(', '),'ok');
    }).catch(e=>toast('실패: '+(e.message||''),'err'));
    return;
  }
  const n=document.getElementById('topN').value||3;
  let url='/api/control/start-top?n='+n+'&minRate=0&market=KR&exch=KRX';
  if(buyAmount!=null)url+='&buyAmount='+encodeURIComponent(buyAmount);
  post(url,{})
    .then(d=>{setEngine(d.status,d.message||'');fetchRunning(true);toast(d.message||'Top '+n+' 시작','ok');})
    .catch(e=>toast('실패: '+(e.message||''),'err'));
};

/* ── Order History — 테이블 ── */
function reasonBadge(reason){
  if(reason==='ACCEPTED')return '<span class="status-accepted">ACCEPTED</span>';
  if(reason==='REJECTED')return '<span class="status-rejected">REJECTED</span>';
  return esc(reason||'-');
}
function filterOrders(){
  const q=(document.getElementById('searchInput').value||'').toLowerCase();
  return rawOrders.filter(r=>{
    const sideOk=sideFilter==='ALL'||r.side===sideFilter;
    const txt=((r.symbol||'')+' '+(r.symbolName||'')+' '+(r.reason||'')).toLowerCase();
    return sideOk&&(!q||txt.includes(q));
  });
}
function renderOrders(){
  const filtered=filterOrders();
  const pageCount=Math.max(1,Math.ceil(filtered.length/PAGE_SZ));
  if(curPage>pageCount)curPage=pageCount;
  if(curPage<1)curPage=1;
  const rows=filtered.slice((curPage-1)*PAGE_SZ,curPage*PAGE_SZ);
  const from=filtered.length?(curPage-1)*PAGE_SZ+1:0;
  const to=Math.min(curPage*PAGE_SZ,filtered.length);
  document.getElementById('pageInfo').textContent=filtered.length+'건';
  document.getElementById('pageInfo2').textContent=from+'-'+to+' / '+filtered.length;
  const tbody=document.getElementById('ordBody');
  if(!rows.length){
    tbody.innerHTML='<tr class="ord-empty-row"><td colspan="6">주문 내역 없음</td></tr>';
    return;
  }
  const syms=[...new Set(rows.map(r=>r.symbol).filter(Boolean))];
  Promise.all(syms.map(s=>fetchName(s))).then(()=>{
    tbody.innerHTML=rows.map(r=>{
      const sideClass=r.side==='BUY'?'side-buy':'side-sell';
      const sym=esc(r.symbol||'-');
      const nm=nameCache[r.symbol]||r.symbolName||'';
      const price=r.price?Number(r.price).toLocaleString():'-';
      const time=fmtTime(r.createdAt);
      const rowClass=r.side==='BUY'?'row-buy':'row-sell';
      return '<tr class="'+rowClass+'">'
        +'<td><div class="td-sym">'+sym+'</div><div class="td-nm">'+esc(nm||r.symbolName||'—')+'</div></td>'
        +'<td><span class="side-badge '+sideClass+'">'+esc(r.side||'-')+'</span></td>'
        +'<td>'+reasonBadge(r.reason)+'</td>'
        +'<td class="r td-price">₩'+price+'</td>'
        +'<td class="r td-qty">'+esc(r.quantity||'-')+'</td>'
        +'<td class="r td-time">'+esc(time)+'</td>'
      +'</tr>';
    }).join('');
  });
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
  const btn=document.getElementById('refBtn');btn.textContent='…';
  const limit=document.getElementById('limitSel').value||30;
  get('/api/orders/kr?limit='+limit)
    .then(rows=>{rawOrders=Array.isArray(rows)?rows:[];if(!keepPage)curPage=1;renderOrders();})
    .catch(()=>toast('주문 조회 실패','err'))
    .finally(()=>btn.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:9px;height:9px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg> 새로고침');
};

/* ── 초기 로드 ── */
fetchStatus();
fetchRunning(false);
fetchTop();
fetchOrders(false);
fetchPnl();
get('/api/watchlist').then(items=>renderWatchlistFolders(Array.isArray(items)?items:[])).catch(()=>{});

/* ── 폴링 ── */
setInterval(()=>{fetchStatus();fetchRunning(true);fetchOrders(true);},5000);
setInterval(fetchTop,60000);
setInterval(fetchPnl,30000);
</script>
</body>
</html>
