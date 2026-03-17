<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AUTO TRADING — DASHBOARD</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#060709;--base:#0a0c10;--panel:#141720;--panel-hi:#191d28;--hover:#1e2330;
  --lime:#a8ff3e;--lime-d:rgba(168,255,62,.1);--lime-b:rgba(168,255,62,.22);--lime-glow:0 0 16px rgba(168,255,62,.4);
  --emerald:#00d97e;--emerald-d:rgba(0,217,126,.08);--emerald-b:rgba(0,217,126,.25);--emerald-glow:0 0 10px rgba(0,217,126,.4);
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);--red-glow:0 0 10px rgba(255,77,106,.4);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:52px;--sidebar-w:280px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);overflow:hidden;}

/* 배경 */
.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.07) 0%,transparent 55%),
    radial-gradient(ellipse 40% 50% at 100% 100%,rgba(0,217,126,.04) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.045) 1px,transparent 1px);background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-10px);}to{opacity:1;transform:translateY(0);}}
@keyframes fu{from{opacity:0;transform:translateY(12px);}to{opacity:1;transform:translateY(0);}}
@keyframes sir{from{opacity:0;transform:translateX(16px);}to{opacity:1;transform:translateX(0);}}
@keyframes pd{0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.7);opacity:.3;}}
@keyframes sp{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes fl{0%{background:rgba(168,255,62,.1);}100%{background:transparent;}}
@keyframes ri{from{opacity:0;transform:translateX(4px);}to{opacity:1;transform:translateX(0);}}

/* ══ TOPBAR ══ */
.topbar{position:fixed;top:0;left:0;right:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.93);backdrop-filter:blur(16px);
  border-bottom:1px solid var(--rim);animation:sd .4s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime),rgba(168,255,62,.3),transparent);opacity:.4;}
.tb-logo{display:flex;align-items:center;gap:10px;padding:0 18px;height:100%;border-right:1px solid var(--rim);min-width:190px;}
.logo-mk{width:30px;height:30px;background:var(--lime);border-radius:7px;display:flex;align-items:center;justify-content:center;
  position:relative;overflow:hidden;box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk::before{content:'';position:absolute;inset:0;background:linear-gradient(135deg,rgba(255,255,255,.3) 0%,transparent 60%);}
.logo-mk svg{width:15px;height:15px;}
.logo-name{font-size:13px;font-weight:800;letter-spacing:.5px;color:var(--t1);}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-pill{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:10px;color:var(--emerald);
  padding:3px 10px;border-radius:20px;background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);box-shadow:var(--emerald-glow);animation:pd 1.4s ease-in-out infinite;}
.tb-nav{display:flex;align-items:center;gap:3px;padding:0 14px;}
.tb-a{font-family:var(--mono);font-size:10px;letter-spacing:.4px;padding:4px 10px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-login{display:flex;align-items:center;gap:5px;padding:0 12px;border-left:1px solid var(--rim);}
.tb-login form{display:flex;align-items:center;gap:5px;}
.tb-login input{height:26px;width:108px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;}
.tb-login input::placeholder{color:var(--t3);}
.tb-lbtn{height:26px;padding:0 10px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:10px;cursor:pointer;transition:all .15s;}
.tb-lbtn:hover{background:var(--lime);color:var(--void);}
.tb-login-st{display:none;align-items:center;gap:7px;font-family:var(--mono);font-size:10px;color:var(--t2);}
.tb-login-st .acc{color:var(--lime);}
.tb-lerr{font-family:var(--mono);font-size:10px;color:var(--red);display:none;margin-left:3px;}
.tb-clock{padding:0 14px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:14px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1px;}

/* ══ SHELL ══ */
.shell{display:flex;height:100vh;padding-top:var(--topbar-h);}
.main{flex:1;min-width:0;display:flex;flex-direction:column;overflow:hidden;}
.body{flex:1;min-height:0;overflow-y:auto;padding:12px;display:flex;flex-direction:column;gap:10px;}
.body::-webkit-scrollbar{width:4px;}
.body::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}

/* ══ KPI ROW ══ */
.kpi-row{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;flex-shrink:0;animation:fu .4s .05s ease both;}
.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  padding:10px 14px;position:relative;overflow:hidden;transition:border-color .2s;cursor:default;}
.kpi:hover{border-color:var(--rim-hi);}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:1px;}
.kl::before{background:var(--lime);box-shadow:var(--lime-glow);}
.ke::before{background:var(--emerald);}
.kr::before{background:var(--red);}
.kg::before{background:var(--gold);}
.kb::before{background:var(--blue);}
.k-lbl{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:5px;}
.k-val{font-family:var(--mono);font-size:20px;font-weight:500;line-height:1;letter-spacing:-0.5px;}
.kl .k-val{color:var(--lime);}
.ke .k-val{color:var(--emerald);}
.kr .k-val{color:var(--red);}
.kg .k-val{color:var(--gold);}
.kb .k-val{color:var(--blue);}
.k-sub{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:2px;}

/* ══ CONTENT GRID ══ */
.content-grid{display:grid;grid-template-columns:1.45fr 1fr;grid-template-rows:auto auto;gap:10px;flex:1;min-height:0;animation:fu .4s .1s ease both;}

