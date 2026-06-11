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
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:52px;--sidebar-w:272px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;overflow:hidden;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.06) 0%,transparent 55%),
    radial-gradient(ellipse 40% 50% at 100% 100%,rgba(0,217,126,.03) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.04) 1px,transparent 1px);background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes sir{from{opacity:0;transform:translateX(14px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.65);opacity:.25;}}
@keyframes sp{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes ri{from{opacity:0;transform:translateX(3px);}to{opacity:1;transform:none;}}
@keyframes fl{0%{background:rgba(168,255,62,.08);}100%{background:transparent;}}

/* ── TOPBAR ── */
.topbar{position:fixed;top:0;left:0;right:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.95);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent 0%,var(--lime) 40%,rgba(168,255,62,.25) 70%,transparent 100%);opacity:.35;}
.tb-logo{display:flex;align-items:center;gap:9px;padding:0 16px;height:100%;border-right:1px solid var(--rim);min-width:180px;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;display:flex;align-items:center;justify-content:center;box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;color:var(--t1);}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-pill{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:9px;color:var(--emerald);
  padding:3px 9px;border-radius:20px;background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);box-shadow:var(--emerald-glow);animation:pd 1.4s ease-in-out infinite;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
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
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── SHELL ── */
.shell{display:flex;height:100vh;padding-top:var(--topbar-h);}
.main{flex:1;min-width:0;display:flex;flex-direction:column;overflow:hidden;}

/* ── KPI ROW ── */
.kpi-row{display:grid;grid-template-columns:repeat(5,1fr);gap:6px;flex-shrink:0;
  padding:8px 10px 0;animation:fu .4s .05s ease both;}
.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:8px 12px;position:relative;overflow:hidden;cursor:default;}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:1.5px;}
.kl::before{background:var(--lime);box-shadow:var(--lime-glow);}
.ke::before{background:var(--emerald);}
.kr::before{background:var(--red);}
.kg::before{background:var(--gold);}
.kb::before{background:var(--blue);}
.k-lbl{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:4px;}
.k-val{font-family:var(--mono);font-size:18px;font-weight:500;line-height:1;letter-spacing:-0.5px;}
.kl .k-val{color:var(--lime);}
.ke .k-val{color:var(--emerald);}
.kr .k-val{color:var(--red);}
.kg .k-val{color:var(--gold);}
.kb .k-val{color:var(--blue);}
.k-sub{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:2px;}

/* ── CONTENT AREA ── */
.content-area{flex:1;min-height:0;display:grid;grid-template-columns:1fr var(--sidebar-w);gap:0;animation:fu .4s .1s ease both;}
.chart-area{display:flex;flex-direction:column;min-height:0;border-right:1px solid var(--rim);padding:8px 0 8px 10px;}