/* ══ 공통 PANEL ══ */
.pn{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;min-height:0;}
.ph{flex-shrink:0;height:34px;display:flex;align-items:center;justify-content:space-between;
  padding:0 12px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.ph-l{display:flex;align-items:center;gap:7px;}
.ph-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;}
.ph-nm{font-family:var(--mono);font-size:9px;font-weight:500;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.ph-bd{font-family:var(--mono);font-size:9px;padding:2px 8px;border-radius:8px;border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.ph-bd.ok{color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.ph-bd.up{color:var(--red);border-color:var(--red-b);background:var(--red-d);}
.ph-bd.dn{color:#60a5fa;border-color:rgba(96,165,250,.3);background:rgba(96,165,250,.08);}

/* ══ 차트 패널 (좌상, 2행 span) ══ */
.chart-pn{grid-column:1;grid-row:1/3;}

/* 툴바 */
.c-toolbar{flex-shrink:0;display:flex;align-items:center;gap:5px;flex-wrap:wrap;
  padding:6px 10px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.mkt-grp{display:flex;gap:2px;}
.mkt-b{font-family:var(--mono);font-size:8px;letter-spacing:1px;padding:3px 9px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-b.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-b.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.exch-s{height:24px;background:var(--panel);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t2);font-family:var(--mono);font-size:9px;padding:0 6px;outline:none;cursor:pointer;display:none;}
.exch-s.show{display:block;}
option{background:var(--panel-hi);}
.sym-in{height:24px;width:80px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t1);font-family:var(--mono);font-size:11px;letter-spacing:1px;padding:0 7px;outline:none;transition:border-color .15s;}
.sym-in:focus{border-color:var(--lime-b);}
.go-b{height:24px;padding:0 10px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:8px;letter-spacing:1px;cursor:pointer;transition:all .12s;}
.go-b:hover{background:var(--lime);color:var(--void);}
.tf-grp{display:flex;gap:2px;margin-left:auto;}
.tf-b{font-family:var(--mono);font-size:8px;letter-spacing:.3px;padding:3px 6px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.tf-b.on{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tf-b:hover:not(.on){color:var(--t1);}

/* 메타 */
.c-meta{flex-shrink:0;padding:8px 12px 6px;display:flex;align-items:baseline;gap:8px;border-bottom:1px solid var(--rim);}
.cm-nm{font-size:12px;font-weight:700;color:var(--t1);}
.cm-sym{font-family:var(--mono);font-size:10px;color:var(--t3);}
.cm-pr{font-family:var(--mono);font-size:18px;font-weight:600;color:var(--t1);letter-spacing:-0.5px;}
.cm-dl{font-family:var(--mono);font-size:11px;}
.cm-dl.up{color:var(--red);}
.cm-dl.dn{color:#60a5fa;}

/* 범례/줌 */
.c-legend{flex-shrink:0;display:flex;align-items:center;gap:10px;padding:4px 10px;
  border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.lg-item{display:flex;align-items:center;gap:4px;font-family:var(--mono);font-size:8px;color:var(--t3);}
.lg-box{width:9px;height:9px;border-radius:2px;}
.zm-grp{margin-left:auto;display:flex;gap:2px;}
.zm-b{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:3px;
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.zm-b:hover{color:var(--t1);border-color:var(--rim-hi);}

/* 캔버스 */
.c-wrap{flex:1;min-height:0;position:relative;background:var(--base);}
#cv{display:block;width:100%;height:100%;}
.c-ov{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
  font-family:var(--mono);font-size:11px;color:var(--t3);letter-spacing:2px;
  background:rgba(10,12,16,.8);pointer-events:none;}
.c-ov.hid{display:none;}
.c-tip{position:absolute;pointer-events:none;background:var(--panel-hi);border:1px solid var(--rim-hi);
  border-radius:4px;padding:6px 10px;font-family:var(--mono);font-size:10px;color:var(--t1);white-space:nowrap;z-index:10;display:none;}
.c-vl,.c-hl{position:absolute;pointer-events:none;display:none;background:rgba(168,255,62,.2);}
.c-vl{width:1px;top:0;bottom:0;} .c-hl{height:1px;left:0;right:0;}

/* ══ 우상단: 통계 ══ */
.stats-pn{grid-column:2;grid-row:1;}
.stat-grid{display:grid;grid-template-columns:1fr 1fr;gap:6px;padding:8px;}
.sc{background:var(--panel-hi);border:1px solid var(--rim);border-radius:var(--r2);padding:8px 10px;}
.sc-l{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:4px;}
.sc-v{font-family:var(--mono);font-size:16px;color:var(--t1);font-weight:500;line-height:1;}
.sc-s{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:2px;}

/* ══ 우하단: 주문+가격 탭 ══ */
.data-pn{grid-column:2;grid-row:2;min-height:0;}
.tab-bar{flex-shrink:0;display:flex;border-bottom:1px solid var(--rim);}
.tab-btn{flex:1;padding:6px 0;text-align:center;font-family:var(--mono);font-size:9px;
  letter-spacing:1.5px;text-transform:uppercase;color:var(--t3);cursor:pointer;
  border-bottom:2px solid transparent;transition:all .15s;background:transparent;border-top:none;border-left:none;border-right:none;}
.tab-btn.on{color:var(--lime);border-bottom-color:var(--lime);}
.tab-content{flex:1;min-height:0;display:none;}
.tab-content.on{display:flex;flex-direction:column;}
.tbl-sc{flex:1;min-height:0;overflow-y:auto;}
.tbl-sc::-webkit-scrollbar{width:3px;}
.tbl-sc::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
table{width:100%;border-collapse:collapse;font-size:11px;}
thead th{position:sticky;top:0;background:var(--panel-hi);font-family:var(--mono);font-size:8px;
  color:var(--t2);font-weight:400;letter-spacing:1.5px;text-transform:uppercase;
  padding:6px 10px;text-align:left;border-bottom:1px solid var(--rim);white-space:nowrap;}
tbody td{padding:7px 10px;border-bottom:1px solid var(--t4);vertical-align:middle;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
tbody tr:hover td{background:var(--hover);}
tbody tr:last-child td{border-bottom:none;}
.et{text-align:center;padding:20px!important;font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:2px;}
.c-id{font-family:var(--mono);font-size:10px;color:var(--t3);}
.c-sym{font-family:var(--mono);font-size:11px;font-weight:600;color:var(--t1);}
.s-buy{font-family:var(--mono);font-size:9px;font-weight:600;padding:1px 6px;border-radius:3px;
  color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.s-sell{font-family:var(--mono);font-size:9px;font-weight:600;padding:1px 6px;border-radius:3px;
  color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.c-pr{font-family:var(--mono);font-size:11px;color:var(--gold);font-weight:500;}
.c-lpr{font-family:var(--mono);font-size:11px;color:var(--lime);font-weight:500;}
.c-mu{font-family:var(--mono);font-size:10px;color:var(--t3);}

/* ══ RIGHT SIDEBAR ══ */
.sidebar{width:var(--sidebar-w);height:100%;background:var(--base);border-left:1px solid var(--rim);
  display:flex;flex-direction:column;overflow:hidden;animation:sir .5s .15s ease both;}
.sb-hd{flex-shrink:0;background:var(--panel-hi);border-bottom:1px solid var(--rim);}
.sb-r1{display:flex;align-items:center;justify-content:space-between;padding:9px 12px 4px;}
.sb-title{display:flex;align-items:center;gap:6px;font-family:var(--mono);font-size:9px;font-weight:600;
  color:var(--lime);letter-spacing:2px;text-transform:uppercase;}
.sb-tdot{width:6px;height:6px;border-radius:50%;background:var(--lime);box-shadow:var(--lime-glow);animation:pd 2s ease-in-out infinite;}
.sb-btns{display:flex;gap:3px;}
.sb-btn{width:24px;height:24px;border-radius:var(--r);border:1px solid var(--rim-hi);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .12s;display:flex;align-items:center;justify-content:center;padding:0;}
.sb-btn:hover{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.sb-btn.act{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.sb-btn.spin svg{animation:sp .5s linear infinite;}
.sb-mkt{display:flex;gap:3px;padding:4px 12px 8px;}
.mkt-sb{flex:1;height:22px;font-family:var(--mono);font-size:8px;letter-spacing:1px;text-transform:uppercase;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-sb.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-sb.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-r2{display:flex;align-items:center;justify-content:space-between;padding:0 12px 7px;}
.sb-upd{font-family:var(--mono);font-size:8px;color:var(--t3);}
.sb-cd{display:flex;align-items:center;gap:3px;}
.sb-cdl{font-family:var(--mono);font-size:8px;color:var(--t3);}
.sb-cdn{font-family:var(--mono);font-size:9px;font-weight:600;color:var(--lime);min-width:18px;text-align:right;}
.sb-pg{height:2px;background:var(--t4);flex-shrink:0;position:relative;}
.sb-pgf{position:absolute;top:0;left:0;height:100%;width:0%;
  background:linear-gradient(90deg,var(--lime),var(--emerald));box-shadow:0 0 5px var(--lime);transition:width 1s linear;}
.sb-tabs{flex-shrink:0;display:flex;border-bottom:1px solid var(--rim);background:var(--panel);}
.sb-tab{flex:1;padding:6px 0;text-align:center;font-family:var(--mono);font-size:8px;letter-spacing:1.5px;
  text-transform:uppercase;color:var(--t3);cursor:pointer;user-select:none;border-bottom:2px solid transparent;transition:all .12s;}
.sb-tab:hover{color:var(--t2);}
.sb-tab.on{color:var(--lime);border-bottom-color:var(--lime);}
.sb-tabs.us .sb-tab.on{color:var(--blue);border-bottom-color:var(--blue);}
.sb-exch{flex-shrink:0;display:none;gap:3px;padding:4px 10px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.sb-exch.show{display:flex;}
.ex-b{flex:1;height:20px;font-family:var(--mono);font-size:8px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.ex-b.on{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-body{flex:1;overflow-y:auto;scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.sb-body::-webkit-scrollbar{width:3px;}
.sb-body::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
.sb-msg{padding:36px 16px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:8px;}
.sb-mi{font-size:20px;}
.sb-mt{font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:1.5px;}
.sb-msg.ld .sb-mi{color:var(--lime);animation:pd 1s ease-in-out infinite;}
.sb-msg.er .sb-mi{color:var(--red);} .sb-msg.er .sb-mt{color:var(--red);font-size:9px;}

/* 랭킹 행 */
.rk-list{padding:2px 0;}
.rk-row{display:grid;grid-template-columns:22px 1fr auto;align-items:center;gap:6px;
  padding:6px 10px;border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;animation:ri .2s ease both;}
.rk-row:last-child{border-bottom:none;}
.rk-row:hover{background:var(--hover);}
.rk-row.fl{animation:fl .6s ease both;}
.rn{font-family:var(--mono);font-size:10px;font-weight:700;text-align:center;flex-shrink:0;}
.rn.r1{color:var(--gold);font-size:12px;text-shadow:0 0 6px rgba(245,200,66,.5);}
.rn.r2{color:#b0b8c8;}
.rn.r3{color:#cd8b5a;}
.rn.rN{color:var(--t3);font-size:9px;}
.ri-i{min-width:0;}
.ri-nm{font-size:11px;font-weight:600;color:var(--t1);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;margin-bottom:1px;}
.ri-cd{font-family:var(--mono);font-size:8px;color:var(--t3);}
.ri-bw{margin-top:3px;}
.ri-b{height:2px;border-radius:1px;transition:width .5s ease;}
.ri-b.kr{background:linear-gradient(90deg,var(--lime),rgba(168,255,62,.1));}
.ri-b.us{background:linear-gradient(90deg,var(--blue),rgba(77,159,255,.1));}
.ri-vl{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:1px;}
.rp-i{text-align:right;flex-shrink:0;}
.rp-pr{font-family:var(--mono);font-size:10px;font-weight:500;color:var(--t1);white-space:nowrap;}
.rp-ch{font-family:var(--mono);font-size:9px;margin-top:2px;display:flex;align-items:center;justify-content:flex-end;white-space:nowrap;}
.rp-ch.ku{color:var(--red);} .rp-ch.kd{color:#60a5fa;}
.rp-ch.uu{color:var(--emerald);} .rp-ch.ud{color:var(--red);}
.rp-ch.fl{color:var(--t3);}
.sb-ft{flex-shrink:0;padding:5px 10px;border-top:1px solid var(--rim);background:var(--panel-hi);
  font-family:var(--mono);font-size:7px;color:var(--t3);text-align:center;letter-spacing:.5px;}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<!-- TOPBAR -->
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
  <div class="tb-pill"><div class="tb-dot"></div><span id="hdSt">LOADING</span></div>
  <div class="tb-nav">
    <a class="tb-a cur" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-login">
    <form id="lf">
      <input type="text" name="accountNo" placeholder="12345678-01" pattern="[0-9]{8}-[0-9]{2}" autocomplete="off"/>
      <input type="password" name="accountPassword" placeholder="Password" autocomplete="off"/>
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

<div class="shell">
  <div class="main">
    <div class="body">

      <!-- KPI -->
      <div class="kpi-row">
        <div class="kpi kl"><div class="k-lbl">Watchlist</div><div class="k-val" id="kW">—</div><div class="k-sub">감시 종목</div></div>
        <div class="kpi ke"><div class="k-lbl">Positions</div><div class="k-val" id="kP">—</div><div class="k-sub">포지션</div></div>
        <div class="kpi kg"><div class="k-lbl">Last Symbol</div><div class="k-val" id="kS" style="font-size:14px;padding-top:3px">—</div><div class="k-sub" id="kSS">—</div></div>
        <div class="kpi kr"><div class="k-lbl">Status</div><div class="k-val" id="kSt" style="font-size:14px;padding-top:3px">—</div><div class="k-sub">엔진 상태</div></div>
        <div class="kpi kb"><div class="k-lbl">Orders</div><div class="k-val" id="kO">—</div><div class="k-sub">최근 주문</div></div>
      </div>

      <!-- CONTENT GRID -->
      <div class="content-grid" style="flex:1;min-height:0;">

        <!-- ── 차트 패널 (좌, 2행) ── -->
        <div class="pn chart-pn">
          <div class="ph">
            <div class="ph-l">
              <div class="ph-dot" style="background:var(--lime);box-shadow:var(--lime-glow)"></div>
              <div class="ph-nm">Price Chart</div>
            </div>
            <span class="ph-bd" id="cBadge">5m · KR</span>
          </div>
          <div class="c-toolbar">
            <div class="mkt-grp">
              <button class="mkt-b on kr" id="mKR" onclick="setMkt('KR')">🇰🇷 KR</button>
              <button class="mkt-b us"    id="mUS" onclick="setMkt('US')">🇺🇸 US</button>
            </div>
            <select class="exch-s" id="exchS" onchange="onExch()">
              <option value="NAS">NASDAQ</option>
              <option value="NYS">NYSE</option>
              <option value="AMS">AMEX</option>
            </select>
            <input class="sym-in" id="symIn" value="005930" autocomplete="off" spellcheck="false"/>
            <button class="go-b" onclick="fetchChart()">조회</button>
            <div class="tf-grp">
              <button class="tf-b"    data-tf="1m"  onclick="setTf('1m',this)">1m</button>
              <button class="tf-b on" data-tf="5m"  onclick="setTf('5m',this)">5m</button>
              <button class="tf-b"    data-tf="15m" onclick="setTf('15m',this)">15m</button>
              <button class="tf-b"    data-tf="30m" onclick="setTf('30m',this)">30m</button>
              <button class="tf-b"    data-tf="60m" onclick="setTf('60m',this)">60m</button>
              <button class="tf-b"    data-tf="1d"  onclick="setTf('1d',this)">1D</button>
              <button class="tf-b"    data-tf="1w"  onclick="setTf('1w',this)">1W</button>
              <button class="tf-b"    data-tf="1mo" onclick="setTf('1mo',this)">1M</button>
            </div>
          </div>
          <div class="c-meta">
            <span class="cm-nm" id="cmN">—</span>
            <span class="cm-sym" id="cmS"></span>
            <span class="cm-pr" id="cmP">—</span>
            <span class="cm-dl" id="cmD">—</span>
          </div>
          <div class="c-legend">
            <div class="lg-item"><div class="lg-box" style="background:var(--emerald)"></div>상승</div>
            <div class="lg-item"><div class="lg-box" style="background:var(--red)"></div>하락</div>
            <div class="lg-item"><div class="lg-box" style="background:rgba(168,255,62,.4);border:1px solid var(--lime)"></div>MA20</div>
            <div class="zm-grp">
              <button class="zm-b" onclick="zm(-1)">− Zoom</button>
              <button class="zm-b" onclick="zm(1)">+ Zoom</button>
              <button class="zm-b" onclick="zmR()">Reset</button>
            </div>
          </div>
          <div class="c-wrap" id="cWrap">
            <canvas id="cv"></canvas>
            <div class="c-vl" id="cvl"></div>
            <div class="c-hl" id="chl"></div>
            <div class="c-tip" id="ctp"></div>
            <div class="c-ov" id="cOv">로딩 중…</div>
          </div>
        </div>

        <!-- ── 우상: 통계 ── -->
        <div class="pn stats-pn">
          <div class="ph">
            <div class="ph-l">
              <div class="ph-dot" style="background:var(--emerald);box-shadow:var(--emerald-glow)"></div>
              <div class="ph-nm">Today's Stats</div>
            </div>
            <span class="ph-bd ok" id="stTime">--:--:--</span>
          </div>
          <div class="stat-grid">
            <div class="sc"><div class="sc-l">Status</div><div class="sc-v" id="scSt">—</div></div>
            <div class="sc"><div class="sc-l">Watchlist</div><div class="sc-v" id="scW">—</div><div class="sc-s">감시 종목</div></div>
            <div class="sc"><div class="sc-l">Positions</div><div class="sc-v" id="scP">—</div></div>
            <div class="sc"><div class="sc-l">Last Order</div><div class="sc-v" id="scL" style="font-size:12px">—</div></div>
          </div>
          <!-- Quick Nav -->
          <div style="display:flex;gap:6px;padding:6px 8px;border-top:1px solid var(--rim);">
            <a href="${pageContext.request.contextPath}/control/kr" style="flex:1;text-align:center;padding:6px;background:var(--emerald-d);border:1px solid var(--emerald-b);color:var(--emerald);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:9px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--emerald)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--emerald-d)';this.style.color='var(--emerald)'">⚡ Control KR</a>
            <a href="${pageContext.request.contextPath}/control/us" style="flex:1;text-align:center;padding:6px;background:var(--lime-d);border:1px solid var(--lime-b);color:var(--lime);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:9px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--lime)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--lime-d)';this.style.color='var(--lime)'">⚡ Control US</a>
            <a href="${pageContext.request.contextPath}/history/orders" style="flex:1;text-align:center;padding:6px;background:var(--gold-d);border:1px solid var(--gold-b);color:var(--gold);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:9px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--gold)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--gold-d)';this.style.color='var(--gold)'">📋 Orders</a>
            <a href="${pageContext.request.contextPath}/balances" style="flex:1;text-align:center;padding:6px;background:var(--panel-hi);border:1px solid var(--rim);color:var(--t2);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:9px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--hover)';this.style.color='var(--t1)'" onmouseout="this.style.background='var(--panel-hi)';this.style.color='var(--t2)'">💰 Balances</a>
            <a href="${pageContext.request.contextPath}/watchlist" style="flex:1;text-align:center;padding:6px;background:var(--blue-d);border:1px solid var(--blue-b);color:var(--blue);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:9px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--blue)';this.style.color='#fff'" onmouseout="this.style.background='var(--blue-d)';this.style.color='var(--blue)'">👁 Watchlist</a>
          </div>
        </div>

        <!-- ── 우하: 주문+가격 탭 ── -->
        <div class="pn data-pn">
          <div class="ph">
            <div class="ph-l">
              <div class="ph-dot" style="background:var(--gold)"></div>
              <div class="ph-nm">Live Data</div>
            </div>
          </div>
          <div class="tab-bar">
            <button class="tab-btn on" onclick="showTab('ord',this)">주문 이력</button>
            <button class="tab-btn"    onclick="showTab('prc',this)">가격 로그</button>
          </div>
          <div class="tab-content on" id="tab-ord">
            <div class="tbl-sc">
              <table>
                <thead><tr><th>ID</th><th>Symbol</th><th>Side</th><th>Qty</th><th>Price</th><th>Time</th></tr></thead>
                <tbody id="ordTb"><tr><td class="et" colspan="6">Loading…</td></tr></tbody>
              </table>
            </div>
          </div>
          <div class="tab-content" id="tab-prc">
            <div class="tbl-sc">
              <table>
                <thead><tr><th>Symbol</th><th>Price</th><th>Time</th></tr></thead>
                <tbody id="prcTb"><tr><td class="et" colspan="3">Loading…</td></tr></tbody>
              </table>
            </div>
          </div>
        </div>

      </div><!-- /content-grid -->
    </div><!-- /body -->
  </div><!-- /main -->

  <!-- ══ RIGHT SIDEBAR ══ -->
  <aside class="sidebar">
    <div class="sb-hd">
      <div class="sb-r1">
        <div class="sb-title"><span class="sb-tdot"></span>거래량 순위 TOP30</div>
        <div class="sb-btns">
          <button class="sb-btn act" id="sbAb" onclick="toggleAuto()" title="자동갱신">
            <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <circle cx="12" cy="12" r="9"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="12" x2="15" y2="15"/>
            </svg>
          </button>
          <button class="sb-btn" id="sbRb" onclick="loadRanking()" title="새로고침">
            <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="sb-mkt">
        <button class="mkt-sb on kr" id="smKR" onclick="setSbMkt('KR')">🇰🇷 국내</button>
        <button class="mkt-sb us"    id="smUS" onclick="setSbMkt('US')">🇺🇸 미국</button>
      </div>
      <div class="sb-r2">
        <span class="sb-upd" id="sbUpd">로딩 중…</span>
        <div class="sb-cd">
          <span class="sb-cdl">갱신</span>
          <span class="sb-cdn" id="sbCdn">30</span>
          <span class="sb-cdl">s</span>
        </div>
      </div>
    </div>
    <div class="sb-pg"><div class="sb-pgf" id="sbPgf"></div></div>
    <div class="sb-tabs" id="sbTabs">
      <div class="sb-tab on" id="stVol" onclick="setSbTab('vol')">거래량</div>
      <div class="sb-tab"    id="stChg" onclick="setSbTab('chg')">등락률</div>
      <div class="sb-tab"    id="stHi"  onclick="setSbTab('hi')">고가순</div>
    </div>
    <div class="sb-exch" id="sbExch">
      <button class="ex-b on" id="exNAS" onclick="setSbExch('NAS')">NASDAQ</button>
      <button class="ex-b"    id="exNYS" onclick="setSbExch('NYS')">NYSE</button>
      <button class="ex-b"    id="exAMS" onclick="setSbExch('AMS')">AMEX</button>
    </div>
    <div class="sb-body" id="sbBody">
      <div class="sb-msg ld"><div class="sb-mi">◈</div><div class="sb-mt">로딩 중…</div></div>
    </div>
    <div class="sb-ft" id="sbFt">KIS · /api/market/ranking?market=KR</div>
  </aside>
</div>

<script>
(function(){
'use strict';
const B='${pageContext.request.contextPath}';
const DAYS=['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const POLL=30;

/* 시계 */
function tick(){
  const n=new Date(),p=v=>String(v).padStart(2,'0');
  document.getElementById('clkT').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p(n.getMonth()+1)+'.'+p(n.getDate())+' '+DAYS[n.getDay()];
  const s=document.getElementById('stTime');if(s)s.textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
}
setInterval(tick,1000);tick();

/* Login */
(function(){
  const f=document.getElementById('lf'),sb=document.getElementById('lst'),as=document.getElementById('lacc');
  const lb=document.getElementById('lob'),eb=document.getElementById('lerr'),pat=/^\d{8}-\d{2}$/;
  const sI=m=>{f.style.display='none';sb.style.display='inline-flex';eb.style.display='none';as.textContent=m||'****';};
  const sO=()=>{sb.style.display='none';f.style.display='';eb.style.display='none';};
  const sE=m=>{eb.textContent=m||'';eb.style.display=m?'inline-flex':'none';};
  const post=(u,d)=>fetch(u,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(d).toString()}).then(r=>r.json());
  fetch(B+'/api/auth/status').then(r=>r.json()).then(d=>d&&d.loggedIn?sI(d.accountMasked):sO()).catch(()=>sO());
  f.addEventListener('submit',e=>{
    e.preventDefault();
    const no=(f.accountNo.value||'').trim(),pw=(f.accountPassword.value||'').trim();
    if(!no||!pw)return;if(!pat.test(no)){sE('Format: 12345678-01');return;}
    post(B+'/api/auth/login',{accountNo:no,accountPassword:pw}).then(d=>d.status==='OK'?sI(d.accountMasked):sE(d.message||'Login failed'));
  });
  lb.addEventListener('click',()=>post(B+'/api/auth/logout',{}).then(()=>sO()));
})();

/* 탭 */
window.showTab=function(id,btn){
  document.querySelectorAll('.tab-content').forEach(t=>t.classList.remove('on'));
  document.querySelectorAll('.tab-btn').forEach(b=>b.classList.remove('on'));
  document.getElementById('tab-'+id).classList.add('on');
  btn.classList.add('on');
};

/* 대시보드 */
function loadDash(){
  fetch(B+'/api/dashboard?limit=10').then(r=>r.json()).then(d=>{
    if(!d)return;
    const st=d.status||'STOPPED';
    document.getElementById('hdSt').textContent=st;
    document.getElementById('kW').textContent=d.watchlistCount||0;
    document.getElementById('kP').textContent=d.positionCount||0;
    document.getElementById('kSt').textContent=st;
    document.getElementById('scSt').textContent=st;
    document.getElementById('scW').textContent=d.watchlistCount||0;
    document.getElementById('scP').textContent=d.positionCount||0;
    const ords=d.recentOrders||[];
    document.getElementById('kO').textContent=ords.length;
    if(ords.length){
      const l=ords[0];
      document.getElementById('kS').textContent=l.symbol||'—';
      document.getElementById('kSS').textContent=l.side||'—';
      document.getElementById('scL').textContent=(l.symbol||'')+(l.side?' · '+l.side:'');
    }
    const ob=document.getElementById('ordTb');
    ob.innerHTML=ords.length?ords.map(r=>{
      const sideClass = r.side==='BUY' ? 's-buy' : 's-sell';
      return '<tr>' +
        '<td class="c-id">#' + e(r.id) + '</td>' +
        '<td class="c-sym">' + e(r.symbol) + '</td>' +
        '<td><span class="' + sideClass + '">' + e(r.side) + '</span></td>' +
        '<td>' + e(r.quantity) + '</td>' +
        '<td class="c-pr">' + Number(r.price||0).toLocaleString('ko-KR') + '</td>' +
        '<td class="c-mu">' + e((r.createdAt||'').substring(11,16)) + '</td>' +
      '</tr>';
    }).join(''):'<tr><td class="et" colspan="6">주문 없음</td></tr>';
    const prcs=d.recentPrices||[];
    const pb=document.getElementById('prcTb');
    pb.innerHTML=prcs.length?prcs.map(r=>{
      return '<tr>' +
        '<td class="c-sym">' + e(r.symbol) + '</td>' +
        '<td class="c-lpr">' + e(r.price) + '</td>' +
        '<td class="c-mu">' + e((r.createdAt||'').substring(11,19)) + '</td>' +
      '</tr>';
    }).join(''):'<tr><td class="et" colspan="3">데이터 없음</td></tr>';
  }).catch(()=>{});
}
loadDash();setInterval(loadDash,15000);

/* ══ 캔들 차트 ══ */
let CS={mkt:'KR',exch:'NAS',sym:'005930',tf:'5m',pts:[],zS:0,zE:1,drag:false,dragX:0,dragZ:null};

window.setMkt=function(m){
  CS.mkt=m;
  ['KR','US'].forEach(x=>document.getElementById('m'+x).className='mkt-b'+(x===m?(m==='KR'?' on kr':' on us'):''));
  document.getElementById('exchS').classList.toggle('show',m==='US');
  if(m==='US'&&!/^[A-Za-z]/.test(CS.sym)){CS.sym='AAPL';document.getElementById('symIn').value='AAPL';}
  if(m==='KR'&&!/^[0-9]/.test(CS.sym)){CS.sym='005930';document.getElementById('symIn').value='005930';}
  fetchChart();
};
window.onExch=function(){CS.exch=document.getElementById('exchS').value;fetchChart();};
window.setTf=function(tf,btn){
  CS.tf=tf;document.querySelectorAll('.tf-b').forEach(b=>b.classList.toggle('on',b.dataset.tf===tf));fetchChart();
};
window.fetchChart=function(){
  const sym=(document.getElementById('symIn').value||'').trim();if(sym)CS.sym=sym;
  document.getElementById('cBadge').textContent='로딩 중…';
  document.getElementById('cOv').textContent='로딩 중…';document.getElementById('cOv').className='c-ov';
  fetch(B+'/api/market/chart?market='+encodeURIComponent(CS.mkt)+'&symbol='+encodeURIComponent(CS.sym)+'&tf='+encodeURIComponent(CS.tf)+'&exch='+encodeURIComponent(CS.exch))
    .then(r=>r.json()).then(d=>{
      if(!d||d.status!=='OK'){document.getElementById('cOv').textContent='데이터 없음';return;}
      CS.pts=(d.points||[]).sort((a,b)=>a.ts-b.ts);CS.zS=0;CS.zE=1;
      document.getElementById('cmN').textContent=d.name||CS.sym;
      document.getElementById('cmS').textContent='· '+CS.sym;
      if(CS.pts.length){
        const last=CS.pts[CS.pts.length-1].price,first=CS.pts[0].price;
        const diff=last-first,rate=first?(diff/first*100):0;
        document.getElementById('cmP').textContent=last.toLocaleString('ko-KR');
        const dl=document.getElementById('cmD');
        dl.textContent=(diff>=0?'▲+':'▼')+Math.abs(diff).toFixed(0)+' ('+(diff>=0?'+':'')+rate.toFixed(2)+'%)';
        dl.className='cm-dl '+(diff>=0?'up':'dn');
        const bdg=document.getElementById('cBadge');
        bdg.textContent=CS.tf+' · '+CS.mkt;bdg.className='ph-bd '+(diff>=0?'up':'dn');
      }
      drawChart();
    }).catch(()=>{document.getElementById('cOv').textContent='차트 로드 실패';});
};

function drawChart(){
  const wrap=document.getElementById('cWrap'),canvas=document.getElementById('cv'),ov=document.getElementById('cOv');
  const pts=CS.pts;
  if(!pts||!pts.length){ov.textContent='데이터 없음';ov.className='c-ov';return;}
  ov.className='c-ov hid';
  const dpr=window.devicePixelRatio||1,W=wrap.clientWidth,H=wrap.clientHeight;
  canvas.width=W*dpr;canvas.height=H*dpr;canvas.style.width=W+'px';canvas.style.height=H+'px';
  const ctx=canvas.getContext('2d');ctx.setTransform(1,0,0,1,0,0);ctx.scale(dpr,dpr);ctx.clearRect(0,0,W,H);
  const tot=pts.length,s=Math.floor(CS.zS*tot),en=Math.max(s+2,Math.floor(CS.zE*tot));
  const vis=pts.slice(s,en);if(!vis.length)return;
  const candles=vis.map((p,i)=>{
    const prev=i===0?p:vis[i-1];
    const open=i===0?p.price:prev.price,close=p.price;
    return{time:p.time,open,high:Math.max(open,close),low:Math.min(open,close),close};
  });
  const PL=52,PR=8,PT=10,PB=22,chartW=W-PL-PR,chartH=H-PT-PB;
  const vals=candles.flatMap(c=>[c.high,c.low]);
  let yMx=Math.max(...vals),yMn=Math.min(...vals);
  const sp=yMx-yMn||1;yMx+=sp*.05;yMn-=sp*.05;
  const ys=v=>PT+chartH*(1-(v-yMn)/(yMx-yMn));
  const n=candles.length,cw=Math.max(2,Math.min(14,chartW/n-1.5)),step=chartW/Math.max(n-1,1);
  /* 그리드 */
  for(let i=0;i<=5;i++){
    const y=PT+chartH*(i/5);
    ctx.strokeStyle='rgba(255,255,255,.035)';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(PL,y);ctx.lineTo(W-PR,y);ctx.stroke();
    const v=yMx-(yMx-yMn)*(i/5);
    ctx.fillStyle='rgba(122,132,153,.65)';ctx.font='8px JetBrains Mono,monospace';ctx.textAlign='right';
    ctx.fillText(v.toLocaleString('ko-KR',{maximumFractionDigits:0}),PL-3,y+3);
  }
  const xStep=Math.max(1,Math.floor(n/6));
  for(let i=0;i<n;i+=xStep){
    const x=PL+i*step;
    ctx.strokeStyle='rgba(255,255,255,.025)';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(x,PT);ctx.lineTo(x,H-PB);ctx.stroke();
    ctx.fillStyle='rgba(122,132,153,.65)';ctx.font='8px JetBrains Mono,monospace';ctx.textAlign='center';
    const lbl=(candles[i].time||'').replace(/\d{4}-\d{2}-\d{2} /,'').substring(0,5);
    ctx.fillText(lbl,x,H-PB+12);
  }
  /* MA20 */
  if(candles.length>=20){
    ctx.beginPath();ctx.strokeStyle='rgba(168,255,62,.5)';ctx.lineWidth=1.2;
    for(let i=19;i<candles.length;i++){
      const avg=candles.slice(i-19,i+1).reduce((s,c)=>s+c.close,0)/20;
      const x=PL+i*step,y=ys(avg);
      i===19?ctx.moveTo(x,y):ctx.lineTo(x,y);
    }
    ctx.stroke();
  }
  /* 캔들 */
  candles.forEach((c,i)=>{
    const x=PL+i*step,yO=ys(c.open),yC=ys(c.close),yH=ys(c.high),yL=ys(c.low);
    const up=c.close>=c.open,fill=up?'#00d97e':'#ff4d6a',stk=up?'#00b060':'#e02040';
    ctx.strokeStyle=stk;ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(x,yH);ctx.lineTo(x,yL);ctx.stroke();
    const top=Math.min(yO,yC),bh=Math.max(Math.abs(yC-yO),1.5);
    ctx.fillStyle=fill;ctx.fillRect(x-cw/2,top,cw,bh);
    ctx.strokeStyle=stk;ctx.lineWidth=.7;ctx.strokeRect(x-cw/2,top,cw,bh);
  });
  /* 마지막 가격선 */
  const lc=candles[candles.length-1].close,yl=ys(lc);
  ctx.setLineDash([3,3]);ctx.strokeStyle='rgba(168,255,62,.35)';ctx.lineWidth=1;
  ctx.beginPath();ctx.moveTo(PL,yl);ctx.lineTo(W-PR,yl);ctx.stroke();ctx.setLineDash([]);
  ctx.fillStyle='rgba(168,255,62,.85)';ctx.font='bold 8px JetBrains Mono,monospace';ctx.textAlign='right';
  ctx.fillText(lc.toLocaleString('ko-KR',{maximumFractionDigits:0}),PL-2,yl+3);
  canvas._m={PL,PR,PT,PB,W,H,yMn,yMx,chartW,chartH,candles,step,n,ys};
}

/* 크로스헤어 */
const cWrap=document.getElementById('cWrap'),cvl=document.getElementById('cvl'),chl=document.getElementById('chl'),ctp=document.getElementById('ctp');
cWrap.addEventListener('mousemove',ev=>{
  const m=document.getElementById('cv')._m;if(!m)return;
  const r=cWrap.getBoundingClientRect(),mx=ev.clientX-r.left,my=ev.clientY-r.top;
  const{PL,PR,PT,PB,W,H,yMn,yMx,candles,step}=m;
  if(mx<PL||mx>W-PR||my<PT||my>H-PB){cvl.style.display='none';chl.style.display='none';ctp.style.display='none';return;}
  const idx=Math.round((mx-PL)/step);
  const c=candles[Math.max(0,Math.min(idx,candles.length-1))];
  cvl.style.display='block';cvl.style.left=mx+'px';
  chl.style.display='block';chl.style.top=my+'px';
  ctp.style.display='block';
  const tw=170,tx=mx+10>W-tw?mx-tw-6:mx+10,ty=my+6>H-76?my-76:my+6;
  ctp.style.left=tx+'px';ctp.style.top=ty+'px';
  const up=c.close>=c.open;
  ctp.innerHTML='<div style="font-size:8px;color:var(--t3);margin-bottom:3px">'+e(c.time||'')+'</div>'+
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:1px 8px;font-size:9px;">'+
      '<span style="color:var(--t2)">O</span><span>'+c.open.toLocaleString('ko-KR')+'</span>'+ 
      '<span style="color:var(--t2)">H</span><span style="color:var(--emerald)">'+c.high.toLocaleString('ko-KR')+'</span>'+ 
      '<span style="color:var(--t2)">L</span><span style="color:var(--red)">'+c.low.toLocaleString('ko-KR')+'</span>'+ 
      '<span style="color:var(--t2)">C</span><span style="color:'+(up?'var(--emerald)':'var(--red)')+';font-weight:600">'+c.close.toLocaleString('ko-KR')+'</span>'+ 
    '</div>';
});
cWrap.addEventListener('mouseleave',()=>{cvl.style.display='none';chl.style.display='none';ctp.style.display='none';});
window.zm=function(d){
  const rng=CS.zE-CS.zS,nr=Math.max(.05,Math.min(1,rng*(d>0?.75:1.33)));
  const c=(CS.zS+CS.zE)/2;CS.zS=Math.max(0,c-nr/2);CS.zE=Math.min(1,c+nr/2);drawChart();
};
window.zmR=function(){CS.zS=0;CS.zE=1;drawChart();};
cWrap.addEventListener('wheel',ev=>{ev.preventDefault();zm(ev.deltaY<0?1:-1);},{passive:false});
cWrap.addEventListener('mousedown',ev=>{CS.drag=true;CS.dragX=ev.clientX;CS.dragZ=[CS.zS,CS.zE];});
window.addEventListener('mouseup',()=>{CS.drag=false;});
window.addEventListener('mousemove',ev=>{
  if(!CS.drag||!CS.dragZ)return;
  const dx=(ev.clientX-CS.dragX)/cWrap.getBoundingClientRect().width;
  const rng=CS.dragZ[1]-CS.dragZ[0];
  let ns=CS.dragZ[0]-dx,ne=CS.dragZ[1]-dx;
  if(ns<0){ne-=ns;ns=0;}if(ne>1){ns-=(ne-1);ne=1;}
  CS.zS=Math.max(0,ns);CS.zE=Math.min(1,ne);drawChart();
});
window.addEventListener('resize',()=>drawChart());

/* ══ SIDEBAR 랭킹 ══ */
let sbD=[],sbM='KR',sbE='NAS',sbT='vol',sbAuto=true,sbPrev=new Set(),sbCd=POLL,sbTimer=null;

window.setSbMkt=function(m){
  sbM=m;
  ['KR','US'].forEach(x=>document.getElementById('sm'+x).className='mkt-sb'+(x===m?(m==='KR'?' on kr':' on us'):''));
  document.getElementById('sbExch').classList.toggle('show',m==='US');
  document.getElementById('sbTabs').className='sb-tabs'+(m==='US'?' us':'');
  document.getElementById('sbFt').textContent='KIS · /api/market/ranking?market='+m;
  loadRanking();
};
window.setSbExch=function(ex){
  sbE=ex;['NAS','NYS','AMS'].forEach(x=>document.getElementById('ex'+x).classList.toggle('on',x===ex));loadRanking();
};
window.setSbTab=function(t){
  sbT=t;['Vol','Chg','Hi'].forEach(x=>document.getElementById('st'+x).classList.toggle('on',x.toLowerCase()===t));
  if(sbD.length)renderRank(sbD);
};
window.toggleAuto=function(){
  sbAuto=!sbAuto;
  const b=document.getElementById('sbAb');
  b.classList.toggle('act',sbAuto);
  if(sbAuto)restartCd();else{clearInterval(sbTimer);document.getElementById('sbPgf').style.width='0%';document.getElementById('sbCdn').textContent='—';}
};
window.loadRanking=function(){
  const rb=document.getElementById('sbRb');rb.classList.add('spin');
  /* ★ 핵심: /api/market/ranking 호출, output 배열 파싱 */
  fetch(B+'/api/market/ranking?market='+sbM+'&exch='+sbE)
    .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
    .then(json=>{
      /* KIS 응답 구조: { rt_cd:"0", output:[{...},{...},...] } */
      const raw=json.data||json.output||json.items||[];
      sbD=Array.isArray(raw)?raw.slice(0,30):[];
      if(!sbD.length&&json.rt_cd&&json.rt_cd!=='0'){
        showSbErr('API 오류: '+(json.msg1||json.msg||'알 수 없음'));
      } else {
        renderRank(sbD);
        const n=new Date(),p=v=>String(v).padStart(2,'0');
        document.getElementById('sbUpd').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds())+' 갱신';
        if(sbAuto)restartCd();
      }
    })
    .catch(err=>showSbErr(err.message))
    .finally(()=>rb.classList.remove('spin'));
};

function showSbErr(msg){
  document.getElementById('sbBody').innerHTML='<div class="sb-msg er"><div class="sb-mi">✕</div><div class="sb-mt">'+e(msg)+'</div></div>';
}
function renderRank(data){
  const body=document.getElementById('sbBody');
  if(!data.length){body.innerHTML='<div class="sb-msg"><div class="sb-mi">—</div><div class="sb-mt">데이터 없음</div></div>';return;}
  const isUS=sbM==='US';

  // 탭별 정렬 (이제 필드가 통일됨)
  const sorted=[...data].sort((a,b)=>{
    if(sbT==='chg') return Math.abs(parseFloat(b.prdy_ctrt)||0) - Math.abs(parseFloat(a.prdy_ctrt)||0);
    if(sbT==='hi')  return (parseFloat(b.stck_prpr)||0) - (parseFloat(a.stck_prpr)||0);
    return (parseFloat(b.acml_vol)||0) - (parseFloat(a.acml_vol)||0);
  }).slice(0,30);

  const maxV=Math.max(...sorted.map(r=>parseFloat(r.acml_vol)||0), 1);
  const newSet=new Set(sorted.map(r=>r.symbol));

  body.innerHTML='<div class="rk-list">'+sorted.map((r,i)=>{
    const rk=i+1, rc=rk===1?'r1':rk===2?'r2':rk===3?'r3':'rN';
    const code = r.symbol || '—';
    const name = r.name  || code;
    const rawP = r.stck_prpr;
    const price = rawP && rawP!=='0'
      ? (isUS ? '$'+Number(rawP).toFixed(2) : Number(rawP).toLocaleString('ko-KR'))
      : '—';
    const diff  = parseFloat(r.prdy_ctrt||'0');
    const sign  = r.prdy_vrss_sign||'3';
    const isUp  = sign==='1'||sign==='2';
    const isDn  = sign==='4'||sign==='5';
    let cc,pfx;
    if(isUp){cc=isUS?'uu':'ku';pfx='▲';}
    else if(isDn){cc=isUS?'ud':'kd';pfx='▼';}
    else{cc='fl';pfx='—';}
    const vol=parseFloat(r.acml_vol)||0;
    const vp=Math.max((vol/maxV*100),2).toFixed(1);
    const vf=vol>=1e8?(vol/1e8).toFixed(1)+'억':vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
    const isNew=!sbPrev.has(code);
    return '<div class="rk-row'+(isNew?' fl':'')+'" style="animation-delay:'+(i*12)+'ms" onclick="onRkClick(\''+e(code)+'\',\''+e(sbM)+'\',\''+e(sbE)+'\')">'+
      '<span class="rn '+rc+'">'+rk+'</span>'+
      '<div class="ri-i">'+
        '<div class="ri-nm" title="'+e(name)+'">'+e(name)+'</div>'+
        '<div class="ri-cd">'+e(code)+'</div>'+
        '<div class="ri-bw"><div class="ri-b '+(isUS?'us':'kr')+'" style="width:'+vp+'%"></div><div class="ri-vl">'+vf+'</div></div>'+
      '</div>'+
      '<div class="rp-i">'+
        '<div class="rp-pr">'+price+'</div>'+
        '<div class="rp-ch '+cc+'">'+pfx+Math.abs(diff).toFixed(2)+'%</div>'+
      '</div>'+
    '</div>';
  }).join('')+'</div>';
  sbPrev=newSet;
}

/* 랭킹 클릭 → 차트 */
window.onRkClick = function(code, mkt, exch) {
  // KIS US 종목코드는 "DNASAAPL" 형태 → 앞 4자 제거
  const sym = code;
  
  CS.sym = sym; CS.mkt = mkt; CS.exch = exch;
  document.getElementById('symIn').value = sym;
  ['KR','US'].forEach(x =>
    document.getElementById('m'+x).className =
      'mkt-b' + (x === mkt ? (mkt === 'KR' ? ' on kr' : ' on us') : '')
  );
  document.getElementById('exchS').classList.toggle('show', mkt === 'US');
  fetchChart();
};

function restartCd(){
  sbCd=POLL;updPg();clearInterval(sbTimer);
  sbTimer=setInterval(()=>{
    sbCd--;document.getElementById('sbCdn').textContent=sbCd;updPg();
    if(sbCd<=0)loadRanking();
  },1000);
}
function updPg(){document.getElementById('sbPgf').style.width=((POLL-sbCd)/POLL*100)+'%';}
function e(s){return String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}

/* 초기 */
fetchChart();
loadRanking();
})();
</script>
</body>
</html>