.pn{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;min-height:0;}
.ph{flex-shrink:0;height:32px;display:flex;align-items:center;justify-content:space-between;
  padding:0 10px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.ph-l{display:flex;align-items:center;gap:6px;}
.ph-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.ph-nm{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.ph-bd{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:7px;border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.ph-bd.ok{color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.ph-bd.up{color:var(--red);border-color:var(--red-b);background:var(--red-d);}
.ph-bd.dn{color:#60a5fa;border-color:rgba(96,165,250,.3);background:rgba(96,165,250,.08);}

.chart-pn{flex:1;min-height:0;display:flex;flex-direction:column;}

/* ── TOOLBAR ── */
.c-toolbar{flex-shrink:0;display:flex;align-items:center;gap:4px;flex-wrap:wrap;
  padding:5px 8px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.mkt-grp{display:flex;gap:2px;}
.mkt-b{font-family:var(--mono);font-size:7px;letter-spacing:1px;padding:3px 8px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-b.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-b.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.exch-s{height:22px;background:var(--panel);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t2);font-family:var(--mono);font-size:8px;padding:0 5px;outline:none;cursor:pointer;display:none;}
.exch-s.show{display:block;}
option{background:var(--panel-hi);}

/* 심볼 입력 + 자동완성 */
.sym-wrap{position:relative;display:flex;gap:3px;}
.sym-in{height:22px;width:84px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t1);font-family:var(--mono);font-size:10px;letter-spacing:1px;padding:0 6px;outline:none;}
.sym-in:focus{border-color:var(--lime-b);}
/* 자동완성 드롭다운 */
.sym-drop{position:absolute;top:26px;left:0;min-width:200px;
  background:var(--panel-hi);border:1px solid var(--rim-hi);border-radius:var(--r2);
  z-index:50;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,.5);display:none;}
.sym-drop.show{display:block;}
.sym-drop-item{padding:6px 10px;cursor:pointer;display:flex;align-items:center;gap:8px;
  transition:background .1s;border-bottom:1px solid var(--t4);}
.sym-drop-item:last-child{border-bottom:none;}
.sym-drop-item:hover{background:var(--hover);}
.sdi-code{font-family:var(--mono);font-size:10px;font-weight:600;color:var(--t1);}
.sdi-name{font-family:var(--mono);font-size:9px;color:var(--t3);flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.sdi-src{font-family:var(--mono);font-size:7px;padding:1px 4px;border-radius:3px;flex-shrink:0;}
.sdi-src.holding{color:var(--lime);background:var(--lime-d);border:1px solid var(--lime-b);}
.sdi-src.recent {color:var(--gold);background:var(--gold-d);border:1px solid var(--gold-b);}
.sdi-src.ranking{color:var(--t3);background:var(--t4);border:1px solid var(--rim);}

.go-b{height:22px;padding:0 9px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:7px;letter-spacing:1px;cursor:pointer;transition:all .12s;}
.go-b:hover{background:var(--lime);color:var(--void);}

/* 분봉 로딩 인디케이터 */
.tf-loading{width:12px;height:12px;border:1.5px solid var(--rim-hi);border-top-color:var(--lime);
  border-radius:50%;animation:sp .6s linear infinite;display:none;flex-shrink:0;}
.tf-loading.show{display:block;}

.tf-grp{display:flex;gap:2px;margin-left:auto;}
.tf-b{font-family:var(--mono);font-size:7px;letter-spacing:.3px;padding:3px 5px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.tf-b.on{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tf-b:hover:not(.on){color:var(--t1);}
/* 분봉 표시 */
.tf-b[data-tf="1m"],.tf-b[data-tf="5m"],.tf-b[data-tf="15m"],.tf-b[data-tf="30m"],.tf-b[data-tf="60m"]{
  border-left-color:rgba(77,159,255,.3);
}

/* 분봉 날짜 선택 */
.tf-date{height:22px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t2);font-family:var(--mono);font-size:9px;padding:0 5px;outline:none;cursor:pointer;
  display:none;}
.tf-date.show{display:block;}

/* ── 메타 ── */
.c-meta{flex-shrink:0;padding:5px 10px 4px;display:flex;align-items:center;gap:8px;border-bottom:1px solid var(--rim);}
.cm-nm{font-size:12px;font-weight:700;color:var(--t1);}
.cm-sym{font-family:var(--mono);font-size:9px;color:var(--t3);}
.cm-pr{font-family:var(--mono);font-size:16px;font-weight:600;color:var(--t1);letter-spacing:-0.5px;}
.cm-dl{font-family:var(--mono);font-size:10px;}
.cm-dl.up{color:var(--red);}.cm-dl.dn{color:#60a5fa;}
.cm-sp{flex:1;}
/* 분봉 현재가 실시간 표시 */
.cm-live{display:none;align-items:center;gap:5px;}
.cm-live.show{display:flex;}
.cm-live-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);animation:pd 1.2s ease-in-out infinite;}
.cm-live-pr{font-family:var(--mono);font-size:11px;font-weight:600;color:var(--emerald);}
.cm-live-lbl{font-family:var(--mono);font-size:8px;color:var(--t3);}
/* 세션 정보 */
.cm-session{font-family:var(--mono);font-size:8px;color:var(--t3);}
.cm-session.open{color:var(--emerald);}
.cm-session.closed{color:var(--red);}

/* ── LEGEND ── */
.c-legend{flex-shrink:0;display:flex;align-items:center;gap:9px;padding:3px 8px;
  border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.lg-item{display:flex;align-items:center;gap:3px;font-family:var(--mono);font-size:7px;color:var(--t3);}
.lg-box{width:8px;height:8px;border-radius:2px;}
.lg-sep{width:1px;height:12px;background:var(--rim);margin:0 2px;}
.zm-grp{margin-left:auto;display:flex;gap:2px;}
.zm-b{font-family:var(--mono);font-size:7px;padding:2px 6px;border-radius:3px;
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.zm-b:hover{color:var(--t1);}

/* ── CANVAS ── */
.c-wrap{flex:1;min-height:0;display:flex;flex-direction:column;background:var(--base);position:relative;cursor:crosshair;}
.c-wrap.drag{cursor:grabbing;}
.c-main{flex:1;min-height:0;position:relative;}
.c-vol{flex-shrink:0;height:50px;position:relative;border-top:1px solid rgba(255,255,255,.04);}
.c-rsi{flex-shrink:0;height:56px;position:relative;border-top:1px solid rgba(255,255,255,.04);}
canvas{display:block;width:100%;height:100%;}

.c-ov{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;
  gap:8px;font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:2px;
  background:rgba(10,12,16,.8);pointer-events:none;z-index:5;}
.c-ov.hid{display:none;}
.c-ov-spin{width:16px;height:16px;border:2px solid var(--rim-hi);border-top-color:var(--lime);
  border-radius:50%;animation:sp .6s linear infinite;}
.c-tip{position:absolute;pointer-events:none;background:rgba(20,23,32,.97);border:1px solid var(--rim-hi);
  border-radius:6px;padding:7px 10px;font-family:var(--mono);font-size:9px;color:var(--t1);white-space:nowrap;z-index:10;display:none;}
.c-vl,.c-hl{position:absolute;pointer-events:none;display:none;}
.c-vl{width:1px;top:0;bottom:0;background:rgba(168,255,62,.18);}
.c-hl{height:1px;left:0;right:0;background:rgba(168,255,62,.12);}
.sub-lbl{position:absolute;top:3px;left:56px;font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;pointer-events:none;z-index:2;}

/* ── SIDEBAR ── */
.sidebar{width:var(--sidebar-w);height:100%;background:var(--base);
  display:flex;flex-direction:column;overflow:hidden;animation:sir .5s .15s ease both;}
.sb-hd{flex-shrink:0;background:var(--panel-hi);border-bottom:1px solid var(--rim);}
.sb-r1{display:flex;align-items:center;justify-content:space-between;padding:8px 10px 3px;}
.sb-title{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:8px;font-weight:600;
  color:var(--lime);letter-spacing:2px;text-transform:uppercase;}
.sb-tdot{width:5px;height:5px;border-radius:50%;background:var(--lime);box-shadow:var(--lime-glow);animation:pd 2s ease-in-out infinite;}
.sb-btns{display:flex;gap:2px;}
.sb-btn{width:22px;height:22px;border-radius:var(--r);border:1px solid var(--rim-hi);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .12s;display:flex;align-items:center;justify-content:center;padding:0;}
.sb-btn:hover,.sb-btn.act{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.sb-btn.spin svg{animation:sp .5s linear infinite;}
.sb-mkt{display:flex;gap:3px;padding:3px 10px 7px;}
.mkt-sb{flex:1;height:20px;font-family:var(--mono);font-size:7px;letter-spacing:1px;text-transform:uppercase;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-sb.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-sb.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-r2{display:flex;align-items:center;justify-content:space-between;padding:0 10px 6px;}
.sb-upd{font-family:var(--mono);font-size:7px;color:var(--t3);}
.sb-cd{display:flex;align-items:center;gap:3px;}
.sb-cdl{font-family:var(--mono);font-size:7px;color:var(--t3);}
.sb-cdn{font-family:var(--mono);font-size:8px;font-weight:600;color:var(--lime);min-width:16px;text-align:right;}
.sb-pg{height:2px;background:var(--t4);flex-shrink:0;}
.sb-pgf{height:100%;width:0%;background:linear-gradient(90deg,var(--lime),var(--emerald));
  box-shadow:0 0 4px var(--lime);transition:width 1s linear;}
.sb-tabs{flex-shrink:0;display:flex;border-bottom:1px solid var(--rim);background:var(--panel);}
.sb-tab{flex:1;padding:5px 0;text-align:center;font-family:var(--mono);font-size:7px;letter-spacing:1.5px;
  text-transform:uppercase;color:var(--t3);cursor:pointer;user-select:none;border-bottom:2px solid transparent;transition:all .12s;}
.sb-tab:hover{color:var(--t2);}
.sb-tab.on{color:var(--lime);border-bottom-color:var(--lime);}
.sb-tabs.us .sb-tab.on{color:var(--blue);border-bottom-color:var(--blue);}
.sb-exch{flex-shrink:0;display:none;gap:3px;padding:4px 8px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.sb-exch.show{display:flex;}
.ex-b{flex:1;height:18px;font-family:var(--mono);font-size:7px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.ex-b.on{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-body{flex:1;overflow-y:auto;scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.sb-body::-webkit-scrollbar{width:2px;}
.sb-body::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
.sb-msg{padding:30px 14px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:7px;}
.sb-mi{font-size:18px;}.sb-mt{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1.5px;}
.sb-msg.ld .sb-mi{color:var(--lime);animation:pd 1s ease-in-out infinite;}
.sb-msg.er .sb-mi,.sb-msg.er .sb-mt{color:var(--red);}

/* ── 랭킹 리스트 ── */
.rk-list{padding:2px 0;}
.rk-row{display:grid;grid-template-columns:20px 1fr auto;align-items:center;gap:5px;
  padding:5px 8px;border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;animation:ri .2s ease both;}
.rk-row:last-child{border-bottom:none;}
.rk-row:hover{background:var(--hover);}
.rk-row.fl{animation:fl .5s ease both;}
.rn{font-family:var(--mono);font-size:9px;font-weight:700;text-align:center;flex-shrink:0;}
.rn.r1{color:var(--gold);font-size:11px;}.rn.r2{color:#b0b8c8;}.rn.r3{color:#cd8b5a;}.rn.rN{color:var(--t3);font-size:8px;}
.ri-i{min-width:0;}
.ri-nm{font-size:10px;font-weight:600;color:var(--t1);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;margin-bottom:1px;}
.ri-cd{font-family:var(--mono);font-size:7px;color:var(--t3);}
.ri-bw{margin-top:2px;}
.ri-b{height:2px;border-radius:1px;transition:width .5s ease;}
.ri-b.kr{background:linear-gradient(90deg,var(--lime),rgba(168,255,62,.05));}
.ri-b.us{background:linear-gradient(90deg,var(--blue),rgba(77,159,255,.05));}
.ri-vl{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:1px;}
.rp-i{text-align:right;flex-shrink:0;}
.rp-pr{font-family:var(--mono);font-size:9px;font-weight:500;color:var(--t1);}
.rp-ch{font-family:var(--mono);font-size:8px;margin-top:1px;display:flex;align-items:center;justify-content:flex-end;}
.rp-ch.ku{color:var(--red);}.rp-ch.kd{color:#60a5fa;}
.rp-ch.uu{color:var(--emerald);}.rp-ch.ud{color:var(--red);}.rp-ch.fl{color:var(--t3);}

/* ── 보유종목 / 최근 본 종목 ── */
.sym-section{padding:0;}
.sym-sec-hd{display:flex;align-items:center;justify-content:space-between;
  padding:7px 10px 5px;border-bottom:1px solid var(--t4);}
.sym-sec-title{font-family:var(--mono);font-size:8px;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.sym-sec-cnt{font-family:var(--mono);font-size:8px;padding:1px 6px;border-radius:4px;
  color:var(--t3);background:var(--t4);border:1px solid var(--rim);}
.sym-row{display:flex;align-items:center;gap:6px;padding:6px 10px;
  border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;}
.sym-row:last-child{border-bottom:none;}
.sym-row:hover{background:var(--hover);}
.sym-row-code{font-family:var(--mono);font-size:10px;font-weight:700;color:var(--t1);min-width:52px;}
.sym-row-name{font-family:var(--mono);font-size:9px;color:var(--t2);flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.sym-row-info{text-align:right;flex-shrink:0;}
.sym-row-price{font-family:var(--mono);font-size:9px;color:var(--t1);}
.sym-row-pnl{font-family:var(--mono);font-size:8px;margin-top:1px;}
.sym-row-pnl.pos{color:var(--emerald);}.sym-row-pnl.neg{color:var(--red);}
/* 보유 뱃지 */
.hold-badge{font-family:var(--mono);font-size:7px;padding:1px 5px;border-radius:3px;
  color:var(--lime);background:var(--lime-d);border:1px solid var(--lime-b);flex-shrink:0;}
/* 최근 뱃지 */
.recent-badge{font-family:var(--mono);font-size:7px;padding:1px 5px;border-radius:3px;
  color:var(--gold);background:var(--gold-d);border:1px solid var(--gold-b);flex-shrink:0;}
.sym-empty{font-family:var(--mono);font-size:9px;color:var(--t3);text-align:center;padding:16px;letter-spacing:.5px;}

.sb-ft{flex-shrink:0;padding:4px 8px;border-top:1px solid var(--rim);background:var(--panel-hi);
  font-family:var(--mono);font-size:6px;color:var(--t3);text-align:center;letter-spacing:.5px;}

/* ── DATA BAR ── */
.data-bar{flex-shrink:0;height:120px;border-top:1px solid var(--rim);display:flex;gap:0;padding:0 10px 8px;}
.db-sec{flex:1;min-width:0;display:flex;flex-direction:column;overflow:hidden;}
.db-sec+.db-sec{border-left:1px solid var(--rim);margin-left:8px;padding-left:8px;}
.db-hd{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;
  padding:5px 0 4px;flex-shrink:0;border-bottom:1px solid var(--rim);margin-bottom:2px;}
.db-sc{flex:1;overflow-y:auto;scrollbar-width:none;}
.db-sc::-webkit-scrollbar{display:none;}
.db-row{display:grid;padding:2px 0;font-size:10px;border-bottom:1px solid var(--t4);}
.db-row:last-child{border-bottom:none;}
.ord-row{grid-template-columns:28px 58px 38px 44px 70px 1fr;}
.prc-row{grid-template-columns:60px 80px 1fr;}
.db-row span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.db-th{display:grid;font-family:var(--mono);font-size:7px;color:var(--t2);letter-spacing:.5px;margin-bottom:1px;}
.ord-th{grid-template-columns:28px 58px 38px 44px 70px 1fr;}
.prc-th{grid-template-columns:60px 80px 1fr;}
.c-id{font-family:var(--mono);font-size:9px;color:var(--t3);}
.c-sym{font-family:var(--mono);font-size:10px;font-weight:600;color:var(--t1);}
.s-buy{font-family:var(--mono);font-size:8px;font-weight:600;padding:0 4px;border-radius:2px;
  color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.s-sell{font-family:var(--mono);font-size:8px;font-weight:600;padding:0 4px;border-radius:2px;
  color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.c-pr{font-family:var(--mono);font-size:10px;color:var(--gold);font-weight:500;}
.c-lpr{font-family:var(--mono);font-size:10px;color:var(--lime);font-weight:500;}
.c-mu{font-family:var(--mono);font-size:9px;color:var(--t3);}
.db-empty{font-family:var(--mono);font-size:9px;color:var(--t3);text-align:center;padding:12px 0;letter-spacing:1px;}
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
  <div class="tb-pill"><div class="tb-dot"></div><span id="hdSt">LOADING</span></div>
  <div class="tb-nav">
    <a class="tb-a cur"  href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/backtest">Backtest</a>
  </div>
  <div class="tb-login">
    <form id="lf" autocomplete="off">
      <input type="text"     name="accountNo"       placeholder="12345678-01" autocomplete="off" maxlength="20"/>
      <input type="password" name="accountPassword" placeholder="Password"    autocomplete="new-password" maxlength="50"/>
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

    <!-- KPI -->
    <div class="kpi-row">
      <div class="kpi kl"><div class="k-lbl">Watchlist</div><div class="k-val" id="kW">—</div><div class="k-sub">감시 종목</div></div>
      <div class="kpi ke"><div class="k-lbl">Positions</div><div class="k-val" id="kP">—</div><div class="k-sub">포지션</div></div>
      <div class="kpi kg"><div class="k-lbl">Last Symbol</div><div class="k-val" id="kS" style="font-size:13px;padding-top:2px">—</div><div class="k-sub" id="kSS">—</div></div>
      <div class="kpi kr"><div class="k-lbl">Status</div><div class="k-val" id="kSt" style="font-size:13px;padding-top:2px">—</div><div class="k-sub">엔진 상태</div></div>
      <div class="kpi kb"><div class="k-lbl">Orders</div><div class="k-val" id="kO">—</div><div class="k-sub">최근 주문</div></div>
    </div>

    <div class="content-area" style="flex:1;min-height:0;">

      <!-- 차트 영역 -->
      <div class="chart-area">
        <div class="chart-pn pn">
          <div class="ph">
            <div class="ph-l">
              <div class="ph-dot" style="background:var(--lime);box-shadow:var(--lime-glow)"></div>
              <div class="ph-nm">Price Chart</div>
            </div>
            <span class="ph-bd" id="cBadge">1d · KR</span>
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

            <!-- 심볼 입력 + 자동완성 -->
            <div class="sym-wrap">
              <input class="sym-in" id="symIn" value="005930" autocomplete="off" spellcheck="false" maxlength="12" placeholder="종목코드"/>
              <div class="sym-drop" id="symDrop"></div>
            </div>
            <button class="go-b" onclick="fetchChart()">조회</button>

            <!-- 분봉 로딩 스피너 -->
            <div class="tf-loading" id="tfLoading"></div>

            <!-- 분봉 날짜 선택 -->
            <input type="date" class="tf-date" id="tfDate" title="분봉 날짜 선택"/>

            <div class="tf-grp">
              <button class="tf-b"    data-tf="1m"  onclick="setTf('1m',this)" title="1분봉">1m</button>
              <button class="tf-b"    data-tf="5m"  onclick="setTf('5m',this)" title="5분봉">5m</button>
              <button class="tf-b"    data-tf="15m" onclick="setTf('15m',this)" title="15분봉">15m</button>
              <button class="tf-b"    data-tf="30m" onclick="setTf('30m',this)" title="30분봉">30m</button>
              <button class="tf-b"    data-tf="60m" onclick="setTf('60m',this)" title="60분봉">60m</button>
              <button class="tf-b on" data-tf="1d"  onclick="setTf('1d',this)"  title="일봉">1D</button>
              <button class="tf-b"    data-tf="1w"  onclick="setTf('1w',this)"  title="주봉">1W</button>
            </div>
          </div>

          <div class="c-meta">
            <span class="cm-nm" id="cmN">—</span>
            <span class="cm-sym" id="cmS"></span>
            <span class="cm-pr" id="cmP">—</span>
            <span class="cm-dl" id="cmD">—</span>
            <span class="cm-sp"></span>
            <!-- 분봉 실시간 현재가 -->
            <div class="cm-live" id="cmLive">
              <div class="cm-live-dot"></div>
              <span class="cm-live-lbl">현재가</span>
              <span class="cm-live-pr" id="cmLivePr">—</span>
            </div>
            <span class="cm-session" id="cmSession"></span>
          </div>

          <div class="c-legend">
            <div class="lg-item"><div class="lg-box" style="background:var(--emerald)"></div>상승</div>
            <div class="lg-item"><div class="lg-box" style="background:var(--red)"></div>하락</div>
            <div class="lg-sep"></div>
            <div class="lg-item"><div class="lg-box" style="background:rgba(77,159,255,.35);border:1px solid var(--blue)"></div>BB</div>
            <div class="lg-item"><div class="lg-box" style="background:rgba(168,255,62,.4);border:1px solid var(--lime)"></div>MA20</div>
            <div class="lg-item" style="color:var(--gold)">RSI14</div>
            <div class="lg-sep"></div>
            <!-- 분봉 전용: 분봉 카운트 -->
            <div class="lg-item" id="lgMinInfo" style="display:none;color:var(--t3);"></div>
            <div class="zm-grp">
              <button class="zm-b" onclick="zm(-1)">− Zoom</button>
              <button class="zm-b" onclick="zm(1)">+ Zoom</button>
              <button class="zm-b" onclick="zmR()">Reset</button>
            </div>
          </div>

          <div class="c-wrap" id="cWrap">
            <div class="c-main" id="mainWrap">
              <canvas id="cv"></canvas>
              <div class="c-vl" id="cvl"></div>
              <div class="c-hl" id="chl"></div>
              <div class="c-tip" id="ctp"></div>
              <div class="c-ov" id="cOv">
                <div class="c-ov-spin" id="cOvSpin"></div>
                <span id="cOvTxt">로딩 중…</span>
              </div>
            </div>
            <div class="c-vol" id="volWrap">
              <canvas id="cvVol"></canvas>
              <div class="sub-lbl">VOL</div>
            </div>
            <div class="c-rsi" id="rsiWrap">
              <canvas id="cvRsi"></canvas>
              <div class="sub-lbl">RSI·14</div>
            </div>
          </div>
        </div>

        <!-- 하단 데이터 바 -->
        <div class="data-bar">
          <div class="db-sec">
            <div class="db-hd">주문 이력</div>
            <div class="db-th ord-th"><span>ID</span><span>Symbol</span><span>Side</span><span>Qty</span><span>Price</span><span>Time</span></div>
            <div class="db-sc" id="ordList"></div>
          </div>
          <div class="db-sec">
            <div class="db-hd">가격 로그</div>
            <div class="db-th prc-th"><span>Symbol</span><span>Price</span><span>Time</span></div>
            <div class="db-sc" id="prcList"></div>
          </div>
          <div class="db-sec" style="max-width:240px;flex:0 0 240px;">
            <div class="db-hd">빠른 이동</div>
            <div style="display:flex;flex-direction:column;gap:4px;padding-top:4px;">
              <a href="${pageContext.request.contextPath}/control/kr"     style="flex:1;text-align:center;padding:5px 8px;background:var(--emerald-d);border:1px solid var(--emerald-b);color:var(--emerald);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--emerald)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--emerald-d)';this.style.color='var(--emerald)'">⚡ Control KR</a>
              <a href="${pageContext.request.contextPath}/control/us"     style="flex:1;text-align:center;padding:5px 8px;background:var(--lime-d);border:1px solid var(--lime-b);color:var(--lime);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--lime)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--lime-d)';this.style.color='var(--lime)'">⚡ Control US</a>
              <a href="${pageContext.request.contextPath}/history/orders"  style="flex:1;text-align:center;padding:5px 8px;background:var(--gold-d);border:1px solid var(--gold-b);color:var(--gold);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--gold)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--gold-d)';this.style.color='var(--gold)'">📋 Orders</a>
              <a href="${pageContext.request.contextPath}/balances"        style="flex:1;text-align:center;padding:5px 8px;background:var(--panel-hi);border:1px solid var(--rim);color:var(--t2);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--hover)';this.style.color='var(--t1)'" onmouseout="this.style.background='var(--panel-hi)';this.style.color='var(--t2)'">💰 Balances</a>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT SIDEBAR -->
      <aside class="sidebar">
        <div class="sb-hd">
          <div class="sb-r1">
            <div class="sb-title"><span class="sb-tdot"></span>TOP 30</div>
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
            <div class="sb-cd"><span class="sb-cdl">갱신</span><span class="sb-cdn" id="sbCdn">30</span><span class="sb-cdl">s</span></div>
          </div>
        </div>
        <div class="sb-pg"><div class="sb-pgf" id="sbPgf"></div></div>

        <!-- 탭: 랭킹 / 보유종목 / 최근 본 -->
        <div class="sb-tabs" id="sbTabs">
          <div class="sb-tab on" id="stVol" onclick="setSbTab('vol')">거래량</div>
          <div class="sb-tab"    id="stChg" onclick="setSbTab('chg')">등락률</div>
          <div class="sb-tab"    id="stHold" onclick="setSbTab('hold')">보유</div>
          <div class="sb-tab"    id="stRecent" onclick="setSbTab('recent')">최근</div>
        </div>
        <div class="sb-exch" id="sbExch">
          <button class="ex-b on" id="exNAS" onclick="setSbExch('NAS')">NASDAQ</button>
          <button class="ex-b"    id="exNYS" onclick="setSbExch('NYS')">NYSE</button>
          <button class="ex-b"    id="exAMS" onclick="setSbExch('AMS')">AMEX</button>
        </div>
        <div class="sb-body" id="sbBody">
          <div class="sb-msg ld"><div class="sb-mi">◈</div><div class="sb-mt">로딩 중…</div></div>
        </div>
        <div class="sb-ft" id="sbFt">KIS · /api/market/ranking</div>
      </aside>

    </div>
  </div>
</div>

<script>
'use strict';
const B    = '${pageContext.request.contextPath}';
const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const POLL = 30;

function esc(s){
  return String(s==null?'':s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#x27;');
}
async function safeFetch(url,opts){
  const res=await fetch(url,opts);
  if(!res.ok) throw new Error('HTTP '+res.status);
  const ct=res.headers.get('content-type')||'';
  if(!ct.includes('application/json')) throw new Error('Unexpected content-type');
  return res.json();
}
function p2(v){return String(v).padStart(2,'0');}

/* ── 시계 ── */
function tick(){
  const n=new Date();
  document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000); tick();

/* ── 오버레이 상태 ── */
function showOv(msg,spinning){
  const ov=document.getElementById('cOv');
  const sp=document.getElementById('cOvSpin');
  const tx=document.getElementById('cOvTxt');
  ov.className='c-ov';
  sp.style.display=spinning?'block':'none';
  tx.textContent=msg;
}
function hideOv(){ document.getElementById('cOv').className='c-ov hid'; }

/* ── Login ── */
(function(){
  const f=document.getElementById('lf'),sb=document.getElementById('lst');
  const as=document.getElementById('lacc'),lb=document.getElementById('lob'),eb=document.getElementById('lerr');
  const showIn=m=>{f.style.display='none';sb.style.display='inline-flex';eb.style.display='none';as.textContent=m||'****';};
  const showOut=()=>{sb.style.display='none';f.style.display='';eb.style.display='none';};
  const showErr=m=>{eb.textContent=m||'';eb.style.display=m?'inline-flex':'none';};
  safeFetch(B+'/api/auth/status').then(d=>d&&d.loggedIn?showIn(d.accountMasked):showOut()).catch(()=>showOut());
  f.addEventListener('submit',e=>{
    e.preventDefault();
    const no=(f.accountNo.value||'').trim(),pw=(f.accountPassword.value||'').trim();
    if(!no||!pw)return;
    if(!/^[\d\-]+$/.test(no)){showErr('계정번호 형식 오류');return;}
    fetch(B+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({accountNo:no,accountPassword:pw}).toString()})
      .then(r=>r.json()).then(d=>d.status==='OK'?showIn(d.accountMasked):showErr(d.message||'Login failed'))
      .catch(()=>showErr('서버 오류'));
  });
  lb.addEventListener('click',()=>fetch(B+'/api/auth/logout',{method:'POST'}).then(()=>showOut()));
})();

/* ── 대시보드 ── */
function loadDash(){
  safeFetch(B+'/api/dashboard?limit=10').then(d=>{
    if(!d) return;
    const st=d.status||'STOPPED';
    document.getElementById('hdSt').textContent=esc(st);
    document.getElementById('kW').textContent=parseInt(d.watchlistCount)||0;
    document.getElementById('kP').textContent=parseInt(d.positionCount)||0;
    document.getElementById('kSt').textContent=esc(st);
    const ords=Array.isArray(d.recentOrders)?d.recentOrders:[];
    document.getElementById('kO').textContent=ords.length;
    if(ords.length){
      const l=ords[0];
      document.getElementById('kS').textContent=esc(l.symbol||'—');
      document.getElementById('kSS').textContent=esc(l.side||'—');
    }
    const ol=document.getElementById('ordList');
    ol.innerHTML=ords.length?ords.map(r=>
      '<div class="db-row ord-row">'
      +'<span class="c-id">#'+esc(r.id)+'</span>'
      +'<span class="c-sym">'+esc(r.symbol)+'</span>'
      +'<span><span class="'+(r.side==='BUY'?'s-buy':'s-sell')+'">'+esc(r.side)+'</span></span>'
      +'<span>'+esc(r.quantity)+'</span>'
      +'<span class="c-pr">'+Number(r.price||0).toLocaleString('ko-KR')+'</span>'
      +'<span class="c-mu">'+esc((r.createdAt||'').substring(11,16))+'</span>'
      +'</div>').join(''):'<div class="db-empty">주문 없음</div>';
    const prcs=Array.isArray(d.recentPrices)?d.recentPrices:[];
    const pl=document.getElementById('prcList');
    pl.innerHTML=prcs.length?prcs.map(r=>
      '<div class="db-row prc-row">'
      +'<span class="c-sym">'+esc(r.symbol)+'</span>'
      +'<span class="c-lpr">'+esc(r.price)+'</span>'
      +'<span class="c-mu">'+esc((r.createdAt||'').substring(11,19))+'</span>'
      +'</div>').join(''):'<div class="db-empty">데이터 없음</div>';
  }).catch(()=>{});
}
loadDash(); setInterval(loadDash,15000);

/* ══════════════════════════════════════════
   최근 본 종목 (메모리 기반, 최대 10개)
══════════════════════════════════════════ */
const recentSyms=[];  /* [{sym,mkt,name,price}] */
function addRecent(sym,mkt,name,price){
  const idx=recentSyms.findIndex(r=>r.sym===sym&&r.mkt===mkt);
  if(idx>=0) recentSyms.splice(idx,1);
  recentSyms.unshift({sym,mkt,name:name||sym,price:price||0});
  if(recentSyms.length>10) recentSyms.pop();
}

/* ══════════════════════════════════════════
   보유 종목 (잔고 API에서 가져옴)
══════════════════════════════════════════ */
let holdings=[];  /* [{sym,name,qty,avgP,curP,pnl,pnlPct}] */
function loadHoldings(){
  safeFetch(B+'/api/account/balance/kr').then(d=>{
    if(!d||d.status!=='OK') return;
    const items=Array.isArray(d.output1)?d.output1:[];
    holdings=items.filter(r=>(parseFloat(r.hldg_qty||0)>0)).map(r=>({
      sym  : r.pdno||'',
      name : r.prdt_name||r.pdno||'—',
      qty  : parseFloat(r.hldg_qty||0),
      avgP : parseFloat(r.pchs_avg_pric||0),
      curP : parseFloat(r.prpr||r.stck_prpr||0),
      pnl  : parseFloat(r.evlu_pfls_amt||0),
      pnlPct: parseFloat(r.evlu_pfls_rt||0)
    }));
    /* 보유 탭이 열려 있으면 갱신 */
    if(sbT==='hold') renderSbHold();
  }).catch(()=>{});
}
loadHoldings();
setInterval(loadHoldings,60000);

/* ══════════════════════════════════════════
   자동완성 드롭다운
══════════════════════════════════════════ */
let dropDebounce=null;
let dropData=[];

function buildDropData(){
  const res=[];
  /* 1) 보유 종목 우선 */
  holdings.forEach(h=>{
    res.push({sym:h.sym,name:h.name,src:'holding',mkt:'KR'});
  });
  /* 2) 최근 본 종목 */
  recentSyms.forEach(r=>{
    if(!res.find(x=>x.sym===r.sym&&x.mkt===r.mkt))
      res.push({sym:r.sym,name:r.name,src:'recent',mkt:r.mkt});
  });
  return res;
}

function showDrop(items){
  const drop=document.getElementById('symDrop');
  if(!items.length){drop.classList.remove('show');return;}
  drop.innerHTML=items.slice(0,8).map(it=>{
    const srcCls=it.src==='holding'?'holding':it.src==='recent'?'recent':'ranking';
    const srcTxt=it.src==='holding'?'보유':it.src==='recent'?'최근':'';
    return '<div class="sym-drop-item" data-sym="'+esc(it.sym)+'" data-mkt="'+esc(it.mkt)+'">'
      +'<span class="sdi-code">'+esc(it.sym)+'</span>'
      +'<span class="sdi-name">'+esc(it.name)+'</span>'
      +(srcTxt?'<span class="sdi-src '+srcCls+'">'+srcTxt+'</span>':'')
      +'</div>';
  }).join('');
  drop.classList.add('show');
}

document.getElementById('symIn').addEventListener('input',function(){
  const q=this.value.trim().toUpperCase();
  clearTimeout(dropDebounce);
  if(!q){
    /* 입력 없으면 보유+최근 보여주기 */
    showDrop(buildDropData());
    return;
  }
  /* 즉시 로컬 매칭 */
  const local=buildDropData().filter(it=>it.sym.includes(q)||it.name.toLowerCase().includes(q.toLowerCase()));
  if(local.length) showDrop(local);

  /* 서버 symbol-suggest (300ms 디바운스) */
  dropDebounce=setTimeout(()=>{
    safeFetch(B+'/api/market/symbol-suggest?q='+encodeURIComponent(q)+'&market='+encodeURIComponent(CS.mkt)+'&limit=8')
      .then(d=>{
        if(!Array.isArray(d.data)) return;
        const remote=d.data.map(it=>({sym:it.symbol,name:it.name||it.symbol,src:'ranking',mkt:it.market||CS.mkt}));
        /* 로컬 먼저, 원격은 중복 제거 후 합침 */
        const merged=[...local];
        remote.forEach(r=>{ if(!merged.find(x=>x.sym===r.sym)) merged.push(r); });
        showDrop(merged);
      }).catch(()=>{});
  },300);
});

document.getElementById('symIn').addEventListener('focus',function(){
  if(!this.value.trim()) showDrop(buildDropData());
});
document.addEventListener('click',e=>{
  if(!e.target.closest('.sym-wrap')) document.getElementById('symDrop').classList.remove('show');
});

document.getElementById('symDrop').addEventListener('click',e=>{
  const item=e.target.closest('.sym-drop-item');
  if(!item) return;
  const sym=item.dataset.sym;
  const mkt=item.dataset.mkt;
  document.getElementById('symIn').value=sym;
  document.getElementById('symDrop').classList.remove('show');
  if(mkt&&mkt!==CS.mkt) setMkt(mkt);
  else fetchChart();
});

/* ══════════════════════════════════════════
   차트 상태
══════════════════════════════════════════ */
const CS={mkt:'KR',exch:'NAS',sym:'005930',tf:'1d',raw:[],zS:0,zE:1,drag:false,dragX:0,dragZ:null};
const MINUTE_TFS=new Set(['1m','3m','5m','10m','15m','30m','60m']);
function isMinuteTf(tf){return MINUTE_TFS.has(tf);}
function sanitizeSymbol(raw){return (raw||'').trim().replace(/[^A-Za-z0-9.\-_]/g,'').substring(0,12).toUpperCase();}

/* 분봉 날짜 선택기 */
function updateDatePicker(){
  const dp=document.getElementById('tfDate');
  if(isMinuteTf(CS.tf)){
    const n=new Date();
    dp.max=n.toISOString().slice(0,10);
    /* 기본값: 오늘 */
    if(!dp.value) dp.value=n.toISOString().slice(0,10);
    dp.classList.add('show');
  } else {
    dp.classList.remove('show');
  }
}

/* 장 시간 확인 (KRX 09:00~15:30 KST) */
function getSessionInfo(){
  const now=new Date();
  const kst=new Date(now.toLocaleString('en-US',{timeZone:'Asia/Seoul'}));
  const h=kst.getHours(), m=kst.getMinutes();
  const dow=kst.getDay();
  if(dow===0||dow===6) return {open:false,label:'주말 휴장'};
  const total=h*60+m;
  if(total>=9*60 && total<15*60+30) return {open:true,label:'장 중 (KRX)'};
  if(total>=8*60 && total<9*60) return {open:false,label:'장 전 (8:00~9:00)'};
  if(total>=15*60+30) return {open:false,label:'장 후 마감'};
  return {open:false,label:'장 전'};
}

function resetViewRange(){
  const n=CS.raw.length;
  if(!n){CS.zS=0;CS.zE=1;return;}
  let bars=n;
  if(isMinuteTf(CS.tf))   bars=Math.min(120,n);
  else if(CS.tf==='1d')   bars=Math.min(240,n);
  else if(CS.tf==='1w')   bars=Math.min(104,n);
  if(n>bars){CS.zE=1;CS.zS=Math.max(0,1-bars/n);}
  else{CS.zS=0;CS.zE=1;}
}

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
  CS.tf=tf;
  document.querySelectorAll('.tf-b').forEach(b=>b.classList.toggle('on',b.dataset.tf===tf));
  updateDatePicker();
  fetchChart();
};

/* 날짜 변경 시 재조회 */
document.getElementById('tfDate').addEventListener('change',()=>fetchChart());

window.fetchChart=function(){
  const symEl=document.getElementById('symIn');
  if(symEl){const v=sanitizeSymbol(symEl.value);if(v) CS.sym=v;symEl.value=CS.sym;}

  document.getElementById('cBadge').textContent='로딩 중…';
  showOv('데이터 로딩 중…',true);
  document.getElementById('tfLoading').classList.add('show');

  /* 분봉 실시간 표시 토글 */
  const isMin=isMinuteTf(CS.tf);
  document.getElementById('cmLive').classList.toggle('show',isMin);
  document.getElementById('lgMinInfo').style.display=isMin?'flex':'none';

  /* 세션 정보 표시 (KR 분봉에만) */
  const sesEl=document.getElementById('cmSession');
  if(isMin&&CS.mkt==='KR'){
    const si=getSessionInfo();
    sesEl.textContent=si.label;
    sesEl.className='cm-session '+(si.open?'open':'closed');
  } else {
    sesEl.textContent='';
  }

  /* 날짜 파라미터 (분봉용) */
  let extra='';
  if(isMin){
    const dp=document.getElementById('tfDate');
    if(dp.value) extra='&date='+encodeURIComponent(dp.value.replace(/-/g,''));
  }

  const url=B+'/api/market/chart'
    +'?market='+encodeURIComponent(CS.mkt)
    +'&symbol='+encodeURIComponent(CS.sym)
    +'&tf='+encodeURIComponent(CS.tf)
    +'&exch='+encodeURIComponent(CS.exch)
    +extra;

  safeFetch(url).then(d=>{
    document.getElementById('tfLoading').classList.remove('show');
    if(!d||d.status!=='OK'){showOv('데이터 없음',false);return;}

    const pts=(d.points||[]).sort((a,b)=>(a.ts||0)-(b.ts||0));
    CS.raw=pts.map(p=>({
      time :p.time||'',ts:p.ts||0,
      open :p.open !=null?Number(p.open) :Number(p.price||0),
      high :p.high !=null?Number(p.high) :Number(p.price||0),
      low  :p.low  !=null?Number(p.low)  :Number(p.price||0),
      close:p.close!=null?Number(p.close):Number(p.price||0),
      volume:Number(p.volume||p.vol||0)
    })).filter(p=>p.close>0);

    /* 분봉 날짜 필터 */
    if(isMin&&CS.raw.length){
      const byDate={};
      CS.raw.forEach(p=>{const d=(p.time||'').slice(0,10);if(d)byDate[d]=(byDate[d]||0)+1;});
      const ds=Object.keys(byDate);
      if(ds.length>1){const keep=ds.sort((a,b)=>byDate[b]-byDate[a])[0];CS.raw=CS.raw.filter(p=>(p.time||'').startsWith(keep));}
    }

    if(CS.raw.length===0){showOv('데이터 없음',false);return;}

    /* 분봉 카운트 표시 */
    if(isMin){
      document.getElementById('lgMinInfo').textContent=CS.raw.length+'개 봉';
    }

    resetViewRange();

    const name=d.name||CS.sym;
    document.getElementById('cmN').textContent=esc(name);
    document.getElementById('cmS').textContent='· '+esc(CS.sym);

    const last=CS.raw[CS.raw.length-1].close;
    const first=CS.raw[0].open;
    const diff=last-first, rate=first?diff/first*100:0;

    document.getElementById('cmP').textContent=last.toLocaleString('ko-KR');
    /* 분봉 실시간 현재가 */
    if(isMin) document.getElementById('cmLivePr').textContent=last.toLocaleString('ko-KR');

    const dl=document.getElementById('cmD');
    dl.textContent=(diff>=0?'▲+':'▼')+Math.abs(diff).toFixed(0)+' ('+(diff>=0?'+':'')+rate.toFixed(2)+'%)';
    dl.className='cm-dl '+(diff>=0?'up':'dn');
    const bdg=document.getElementById('cBadge');
    bdg.textContent=esc(CS.tf)+' · '+esc(CS.mkt);
    bdg.className='ph-bd '+(diff>=0?'up':'dn');

    /* 최근 본 종목에 추가 */
    addRecent(CS.sym,CS.mkt,name,last);
    /* 보유 탭 갱신 (혹시 열려있으면) */
    if(sbT==='recent') renderSbRecent();

    hideOv();
    drawAll();
  }).catch(()=>{
    document.getElementById('tfLoading').classList.remove('show');
    showOv('차트 로드 실패',false);
  });
};

/* ── 지표 계산 ── */
function calcMA(closes,n){return closes.map((_,i)=>i<n-1?null:closes.slice(i-n+1,i+1).reduce((s,v)=>s+v,0)/n);}
function calcBB(closes,n=20,mult=2){
  const mid=calcMA(closes,n);
  return mid.map((m,i)=>{
    if(m===null) return{mid:null,upper:null,lower:null};
    const sl=closes.slice(i-n+1,i+1),sd=Math.sqrt(sl.reduce((s,v)=>s+(v-m)**2,0)/n);
    return{mid:m,upper:m+mult*sd,lower:m-mult*sd};
  });
}
function calcRSI(closes,n=14){
  const r=new Array(closes.length).fill(null);
  if(closes.length<=n) return r;
  let ag=0,al=0;
  for(let i=1;i<=n;i++){const d=closes[i]-closes[i-1];d>0?ag+=d:al-=d;}
  ag/=n;al/=n;
  r[n]=al===0?100:100-100/(1+ag/al);
  for(let i=n+1;i<closes.length;i++){
    const d=closes[i]-closes[i-1],g=d>0?d:0,l=d<0?-d:0;
    ag=(ag*(n-1)+g)/n;al=(al*(n-1)+l)/n;
    r[i]=al===0?100:100-100/(1+ag/al);
  }
  return r;
}

function drawAll(){
  const raw=CS.raw; if(!raw||!raw.length) return;
  const tot=raw.length,s=Math.floor(CS.zS*tot),en=Math.max(s+2,Math.floor(CS.zE*tot));
  const vis=raw.slice(s,en);
  drawMain(vis); drawVol(vis); drawRsi(vis);
}

function initCanvas(wrap,canvas){
  const dpr=window.devicePixelRatio||1,W=wrap.clientWidth,H=wrap.clientHeight;
  if(!W||!H) return null;
  canvas.width=W*dpr;canvas.height=H*dpr;
  canvas.style.width=W+'px';canvas.style.height=H+'px';
  const ctx=canvas.getContext('2d');
  ctx.setTransform(1,0,0,1,0,0);ctx.scale(dpr,dpr);ctx.clearRect(0,0,W,H);
  return{ctx,W,H};
}

function drawMain(vis){
  const wrap=document.getElementById('mainWrap'),canvas=document.getElementById('cv');
  if(!vis||!vis.length){showOv('데이터 없음',false);return;}
  hideOv();
  const r=initCanvas(wrap,canvas); if(!r) return;
  const{ctx,W,H}=r;
  const PL=58,PR=10,PT=12,PB=22,chartW=W-PL-PR,chartH=H-PT-PB,n=vis.length;
  const closes=vis.map(c=>c.close);
  const bb=calcBB(closes),ma=calcMA(closes,20);
  const allVals=vis.flatMap(c=>[c.high,c.low]).concat(bb.flatMap(b=>[b.upper,b.lower]).filter(v=>v!=null));
  let yMx=Math.max(...allVals),yMn=Math.min(...allVals);
  const sp=yMx-yMn||1;yMx+=sp*.06;yMn-=sp*.06;
  const ys=v=>PT+chartH*(1-(v-yMn)/(yMx-yMn));

  const cnt=Math.max(n-1,1);
  const step=Math.min(Math.max(chartW/Math.max(cnt,1),2),20);
  const usedW=step*(n-1);
  const xBase=n===1?PL+chartW/2:PL+Math.max(0,chartW-usedW);
  const cw=Math.max(1.5,Math.min(10,step*0.6));

  ctx.font='8px JetBrains Mono,monospace';

  /* 그리드 */
  for(let i=0;i<=4;i++){
    const y=PT+chartH*(i/4);
    ctx.strokeStyle='rgba(255,255,255,.025)';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(PL,y);ctx.lineTo(W-PR,y);ctx.stroke();
    ctx.fillStyle='rgba(122,132,153,.55)';ctx.textAlign='right';
    ctx.fillText((yMx-(yMx-yMn)*(i/4)).toLocaleString('ko-KR',{maximumFractionDigits:0}),PL-4,y+3);
  }

  /* x축 레이블 개선: 분봉은 시간, 일봉은 날짜/월 */
  const xLabelStep=n<=10?1:Math.max(1,Math.floor(n/8));
  let lastDateLabel='';
  for(let i=0;i<n;i+=xLabelStep){
    const x=xBase+i*step;
    if(x<PL||x>W-PR) continue;
    ctx.strokeStyle='rgba(255,255,255,.02)';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(x,PT);ctx.lineTo(x,H-PB);ctx.stroke();
    ctx.fillStyle='rgba(122,132,153,.5)';ctx.textAlign='center';

    let label='';
    const t=vis[i].time||'';
    if(isMinuteTf(CS.tf)){
      /* 분봉: 시간:분만, 날짜가 바뀌면 날짜도 표시 */
      const dateStr=t.slice(0,10);
      const timeStr=t.replace(/^\d{4}-\d{2}-\d{2}[\sT]?/,'').substring(0,5);
      if(dateStr&&dateStr!==lastDateLabel){
        label=dateStr.slice(5)+'↵'+timeStr;  /* MM-DD + 줄내려서 HH:mm */
        lastDateLabel=dateStr;
        ctx.fillStyle='rgba(168,255,62,.45)';
        ctx.fillText(dateStr.slice(5),x,H-PB+8);
        ctx.fillStyle='rgba(122,132,153,.5)';
        ctx.fillText(timeStr,x,H-PB+18);
      } else {
        ctx.fillText(timeStr,x,H-PB+12);
      }
    } else if(CS.tf==='1w'){
      label=t.substring(0,7); /* YYYY-MM */
      ctx.fillText(label,x,H-PB+12);
    } else {
      label=t.substring(5,10); /* MM-DD */
      ctx.fillText(label,x,H-PB+12);
    }
  }

  /* 분봉 세션 구분선 (KR: 09:00 수직선) */
  if(isMinuteTf(CS.tf)&&CS.mkt==='KR'){
    vis.forEach((c,i)=>{
      const t=(c.time||'').replace(/^\d{4}-\d{2}-\d{2}[\sT]?/,'');
      if(t.startsWith('09:00')||t.startsWith('090000')){
        const x=xBase+i*step;
        ctx.strokeStyle='rgba(168,255,62,.2)';ctx.lineWidth=1;ctx.setLineDash([4,4]);
        ctx.beginPath();ctx.moveTo(x,PT);ctx.lineTo(x,H-PB);ctx.stroke();
        ctx.setLineDash([]);
      }
    });
  }

  /* BB fill */
  ctx.beginPath();let sv=true;
  for(let i=0;i<n;i++){const b=bb[i];if(b.upper===null)continue;const x=xBase+i*step;sv?(ctx.moveTo(x,ys(b.upper)),sv=false):ctx.lineTo(x,ys(b.upper));}
  for(let i=n-1;i>=0;i--){const b=bb[i];if(b.lower===null)continue;ctx.lineTo(xBase+i*step,ys(b.lower));}
  ctx.closePath();ctx.fillStyle='rgba(77,159,255,.06)';ctx.fill();

  [{key:'upper',c:'rgba(77,159,255,.4)'},{key:'mid',c:'rgba(77,159,255,.22)'},{key:'lower',c:'rgba(77,159,255,.4)'}].forEach(({key,c})=>{
    ctx.beginPath();ctx.strokeStyle=c;ctx.lineWidth=.8;let mv=true;
    for(let i=0;i<n;i++){const b=bb[i];if(b[key]===null){mv=true;continue;}const x=xBase+i*step,y=ys(b[key]);mv?(ctx.moveTo(x,y),mv=false):ctx.lineTo(x,y);}
    ctx.stroke();
  });

  /* MA20 */
  ctx.beginPath();ctx.strokeStyle='rgba(168,255,62,.5)';ctx.lineWidth=1.1;let mm=true;
  for(let i=0;i<n;i++){if(ma[i]===null){mm=true;continue;}const x=xBase+i*step,y=ys(ma[i]);mm?(ctx.moveTo(x,y),mm=false):ctx.lineTo(x,y);}
  ctx.stroke();

  /* 캔들 */
  vis.forEach((c,i)=>{
    const x=xBase+i*step,yO=ys(c.open),yC=ys(c.close),yH=ys(c.high),yL=ys(c.low);
    const up=c.close>=c.open,fill=up?'#00d97e':'#ff4d6a',stk=up?'#009960':'#dd2040';
    ctx.strokeStyle=stk;ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(x,yH);ctx.lineTo(x,yL);ctx.stroke();
    const top=Math.min(yO,yC),bh=Math.max(Math.abs(yC-yO),1.5);
    ctx.fillStyle=fill;ctx.fillRect(x-cw/2,top,cw,bh);
    ctx.strokeStyle=stk;ctx.lineWidth=.6;ctx.strokeRect(x-cw/2,top,cw,bh);
  });

  /* 마지막 가격 점선 + 레이블 */
  const lc=vis[n-1].close,yl=ys(lc);
  ctx.setLineDash([3,3]);ctx.strokeStyle='rgba(168,255,62,.25)';ctx.lineWidth=1;
  ctx.beginPath();ctx.moveTo(PL,yl);ctx.lineTo(PL+usedW,yl);ctx.stroke();ctx.setLineDash([]);
  /* 레이블 박스 */
  const priceLabel=lc.toLocaleString('ko-KR',{maximumFractionDigits:0});
  ctx.font='bold 8px JetBrains Mono,monospace';
  const pw=ctx.measureText(priceLabel).width+8;
  ctx.fillStyle='rgba(168,255,62,.85)';
  ctx.fillRect(PL-pw-4,yl-7,pw+4,14);
  ctx.fillStyle='rgba(6,7,9,.95)';ctx.textAlign='right';
  ctx.fillText(priceLabel,PL-4,yl+3.5);

  canvas._m={PL,PR,PT,PB,W,H,yMn,yMx,vis,step,n,ys,xBase,usedW};
}

function drawVol(vis){
  const wrap=document.getElementById('volWrap'),canvas=document.getElementById('cvVol');
  if(!vis||!vis.length) return;
  const r=initCanvas(wrap,canvas);if(!r) return;
  const{ctx,W,H}=r;
  const PL=58,PR=10,PT=3,PB=10,chartW=W-PL-PR,chartH=H-PT-PB,n=vis.length;
  const maxV=Math.max(...vis.map(c=>c.volume),1);
  const cnt=Math.max(n-1,1),step=Math.min(Math.max(chartW/Math.max(cnt,1),2),20);
  const usedW=step*(n-1),xBase=n===1?PL+chartW/2:PL+Math.max(0,chartW-usedW);
  const cw=Math.max(1.5,Math.min(10,step*0.56));
  ctx.font='7px JetBrains Mono,monospace';ctx.fillStyle='rgba(122,132,153,.35)';ctx.textAlign='right';
  const vL=maxV>=1e8?(maxV/1e8).toFixed(1)+'억':maxV>=1e6?(maxV/1e6).toFixed(1)+'M':maxV>=1e3?(maxV/1e3).toFixed(0)+'K':''+maxV;
  ctx.fillText(vL,PL-4,PT+9);
  vis.forEach((c,i)=>{
    const x=xBase+i*step;if(x<PL||x>W-PR) return;
    const bh=Math.max((c.volume/maxV)*chartH,1),y=PT+chartH-bh;
    ctx.fillStyle=c.close>=c.open?'rgba(0,217,126,.45)':'rgba(255,77,106,.45)';
    ctx.fillRect(x-cw/2,y,cw,bh);
  });
}

function drawRsi(vis){
  const wrap=document.getElementById('rsiWrap'),canvas=document.getElementById('cvRsi');
  if(!vis||!vis.length) return;
  const r=initCanvas(wrap,canvas);if(!r) return;
  const{ctx,W,H}=r;
  const PL=58,PR=10,PT=3,PB=10,chartW=W-PL-PR,chartH=H-PT-PB,n=vis.length;
  const closes=vis.map(c=>c.close),rsi=calcRSI(closes,14);
  const cnt=Math.max(n-1,1),step=Math.min(Math.max(chartW/Math.max(cnt,1),2),20);
  const usedW=step*(n-1),xBase=n===1?PL+chartW/2:PL+Math.max(0,chartW-usedW);
  ctx.fillStyle='rgba(255,77,106,.04)';ctx.fillRect(PL,PT,chartW,chartH*(1-70/100));
  ctx.fillStyle='rgba(77,159,255,.04)';ctx.fillRect(PL,PT+chartH*(1-30/100),chartW,chartH*(30/100));
  [[70,'rgba(255,77,106,.35)'],[50,'rgba(255,255,255,.06)'],[30,'rgba(77,159,255,.35)']].forEach(([v,c])=>{
    const y=PT+chartH*(1-v/100);
    ctx.strokeStyle=c;ctx.lineWidth=.7;ctx.setLineDash([3,3]);
    ctx.beginPath();ctx.moveTo(PL,y);ctx.lineTo(W-PR,y);ctx.stroke();ctx.setLineDash([]);
    ctx.fillStyle=c;ctx.font='7px JetBrains Mono,monospace';ctx.textAlign='right';ctx.fillText(v,PL-4,y+3);
  });
  ctx.beginPath();ctx.strokeStyle='#f5c842';ctx.lineWidth=1.1;let mv=true;
  for(let i=0;i<n;i++){
    if(rsi[i]===null){mv=true;continue;}
    const x=xBase+i*step;if(x<PL||x>W-PR){mv=true;continue;}
    const y=PT+chartH*(1-rsi[i]/100);
    mv?(ctx.moveTo(x,y),mv=false):ctx.lineTo(x,y);
  }
  ctx.stroke();
  const lastRsi=rsi.filter(v=>v!==null).pop();
  if(lastRsi!=null){
    const yy=PT+chartH*(1-lastRsi/100);
    const col=lastRsi<30?'#4d9fff':lastRsi>70?'#ff4d6a':'#f5c842';
    ctx.beginPath();ctx.arc(xBase+(n-1)*step,yy,2.5,0,Math.PI*2);ctx.fillStyle=col;ctx.fill();
    ctx.fillStyle=col;ctx.font='bold 8px JetBrains Mono,monospace';ctx.textAlign='left';
    ctx.fillText(lastRsi.toFixed(1),W-PR+2,yy+3);
  }
}

/* ── 크로스헤어 ── */
const mainWrap=document.getElementById('mainWrap');
const cvl=document.getElementById('cvl'),chl=document.getElementById('chl'),ctp=document.getElementById('ctp');
mainWrap.addEventListener('mousemove',ev=>{
  const m=document.getElementById('cv')._m;if(!m) return;
  const rect=mainWrap.getBoundingClientRect(),mx=ev.clientX-rect.left,my=ev.clientY-rect.top;
  const{PL,PR,PT,PB,W,H,vis,step,xBase}=m;
  if(mx<xBase-step/2||mx>xBase+step*(vis.length-1)+step/2||my<PT||my>H-PB){
    cvl.style.display='none';chl.style.display='none';ctp.style.display='none';return;
  }
  const idx=Math.round((mx-xBase)/step),c=vis[Math.max(0,Math.min(idx,vis.length-1))];
  const cx=xBase+Math.max(0,Math.min(idx,vis.length-1))*step;
  cvl.style.display='block';cvl.style.left=cx+'px';
  chl.style.display='block';chl.style.top=my+'px';
  ctp.style.display='block';
  const closes=vis.map(v=>v.close),rsiArr=calcRSI(closes,14),rv=rsiArr[Math.max(0,Math.min(idx,rsiArr.length-1))];
  const bbArr=calcBB(closes),bv=bbArr[Math.max(0,Math.min(idx,bbArr.length-1))];
  const volV=c.volume>=1e6?(c.volume/1e6).toFixed(2)+'M':c.volume>=1e3?(c.volume/1e3).toFixed(0)+'K':c.volume.toLocaleString();
  const tw=195,tx=mx+10>W-tw?mx-tw-4:mx+10,ty=Math.min(my+6,H-140);
  ctp.style.left=tx+'px';ctp.style.top=ty+'px';
  const up=c.close>=c.open;
  ctp.innerHTML=
    '<div style="font-size:7px;color:var(--t3);margin-bottom:4px;letter-spacing:.5px">'+esc(c.time||'')+'</div>'
    +'<div style="display:grid;grid-template-columns:1fr 1fr;gap:1px 10px;font-size:9px;">'
    +'<span style="color:var(--t2)">O</span><span>'+c.open.toLocaleString('ko-KR')+'</span>'
    +'<span style="color:var(--t2)">H</span><span style="color:#00d97e">'+c.high.toLocaleString('ko-KR')+'</span>'
    +'<span style="color:var(--t2)">L</span><span style="color:#ff4d6a">'+c.low.toLocaleString('ko-KR')+'</span>'
    +'<span style="color:var(--t2)">C</span><span style="color:'+(up?'#00d97e':'#ff4d6a')+';font-weight:600">'+c.close.toLocaleString('ko-KR')+'</span>'
    +'<span style="color:var(--t2)">VOL</span><span style="color:var(--gold)">'+esc(volV)+'</span>'
    +(rv!=null?'<span style="color:var(--t2)">RSI</span><span style="color:var(--gold)">'+rv.toFixed(1)+'</span>':'')
    +(bv&&bv.upper!=null?'<span style="color:var(--t2)">BB↑</span><span style="color:var(--blue)">'+bv.upper.toFixed(0)+'</span>':'')
    +(bv&&bv.lower!=null?'<span style="color:var(--t2)">BB↓</span><span style="color:var(--blue)">'+bv.lower.toFixed(0)+'</span>':'')
    +'</div>';
});
mainWrap.addEventListener('mouseleave',()=>{cvl.style.display='none';chl.style.display='none';ctp.style.display='none';});

/* ── 줌/패닝 ── */
const cWrap=document.getElementById('cWrap');
window.zm=function(d,anchor){
  const rng=CS.zE-CS.zS,nr=Math.max(.03,Math.min(1,rng*(d>0?.78:1.28)));
  const a=Math.max(0,Math.min(1,anchor==null?.5:anchor));
  const pivot=CS.zS+rng*a;let ns=pivot-nr*a,ne=ns+nr;
  if(ns<0){ne-=ns;ns=0;}if(ne>1){ns-=ne-1;ne=1;}
  CS.zS=Math.max(0,ns);CS.zE=Math.min(1,ne);drawAll();
};
window.zmR=function(){resetViewRange();drawAll();};
cWrap.addEventListener('wheel',ev=>{
  ev.preventDefault();
  const rect=cWrap.getBoundingClientRect(),anchor=(ev.clientX-rect.left)/Math.max(rect.width,1);
  zm(ev.deltaY<0?1:-1,anchor);
},{passive:false});
cWrap.addEventListener('mousedown',ev=>{
  if(ev.button!==0) return;
  CS.drag=true;CS.dragX=ev.clientX;CS.dragZ=[CS.zS,CS.zE];cWrap.classList.add('drag');
});
window.addEventListener('mouseup',()=>{CS.drag=false;cWrap.classList.remove('drag');});
cWrap.addEventListener('dblclick',()=>{zmR();});
window.addEventListener('mousemove',ev=>{
  if(!CS.drag||!CS.dragZ) return;
  const dx=(ev.clientX-CS.dragX)/cWrap.getBoundingClientRect().width,rng=CS.dragZ[1]-CS.dragZ[0];
  let ns=CS.dragZ[0]-dx,ne=CS.dragZ[1]-dx;
  if(ns<0){ne-=ns;ns=0;}if(ne>1){ns-=(ne-1);ne=1;}
  CS.zS=Math.max(0,ns);CS.zE=Math.min(1,ne);drawAll();
});
window.addEventListener('resize',drawAll);

/* ══════════════════════════════════════════
   사이드바
══════════════════════════════════════════ */
let sbD=[],sbM='KR',sbE='NAS',sbT='vol',sbAuto=true,sbPrev=new Set(),sbCd=POLL,sbTimer=null;

/* ── 보유 종목 렌더 ── */
function renderSbHold(){
  const body=document.getElementById('sbBody');
  if(!holdings.length){
    body.innerHTML='<div class="sym-section"><div class="sym-empty">보유 종목 없음<br><span style="font-size:8px;color:var(--t3)">잔고 조회 후 표시됩니다</span></div></div>';
    return;
  }
  body.innerHTML='<div class="sym-section">'
    +'<div class="sym-sec-hd"><span class="sym-sec-title">보유 종목</span><span class="sym-sec-cnt">'+holdings.length+'</span></div>'
    +holdings.map(h=>{
      const pc=h.pnl>=0?'pos':'neg';
      return '<div class="sym-row" data-sym="'+esc(h.sym)+'" data-mkt="KR">'
        +'<span class="sym-row-code">'+esc(h.sym)+'</span>'
        +'<span class="sym-row-name">'+esc(h.name)+'</span>'
        +'<div class="sym-row-info">'
        +'<div class="sym-row-price">'+Math.round(h.curP).toLocaleString('ko-KR')+'</div>'
        +'<div class="sym-row-pnl '+pc+'">'+(h.pnl>=0?'+':'')+Math.round(h.pnl).toLocaleString('ko-KR')
        +' ('+h.pnlPct.toFixed(2)+'%)</div>'
        +'</div>'
        +'<span class="hold-badge">'+Math.round(h.qty)+'주</span>'
        +'</div>';
    }).join('')
    +'</div>';
}

/* ── 최근 본 종목 렌더 ── */
function renderSbRecent(){
  const body=document.getElementById('sbBody');
  if(!recentSyms.length){
    body.innerHTML='<div class="sym-section"><div class="sym-empty">최근 본 종목 없음<br><span style="font-size:8px;color:var(--t3)">차트를 조회하면 여기에 표시됩니다</span></div></div>';
    return;
  }
  body.innerHTML='<div class="sym-section">'
    +'<div class="sym-sec-hd"><span class="sym-sec-title">최근 본 종목</span><span class="sym-sec-cnt">'+recentSyms.length+'</span></div>'
    +recentSyms.map(r=>{
      return '<div class="sym-row" data-sym="'+esc(r.sym)+'" data-mkt="'+esc(r.mkt)+'">'
        +'<span class="sym-row-code">'+esc(r.sym)+'</span>'
        +'<span class="sym-row-name">'+esc(r.name)+'</span>'
        +'<div class="sym-row-info">'
        +'<div class="sym-row-price">'+Number(r.price).toLocaleString('ko-KR')+'</div>'
        +'</div>'
        +'<span class="recent-badge">'+esc(r.mkt)+'</span>'
        +'</div>';
    }).join('')
    +'</div>';
}

/* 사이드바 클릭 이벤트 위임 */
document.getElementById('sbBody').addEventListener('click',function(e){
  const row=e.target.closest('.rk-row, .sym-row');
  if(!row) return;
  const sym=row.dataset.sym||row.dataset.code;
  const mkt=row.dataset.mkt;
  const exch=row.dataset.exch;
  if(!sym) return;
  CS.sym=sanitizeSymbol(sym);
  if(mkt) CS.mkt=(mkt==='US')?'US':'KR';
  if(exch&&['NAS','NYS','AMS'].includes(exch)) CS.exch=exch;
  document.getElementById('symIn').value=CS.sym;
  ['KR','US'].forEach(x=>document.getElementById('m'+x).className='mkt-b'+(x===CS.mkt?(CS.mkt==='KR'?' on kr':' on us'):''));
  document.getElementById('exchS').classList.toggle('show',CS.mkt==='US');
  fetchChart();
});

window.setSbMkt=function(m){
  sbM=m;
  ['KR','US'].forEach(x=>document.getElementById('sm'+x).className='mkt-sb'+(x===m?(m==='KR'?' on kr':' on us'):''));
  document.getElementById('sbExch').classList.toggle('show',m==='US');
  document.getElementById('sbTabs').className='sb-tabs'+(m==='US'?' us':'');
  document.getElementById('sbFt').textContent='KIS · /api/market/ranking?market='+esc(m);
  loadRanking();
};
window.setSbExch=function(ex){
  const allowed=['NAS','NYS','AMS'];if(!allowed.includes(ex)) return;
  sbE=ex;['NAS','NYS','AMS'].forEach(x=>document.getElementById('ex'+x).classList.toggle('on',x===ex));
  loadRanking();
};
window.setSbTab=function(t){
  sbT=t;
  ['Vol','Chg','Hold','Recent'].forEach(x=>{
    const el=document.getElementById('st'+x);if(el) el.classList.toggle('on',x.toLowerCase()===t);
  });
  /* 랭킹/보유/최근 렌더 분기 */
  if(t==='hold'){renderSbHold();return;}
  if(t==='recent'){renderSbRecent();return;}
  if(sbD.length) renderRank(sbD);
};
window.toggleAuto=function(){
  sbAuto=!sbAuto;
  document.getElementById('sbAb').classList.toggle('act',sbAuto);
  if(sbAuto) restartCd(); else{clearInterval(sbTimer);document.getElementById('sbPgf').style.width='0%';document.getElementById('sbCdn').textContent='—';}
};

window.loadRanking=function(){
  const rb=document.getElementById('sbRb');rb.classList.add('spin');
  safeFetch(B+'/api/market/ranking?market='+encodeURIComponent(sbM)+'&exch='+encodeURIComponent(sbE))
    .then(json=>{
      const raw=json.data||json.output||json.items||[];
      sbD=Array.isArray(raw)?raw.slice(0,30):[];
      if(!sbD.length&&json.rt_cd&&json.rt_cd!=='0'){
        showSbErr('API 오류: '+esc(json.msg1||json.msg||'알 수 없음'));
      } else if(sbT==='vol'||sbT==='chg'){
        renderRank(sbD);
        const n=new Date();
        document.getElementById('sbUpd').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds())+' 갱신';
        if(sbAuto) restartCd();
      }
    })
    .catch(err=>showSbErr(esc(err.message)))
    .finally(()=>rb.classList.remove('spin'));
};

function showSbErr(msg){
  document.getElementById('sbBody').innerHTML='<div class="sb-msg er"><div class="sb-mi">✕</div><div class="sb-mt">'+esc(msg)+'</div></div>';
}

function renderRank(data){
  const body=document.getElementById('sbBody');
  if(!data.length){body.innerHTML='<div class="sb-msg"><div class="sb-mi">—</div><div class="sb-mt">데이터 없음</div></div>';return;}
  const isUS=sbM==='US';
  const metric=r=>parseFloat((r&&r.acml_tr_pbmn!=null)?r.acml_tr_pbmn:r&&r.acml_vol)||0;
  const sorted=[...data].sort((a,b)=>{
    if(sbT==='chg') return Math.abs(parseFloat(b.prdy_ctrt)||0)-Math.abs(parseFloat(a.prdy_ctrt)||0);
    return metric(b)-metric(a);
  }).slice(0,30);
  const maxV=Math.max(...sorted.map(r=>metric(r)),1);
  const newSet=new Set(sorted.map(r=>r.symbol));
  body.innerHTML='<div class="rk-list">'+sorted.map((r,i)=>{
    const rk=i+1,rc=rk===1?'r1':rk===2?'r2':rk===3?'r3':'rN';
    const code=esc(r.symbol||'—'),name=esc(r.name||r.symbol||'—');
    const rawP=r.stck_prpr;
    const price=rawP&&rawP!=='0'?(isUS?'$'+Number(rawP).toFixed(2):Number(rawP).toLocaleString('ko-KR')):'—';
    const diff=parseFloat(r.prdy_ctrt||'0'),sign=r.prdy_vrss_sign||'3';
    const isUp=sign==='1'||sign==='2',isDn=sign==='4'||sign==='5';
    let cc,pfx;if(isUp){cc=isUS?'uu':'ku';pfx='▲';}else if(isDn){cc=isUS?'ud':'kd';pfx='▼';}else{cc='fl';pfx='—';}
    const vol=metric(r),vp=Math.max((vol/maxV*100),2).toFixed(1);
    const vf=vol>=1e8?(vol/1e8).toFixed(1)+'억':vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
    const isNew=!sbPrev.has(r.symbol);
    return '<div class="rk-row'+(isNew?' fl':'')+'" style="animation-delay:'+(i*10)+'ms" data-code="'+code+'" data-sym="'+code+'" data-mkt="'+esc(sbM)+'" data-exch="'+esc(sbE)+'">'
      +'<span class="rn '+rc+'">'+rk+'</span>'
      +'<div class="ri-i"><div class="ri-nm" title="'+name+'">'+name+'</div><div class="ri-cd">'+code+'</div>'
      +'<div class="ri-bw"><div class="ri-b '+(isUS?'us':'kr')+'" style="width:'+vp+'%"></div><div class="ri-vl">'+esc(vf)+'</div></div>'
      +'</div>'
      +'<div class="rp-i"><div class="rp-pr">'+esc(price)+'</div><div class="rp-ch '+cc+'">'+pfx+Math.abs(diff).toFixed(2)+'%</div></div>'
      +'</div>';
  }).join('')+'</div>';
  sbPrev=newSet;
}

function restartCd(){
  sbCd=POLL;updPg();clearInterval(sbTimer);
  sbTimer=setInterval(()=>{sbCd--;document.getElementById('sbCdn').textContent=sbCd;updPg();if(sbCd<=0)loadRanking();},1000);
}
function updPg(){document.getElementById('sbPgf').style.width=((POLL-sbCd)/POLL*100)+'%';}

/* ── 초기 로드 ── */
updateDatePicker();
fetchChart();
loadRanking();
</script>
</body>
</html>
