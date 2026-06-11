<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>Backtest · AUTOTRADE TERMINAL</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
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
  --t1:#f0f4ff;--t2:#c8d4e8;--t3:#8a96aa;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Malgun Gothic','맑은 고딕',sans-serif;
  --r:6px;--r2:10px;--topbar-h:56px;--sidebar-w:510px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);overflow-x:hidden;}
.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 90% 60% at 50% -10%,rgba(168,255,62,.07) 0%,transparent 55%),
             radial-gradient(ellipse 50% 70% at 100% 80%,rgba(0,217,126,.04) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.04) 1px,transparent 1px);background-size:28px 28px;}

@keyframes pd{0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.65);opacity:.25;}}

/* TOPBAR */
.topbar{position:fixed;top:0;left:0;right:0;z-index:200;height:var(--topbar-h);
  display:flex;align-items:center;
  background:rgba(6,7,9,.94);backdrop-filter:blur(14px);border-bottom:1px solid var(--rim);}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime),rgba(168,255,62,.3),transparent);opacity:.5;}
.tb-logo{display:flex;align-items:center;gap:11px;padding:0 22px;height:100%;
  border-right:1px solid var(--rim);min-width:210px;text-decoration:none;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;box-shadow:var(--lime-glow);}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;color:var(--t1);}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-pill{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:9px;color:var(--emerald);
  padding:3px 9px;border-radius:20px;background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);box-shadow:0 0 10px rgba(0,217,126,.4);animation:pd 1.4s ease-in-out infinite;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
.tb-a{font-family:var(--mono);font-size:10px;letter-spacing:.4px;padding:5px 11px;
  border-radius:var(--r);border:1px solid transparent;background:transparent;color:var(--t2);
  cursor:pointer;transition:all .15s;text-decoration:none;}
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
.f-label{font-family:var(--sans);font-size:10px;color:var(--t2);letter-spacing:.3px;
  text-transform:uppercase;margin-bottom:5px;display:block;}
/* GLOBAL TOOLTIP */
#_gtip{position:fixed;z-index:9999;background:#111d2d;color:#c0ccdc;border:1px solid #3a4e6a;
  border-radius:6px;padding:7px 10px;font-size:11px;font-family:'Malgun Gothic','맑은 고딕',sans-serif;
  font-weight:normal;letter-spacing:0;text-transform:none;max-width:220px;line-height:1.55;
  pointer-events:none;display:none;box-shadow:0 4px 16px rgba(0,0,0,0.5);white-space:pre-line;}
.f-input{width:100%;height:32px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--sans);font-size:12px;
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
.sym-tags{display:flex;flex-wrap:wrap;gap:4px;margin-top:6px;min-height:4px;}
.sym-tag{display:inline-flex;align-items:center;gap:4px;padding:3px 8px 3px 10px;
  background:rgba(168,255,62,.12);border:1px solid rgba(168,255,62,.3);border-radius:12px;
  font-family:var(--mono);font-size:11px;font-weight:700;color:var(--lime);}
.sym-tag-x{cursor:pointer;color:var(--t3);font-size:13px;line-height:1;padding:0 1px;}
.sym-tag-x:hover{color:#ff5555;}
.btn-sym-add{padding:0 11px;height:32px;background:var(--panel-hi);
  border:1px solid var(--rim-hi);border-radius:var(--r);color:var(--lime);
  font-family:var(--mono);font-size:11px;cursor:pointer;flex-shrink:0;white-space:nowrap;}
.btn-sym-add:hover{background:var(--hover);}
.sym-count{font-family:var(--mono);font-size:9px;color:var(--t3);margin-left:2px;}

/* TOP50 PANEL */
.top50-panel{background:var(--base);border:1px solid var(--rim);border-radius:var(--r2);
  margin-bottom:12px;overflow:hidden;}
.top50-hd{display:flex;align-items:center;justify-content:space-between;
  padding:8px 12px;border-bottom:1px solid var(--rim);background:var(--panel);}
.top50-title{font-family:var(--sans);font-size:11px;font-weight:700;color:var(--gold);}
.top50-sel-info{font-family:var(--sans);font-size:10px;color:var(--t2);}
.top50-ctrl{display:flex;align-items:center;gap:5px;padding:7px 10px;border-bottom:1px solid var(--rim);}
.top50-ctrl input[type=number]{width:52px;height:26px;background:var(--panel-hi);
  border:1px solid var(--rim-hi);border-radius:var(--r);color:var(--t1);
  font-family:var(--sans);font-size:11px;padding:0 6px;text-align:center;}
.top50-btn{height:26px;padding:0 10px;border-radius:var(--r);border:1px solid;
  font-family:var(--sans);font-size:11px;cursor:pointer;transition:all .12s;white-space:nowrap;flex-shrink:0;}
.top50-btn-add{background:var(--gold-d);border-color:var(--gold-b);color:var(--gold);}
.top50-btn-add:hover{background:var(--gold);color:var(--void);}
.top50-btn-clr{background:transparent;border-color:var(--rim-hi);color:var(--t2);}
.top50-btn-clr:hover{border-color:var(--rim-hi);color:var(--t1);}
.top50-btn-ref{background:transparent;border-color:var(--rim-hi);color:var(--t2);width:26px;padding:0;display:flex;align-items:center;justify-content:center;}
.top50-btn-ref:hover{color:var(--t1);}
.top50-list{max-height:260px;overflow-y:auto;}
.top50-list::-webkit-scrollbar{width:3px;}
.top50-list::-webkit-scrollbar-track{background:transparent;}
.top50-list::-webkit-scrollbar-thumb{background:var(--t4);border-radius:2px;}
.top50-row{display:flex;align-items:center;gap:0;padding:5px 10px;cursor:pointer;
  border-bottom:1px solid var(--rim);transition:background .1s;}
.top50-row:last-child{border-bottom:none;}
.top50-row:hover{background:var(--hover);}
.top50-row.sel{background:rgba(245,200,66,.09);border-bottom-color:rgba(245,200,66,.18);}
.top50-rank{font-family:var(--mono);font-size:10px;color:var(--t2);min-width:20px;flex-shrink:0;}
.top50-rank.r1{color:#ffd700;font-weight:700;}
.top50-rank.r2{color:#c0c0c0;font-weight:700;}
.top50-rank.r3{color:#cd7f32;font-weight:700;}
.top50-sym{font-family:var(--mono);font-size:11px;font-weight:700;color:var(--lime);min-width:58px;flex-shrink:0;}
.top50-nm{font-family:var(--sans);font-size:11px;color:var(--t1);flex:1;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.top50-rate{font-family:var(--mono);font-size:10px;min-width:46px;text-align:right;flex-shrink:0;}
.top50-rate.up{color:var(--red);}
.top50-rate.dn{color:var(--blue);}
.top50-rate.fl{color:var(--t2);}
.top50-chk{width:13px;height:13px;border-radius:3px;border:1px solid var(--rim-hi);
  background:transparent;flex-shrink:0;margin-right:6px;display:flex;align-items:center;justify-content:center;}
.top50-row.sel .top50-chk{background:var(--gold);border-color:var(--gold);}
.top50-row.sel .top50-chk::after{content:'✓';font-size:9px;color:var(--void);font-weight:900;}

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
.p-grid .f-input{font-family:var(--sans);font-size:11px;height:28px;}

/* BUTTONS */
.btn{width:100%;height:36px;border-radius:var(--r);border:1px solid;font-family:var(--mono);
  font-size:11px;font-weight:600;letter-spacing:.5px;cursor:pointer;transition:all .15s;}
.btn:disabled{opacity:.35;cursor:not-allowed;}
.btn-collect{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.btn-collect:hover:not(:disabled){background:var(--blue);color:var(--void);}
.btn-run{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);margin-bottom:7px;}
.btn-run:hover:not(:disabled){background:var(--lime);color:var(--void);box-shadow:var(--lime-glow);}
.btn-ai{background:#1a1a2e;border-color:#4a3f7a;color:#a78bfa;}
.btn-ai:hover:not(:disabled){background:#4a3f7a;color:#fff;}
.btn-row{display:flex;gap:7px;}
.btn-reset{height:26px;padding:0;border:1px solid var(--rim-hi);border-radius:var(--r);
  background:transparent;color:var(--t3);font-family:var(--mono);font-size:9px;
  cursor:pointer;transition:all .15s;letter-spacing:.5px;width:auto;padding:0 10px;margin-bottom:12px;}
.btn-reset:hover{border-color:var(--rim-hi);color:var(--t2);}

/* PROGRESS */
.progress-wrap{margin-top:10px;display:none;}
.pb-outer{height:3px;background:var(--t4);border-radius:2px;overflow:hidden;margin-bottom:5px;}
.pb-inner{height:100%;background:var(--blue);border-radius:2px;transition:width .3s;}
.pb-msg{font-family:var(--mono);font-size:9px;color:var(--t2);}

/* RUN MODAL */
.run-modal{position:fixed;inset:0;z-index:500;display:flex;align-items:center;justify-content:center;
  background:rgba(0,2,8,.78);backdrop-filter:blur(22px);}
.run-modal-box{
  width:540px;max-width:95vw;
  background:linear-gradient(155deg,#0e1623 0%,#080d18 55%,#0a0f1c 100%);
  border-radius:20px;overflow:hidden;
  box-shadow:0 48px 120px rgba(0,0,0,.95),0 0 0 1px rgba(99,179,255,.12);}
.rm-top-bar{height:3px;background:linear-gradient(90deg,#6366f1 0%,#06b6d4 50%,#6366f1 100%);
  background-size:200% 100%;animation:rmTopFlow 3s linear infinite;}
@keyframes rmTopFlow{0%{background-position:0% 0}100%{background-position:200% 0}}
.rm-inner{padding:40px 44px 36px;}
.rm-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:34px;}
.rm-tag{font-family:var(--mono);font-size:8px;letter-spacing:3px;
  color:rgba(99,179,255,.5);text-transform:uppercase;margin-bottom:10px;}
.rm-title{font-size:22px;font-weight:800;color:#f1f5ff;letter-spacing:-.4px;line-height:1;}
.rm-time-box{text-align:right;}
.rm-time-label{font-family:var(--mono);font-size:8px;letter-spacing:2.5px;
  color:rgba(255,255,255,.2);text-transform:uppercase;margin-bottom:6px;}
.rm-elapsed{font-family:var(--mono);font-size:20px;font-weight:700;color:rgba(255,255,255,.55);
  letter-spacing:2px;}
.rm-big-pct{font-family:var(--mono);font-size:60px;font-weight:700;
  color:#06b6d4;letter-spacing:-3px;line-height:1;margin-bottom:14px;}
.rm-pb-track{height:5px;background:rgba(255,255,255,.06);border-radius:3px;
  overflow:hidden;margin-bottom:28px;position:relative;}
.rm-pb-fill{height:100%;border-radius:3px;
  background:linear-gradient(90deg,#6366f1,#06b6d4);
  transition:width .5s cubic-bezier(.4,0,.2,1);
  box-shadow:0 0 14px rgba(6,182,212,.5);
  position:relative;overflow:hidden;}
.rm-pb-fill::after{content:'';position:absolute;top:0;right:-80px;bottom:0;width:80px;
  background:linear-gradient(90deg,transparent,rgba(255,255,255,.45),transparent);
  animation:rmShine 2s ease-in-out infinite;}
@keyframes rmShine{0%,100%{transform:translateX(-80px);opacity:0}45%,55%{opacity:1}100%{transform:translateX(80px);opacity:0}}
.rm-pills{display:flex;gap:8px;margin-bottom:28px;}
.rm-pill{display:flex;align-items:center;gap:7px;padding:7px 14px;border-radius:8px;
  border:1px solid rgba(255,255,255,.07);background:rgba(255,255,255,.03);
  font-family:var(--mono);font-size:10px;letter-spacing:.8px;
  color:rgba(255,255,255,.3);text-transform:uppercase;transition:all .3s;}
.rm-pill-dot{width:6px;height:6px;border-radius:50%;background:rgba(255,255,255,.15);transition:all .3s;}
.rm-pill.active{border-color:rgba(6,182,212,.35);background:rgba(6,182,212,.08);color:#67e8f9;}
.rm-pill.active .rm-pill-dot{background:#06b6d4;box-shadow:0 0 8px rgba(6,182,212,.8);}
.rm-pill.done{border-color:rgba(52,211,153,.25);background:rgba(52,211,153,.06);color:#6ee7b7;}
.rm-pill.done .rm-pill-dot{background:#34d399;}
.rm-log-box{background:rgba(0,0,0,.3);border:1px solid rgba(255,255,255,.05);
  border-radius:10px;padding:16px 18px;}
.rm-log{font-family:var(--mono);font-size:13px;font-weight:500;color:#e2e8ff;
  min-height:20px;letter-spacing:.1px;}
.rm-log-sub{font-family:var(--mono);font-size:10px;color:rgba(255,255,255,.4);
  margin-top:6px;min-height:14px;letter-spacing:.5px;}

/* ── RESULTS ── */
.results-header{margin-bottom:20px;}
.results-meta{font-family:var(--mono);font-size:10px;color:#8090a8;margin-bottom:16px;
  padding:8px 12px;background:var(--panel);border:1px solid var(--rim);border-radius:var(--r);}
.results-meta span{color:#c0ccdc;}

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
.kpi-label{font-family:var(--mono);font-size:9px;color:#c8d4e4;letter-spacing:1.5px;
  text-transform:uppercase;margin-bottom:8px;}
.kpi-val{font-family:var(--mono);font-size:22px;font-weight:600;line-height:1;margin-bottom:4px;}
.kpi-val.pos{color:var(--emerald);}
.kpi-val.neg{color:var(--red);}
.kpi-val.neu{color:var(--t1);}
.kpi-val.gold-c{color:var(--gold);}
.kpi-sub{font-family:var(--mono);font-size:10px;color:#8090a8;}
.kpi-sub b{color:#c0ccdc;}

/* EQUITY CHART */
.chart-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:16px;margin-bottom:20px;}
.chart-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.chart-title{font-family:var(--mono);font-size:9px;color:#d0d8ea;letter-spacing:2px;text-transform:uppercase;}
.chart-stat{font-family:var(--mono);font-size:11px;}
canvas#equityChart{width:100%;height:110px;display:block;border-radius:var(--r);}

/* STAT TABLES */
.stat-row{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px;}
.stat-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);padding:14px 16px;}
.stat-title{font-family:var(--mono);font-size:9px;color:#d0d8ea;letter-spacing:2px;
  text-transform:uppercase;margin-bottom:10px;padding-bottom:6px;border-bottom:1px solid var(--rim);}
.tbl{width:100%;border-collapse:collapse;font-family:var(--mono);font-size:11px;}
.tbl th{color:#c8d4e4;font-size:9px;letter-spacing:.8px;text-transform:uppercase;
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
.mb-sp{background:rgba(0,217,126,.12);color:var(--emerald);}
.mb-vr{background:rgba(255,185,56,.12);color:var(--gold);}
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
.trades-title{font-family:var(--mono);font-size:9px;color:#d0d8ea;letter-spacing:2px;text-transform:uppercase;}
.trades-count{font-family:var(--mono);font-size:10px;color:#8090a8;}
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

/* ── ACCORDION ── */
.acc-section{border:1px solid var(--rim);border-radius:var(--r);margin-bottom:5px;overflow:hidden;}
.acc-header{padding:9px 14px;cursor:pointer;display:flex;align-items:center;justify-content:space-between;
  background:var(--panel-hi);font-family:var(--sans);font-size:11px;letter-spacing:.3px;
  color:var(--t1);user-select:none;transition:background .15s;}
.acc-header:hover{background:var(--hover);}
.acc-arrow{font-size:8px;transition:transform .2s;color:var(--t2);margin-left:6px;}
.acc-header.is-open .acc-arrow{transform:rotate(180deg);}
.acc-body{display:none;padding:10px 12px 12px;border-top:1px solid var(--rim);}
.acc-body.is-open{display:block;}
.acc-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;margin-right:6px;}
.acc-hdr-l{display:flex;align-items:center;}

/* Mode enable row */
.mode-enable{display:flex;align-items:center;gap:9px;padding:7px 10px;
  background:var(--lime-d);border:1px solid var(--lime-b);border-radius:var(--r);margin-bottom:10px;}
.mode-enable input[type=checkbox]{accent-color:var(--lime);width:15px;height:15px;cursor:pointer;}
.mode-enable label{font-family:var(--sans);font-size:12px;font-weight:700;color:var(--lime);cursor:pointer;}

/* Checkbox row */
.cb-row{display:flex;align-items:center;gap:7px;margin-bottom:7px;}
.cb-row input[type=checkbox]{width:13px;height:13px;accent-color:var(--lime);cursor:pointer;flex-shrink:0;}
.cb-label{font-family:var(--sans);font-size:11px;color:var(--t1);cursor:pointer;line-height:1.4;}

/* Accordion sub-section label */
.acc-sub{font-family:var(--sans);font-size:10px;font-weight:700;letter-spacing:.5px;
  color:var(--gold);margin:10px 0 6px;padding-bottom:4px;border-bottom:1px solid rgba(255,255,255,.06);}

/* Sidebar padding tighter in accordion */
.acc-body .p-grid{margin-bottom:2px;}
.acc-body .f-group{margin-bottom:8px;}

/* Extra KPI grid row */
.kpi-grid-2{display:grid;grid-template-columns:repeat(5,1fr);gap:10px;margin-bottom:20px;}
.kpi-grid-2 .kpi-val{font-size:18px;}

/* Mode stat table wide */
.stat-row-3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;margin-bottom:20px;}
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
  <div class="tb-pill"><div class="tb-dot"></div><span id="hdSt">—</span></div>
  <div class="tb-nav">
    <a class="tb-a"     href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/control/kr">Control·KR</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/control/us">Control·US</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/monitor">Monitor</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/history/orders">Orders</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/balances">Balances</a>
    <a class="tb-a"     href="<%= request.getContextPath() %>/watchlist">Watchlist</a>
    <a class="tb-a cur" href="<%= request.getContextPath() %>/backtest">Backtest</a>
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

      <!-- Symbol (multi) -->
      <div class="f-group">
        <label class="f-label" id="symLabel">종목코드 <span class="sym-count" id="symCount"></span></label>
        <div class="sym-wrap">
          <div class="sym-input-row">
            <input type="text" class="f-input" id="symbolInput"
                   placeholder="예) 005930  Enter로 추가"
                   oninput="this.value=this.value.toUpperCase();onSymbolInput()"
                   onkeydown="if(event.key==='Enter'){event.preventDefault();addCurrentSymbol();}"
                   autocomplete="off">
            <button class="btn-sym-add" onclick="addCurrentSymbol()">+ 추가</button>
            <div class="sym-loading" id="symLoading"></div>
          </div>
          <div class="sym-tags" id="symTags"></div>
          <div class="sym-dropdown" id="symDropdown"></div>
        </div>
      </div>

      <!-- 거래대금 Top50 picker (KRX only) -->
      <div class="top50-panel" id="top50Panel" style="display:none;">
        <div class="top50-hd">
          <span class="top50-title">거래대금 Top 50 · KRX</span>
          <span class="top50-sel-info" id="top50SelInfo">선택 0</span>
        </div>
        <div class="top50-ctrl">
          <span style="font-family:var(--mono);font-size:9px;color:var(--t3);flex-shrink:0;">Top</span>
          <input type="number" id="top50N" value="10" min="1" max="50">
          <button class="top50-btn top50-btn-add" onclick="top50AddToSymbols()">종목 추가</button>
          <button class="top50-btn top50-btn-clr" onclick="top50ClearSel()">해제</button>
          <button class="top50-btn top50-btn-ref" onclick="fetchTop50()" title="새로고침">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:10px;height:10px;"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          </button>
        </div>
        <div class="top50-list" id="top50List">
          <div style="padding:12px 10px;font-size:9px;color:var(--t3);font-family:var(--mono);">로딩 중…</div>
        </div>
      </div>

      <!-- Period chips -->
      <div class="f-group">
        <label class="f-label">기간</label>
        <div class="period-chips">
          <span class="chip" onclick="setPeriod(7,this)">1주</span>
          <span class="chip" onclick="setPeriod(14,this)">2주</span>
          <span class="chip" onclick="setPeriod(30,this)">1개월</span>
          <span class="chip" onclick="setPeriod(90,this)">3개월</span>
        </div>
        <div style="margin-top:6px;font-family:var(--mono);font-size:9px;color:#607090;line-height:1.5;">
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
        <input type="number" class="f-input" id="buyAmount" value="10000000" step="100000" min="10000">
      </div>

      <button class="btn-reset" onclick="resetParams()" style="margin-bottom:10px;">↺ 기본값 초기화</button>

      <!-- ═══ ACCORDION ═══ -->

      <!-- 1. 공통 진입 조건 -->
      <div class="acc-section">
        <div class="acc-header is-open" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--lime);"></div>공통 진입 조건</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body is-open">
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">최소 히스토리 (봉)</label>
              <input type="number" class="f-input" id="p_minHistoryBars" value="30" step="1" min="1">
            </div>
            <div class="f-group">
              <label class="f-label">최소 히스토리 (분)</label>
              <input type="number" class="f-input" id="p_minHistoryMinutes" value="30" step="1" min="1">
            </div>
            <div class="f-group">
              <label class="f-label">최소 가격 (원/USD)</label>
              <input type="number" class="f-input" id="p_minPrice" value="500" step="100" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">VWAP Hard Limit %</label>
              <input type="number" class="f-input" id="p_vwapHardLimitPct" value="8.0" step="0.5" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Turnover KRX Latest (원)</label>
              <input type="number" class="f-input" id="p_minTurnoverKrx" value="50000000" step="1000000" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Turnover KRX Avg (원)</label>
              <input type="number" class="f-input" id="p_minAvgTurnoverKrx" value="30000000" step="1000000" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Turnover US (USD)</label>
              <input type="number" class="f-input" id="p_minTurnoverUs" value="10000" step="1000" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Buy Cooldown (초)</label>
              <input type="number" class="f-input" id="p_buyCooldownSec" value="0" step="30" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Max Daily Entry</label>
              <input type="number" class="f-input" id="p_maxDailyEntryCount" value="2" step="1" min="1">
            </div>
            <div class="f-group" style="grid-column:span 2;">
              <label class="f-label">Max Same Pattern</label>
              <input type="number" class="f-input" id="p_maxSamePatternEntry" value="1" step="1" min="1">
            </div>
          </div>
          <div class="cb-row">
            <input type="checkbox" id="p_useMarketFilter">
            <label class="cb-label" for="p_useMarketFilter">Market Filter 사용 (시장 약세 차단)</label>
          </div>
        </div>
      </div>

      <!-- 2. PULLBACK -->
      <div class="acc-section">
        <div class="acc-header is-open" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--blue);"></div>PULLBACK</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body is-open">
          <div class="mode-enable">
            <input type="checkbox" id="p_enablePullback" checked>
            <label for="p_enablePullback">Enable PULLBACK</label>
          </div>
          <div class="acc-sub">진입 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Min Score ≥</label>
              <input type="number" class="f-input" id="p_pullbackMinScore" value="80" step="1" min="0" max="100">
            </div>
            <div class="f-group">
              <label class="f-label">VWAP Gap %</label>
              <input type="number" class="f-input" id="p_vwapMaxGapPullbackPct" value="1.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Upper % (고점 대비)</label>
              <input type="number" class="f-input" id="p_pullbackUpperPct" value="1.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Lower % (고점 대비)</label>
              <input type="number" class="f-input" id="p_pullbackLowerPct" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Volume Mult ×</label>
              <input type="number" class="f-input" id="p_pullbackVolumeMult" value="1.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Velocity Short</label>
              <input type="number" class="f-input" id="p_pullbackVelocityShort" value="0.0010" step="0.0001" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Velocity Mid</label>
              <input type="number" class="f-input" id="p_pullbackVelocityMid" value="0.0" step="0.0001">
            </div>
            <div class="f-group">
              <label class="f-label">Required Bullish Bars</label>
              <input type="number" class="f-input" id="p_pullbackRequiredBullishBars" value="1" step="1" min="0" max="5">
            </div>
          </div>
          <div class="cb-row"><input type="checkbox" id="p_pullbackRequireAboveVwap" checked><label class="cb-label" for="p_pullbackRequireAboveVwap">Require Above VWAP</label></div>
          <div class="cb-row"><input type="checkbox" id="p_pullbackRequireVwapSlope" checked><label class="cb-label" for="p_pullbackRequireVwapSlope">Require VWAP Slope Up</label></div>
          <div class="cb-row"><input type="checkbox" id="p_pullbackRequireRecentHighBreakout"><label class="cb-label" for="p_pullbackRequireRecentHighBreakout">Require Recent High Breakout</label></div>
          <div class="acc-sub">청산 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Stop Loss %</label>
              <input type="number" class="f-input" id="p_pullbackStopPct" value="2.3" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Take Profit %</label>
              <input type="number" class="f-input" id="p_pullbackTpPct" value="3.2" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Start %</label>
              <input type="number" class="f-input" id="p_pullbackTrailSt" value="2.2" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Drop %</label>
              <input type="number" class="f-input" id="p_pullbackTrailDrop" value="1.6" step="0.1" min="0">
            </div>
          </div>
        </div>
      </div>

      <!-- 3. BREAKOUT -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--lime);"></div>BREAKOUT</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="mode-enable">
            <input type="checkbox" id="p_enableBreakout" checked>
            <label for="p_enableBreakout">Enable BREAKOUT</label>
          </div>
          <div class="acc-sub">진입 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Min Score ≥</label>
              <input type="number" class="f-input" id="p_breakoutMinScore" value="78" step="1" min="0" max="100">
            </div>
            <div class="f-group">
              <label class="f-label">VWAP Gap %</label>
              <input type="number" class="f-input" id="p_vwapMaxGapBreakoutPct" value="2.2" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Retest Lower %</label>
              <input type="number" class="f-input" id="p_breakoutRetestLower" value="1.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Retest Upper %</label>
              <input type="number" class="f-input" id="p_breakoutRetestUpper" value="0.1" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Strong Volume Mult ×</label>
              <input type="number" class="f-input" id="p_breakoutStrongVolMult" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Volume Mult ×</label>
              <input type="number" class="f-input" id="p_volumeMult" value="1.5" step="0.1" min="0">
            </div>
          </div>
          <div class="cb-row"><input type="checkbox" id="p_breakoutRequireAcceleration" checked><label class="cb-label" for="p_breakoutRequireAcceleration">Require Acceleration</label></div>
          <div class="cb-row"><input type="checkbox" id="p_breakoutRequireMultiUptrend" checked><label class="cb-label" for="p_breakoutRequireMultiUptrend">Require Multi Uptrend</label></div>
          <div class="cb-row"><input type="checkbox" id="p_breakoutOverheatBlock" checked><label class="cb-label" for="p_breakoutOverheatBlock">Overheat Block 사용</label></div>
          <div class="acc-sub">청산 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Stop Loss %</label>
              <input type="number" class="f-input" id="p_breakoutStopPct" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Take Profit %</label>
              <input type="number" class="f-input" id="p_breakoutTpPct" value="2.8" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Start %</label>
              <input type="number" class="f-input" id="p_breakoutTrailSt" value="2.3" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Drop %</label>
              <input type="number" class="f-input" id="p_breakoutTrailDrop" value="1.5" step="0.1" min="0">
            </div>
          </div>
        </div>
      </div>

      <!-- 4. EARLY_MOMENTUM -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--purple);"></div>EARLY MOMENTUM</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="mode-enable">
            <input type="checkbox" id="p_enableEarlyMomentum">
            <label for="p_enableEarlyMomentum">Enable EARLY MOMENTUM</label>
          </div>
          <div class="acc-sub">진입 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Min Score ≥</label>
              <input type="number" class="f-input" id="p_emMinScore" value="80" step="1" min="0" max="100">
            </div>
            <div class="f-group">
              <label class="f-label">Velocity</label>
              <input type="number" class="f-input" id="p_emVelocity" value="0.003" step="0.001" min="0">
            </div>
            <div class="f-group" style="grid-column:span 2;">
              <label class="f-label">Volume Mult ×</label>
              <input type="number" class="f-input" id="p_emVolumeMult" value="2.0" step="0.1" min="0">
            </div>
          </div>
          <div class="cb-row"><input type="checkbox" id="p_em3TrendUp" checked><label class="cb-label" for="p_em3TrendUp">3 Trend Up Required</label></div>
          <div class="acc-sub">청산 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Stop Loss %</label>
              <input type="number" class="f-input" id="p_emStopPct" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Take Profit %</label>
              <input type="number" class="f-input" id="p_emTpPct" value="3.0" step="0.1" min="0">
            </div>
          </div>
        </div>
      </div>

      <!-- 5. STRONG_PULLBACK -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--emerald);"></div>STRONG PULLBACK</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="mode-enable">
            <input type="checkbox" id="p_enableStrongPullback" checked>
            <label for="p_enableStrongPullback">Enable STRONG PULLBACK</label>
          </div>
          <div class="acc-sub">진입 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">VWAP Min Above %</label>
              <input type="number" class="f-input" id="p_spVwapMinAbovePct" value="0.2" step="0.05" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Pullback Min %</label>
              <input type="number" class="f-input" id="p_spPullbackMinPct" value="1.2" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Pullback Max %</label>
              <input type="number" class="f-input" id="p_spPullbackMaxPct" value="2.5" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Vol3/Vol10 Max</label>
              <input type="number" class="f-input" id="p_spVol3RatioMax" value="0.6" step="0.05" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Body Ratio Min</label>
              <input type="number" class="f-input" id="p_spBodyRatioMin" value="0.6" step="0.05" min="0">
            </div>
            <div class="f-group" style="grid-column:span 2;">
              <label class="f-label">Min Score ≥</label>
              <input type="number" class="f-input" id="p_spMinScore" value="85" step="1" min="0" max="100">
            </div>
          </div>
          <div class="acc-sub">청산 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Stop Loss %</label>
              <input type="number" class="f-input" id="p_spStopPct" value="1.8" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Take Profit %</label>
              <input type="number" class="f-input" id="p_spTpPct" value="3.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Start %</label>
              <input type="number" class="f-input" id="p_spTrailSt" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Drop %</label>
              <input type="number" class="f-input" id="p_spTrailDrop" value="0.8" step="0.1" min="0">
            </div>
          </div>
        </div>
      </div>

      <!-- 6. VWAP_RECLAIM -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--gold);"></div>VWAP RECLAIM</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="mode-enable">
            <input type="checkbox" id="p_enableVwapReclaim" checked>
            <label for="p_enableVwapReclaim">Enable VWAP RECLAIM</label>
          </div>
          <div class="acc-sub">진입 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Lookback Bars</label>
              <input type="number" class="f-input" id="p_vrLookbackBars" value="5" step="1" min="1">
            </div>
            <div class="f-group">
              <label class="f-label">Volume Mult ×</label>
              <input type="number" class="f-input" id="p_vrVolMult" value="1.8" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Min Above VWAP Bars</label>
              <input type="number" class="f-input" id="p_vrMinAboveVwapBars" value="3" step="1" min="1">
            </div>
            <div class="f-group" style="grid-column:span 2;">
              <label class="f-label">Min Score ≥</label>
              <input type="number" class="f-input" id="p_vrMinScore" value="80" step="1" min="0" max="100">
            </div>
          </div>
          <div class="acc-sub">청산 조건</div>
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Stop Loss %</label>
              <input type="number" class="f-input" id="p_vrStopPct" value="1.5" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Take Profit %</label>
              <input type="number" class="f-input" id="p_vrTpPct" value="2.0" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Start %</label>
              <input type="number" class="f-input" id="p_vrTrailSt" value="1.5" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Trail Drop %</label>
              <input type="number" class="f-input" id="p_vrTrailDrop" value="1.0" step="0.1" min="0">
            </div>
          </div>
        </div>
      </div>

      <!-- 7. 공통 청산 조건 -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:var(--red);"></div>공통 청산 조건</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Emergency Stop %</label>
              <input type="number" class="f-input" id="p_emergencyStopPct" value="5.0" step="0.5" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">VWAP Break Buffer %</label>
              <input type="number" class="f-input" id="p_vwapBreakBuffer" value="0.2" step="0.05" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Breakeven Peak %</label>
              <input type="number" class="f-input" id="p_breakevenPeak" value="1.5" step="0.1" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Breakeven Loss %</label>
              <input type="number" class="f-input" id="p_breakevenLoss" value="-0.3" step="0.05">
            </div>
          </div>
          <div class="cb-row"><input type="checkbox" id="p_useVwapBreak" checked><label class="cb-label" for="p_useVwapBreak">VWAP Break 청산 사용</label></div>
          <div class="cb-row"><input type="checkbox" id="p_useBreakevenGuard" checked><label class="cb-label" for="p_useBreakevenGuard">Breakeven Guard 사용</label></div>
          <div class="cb-row"><input type="checkbox" id="p_useFailedBreakout" checked><label class="cb-label" for="p_useFailedBreakout">Failed Breakout 청산 사용</label></div>
          <div class="cb-row"><input type="checkbox" id="p_useFailedPullback" checked><label class="cb-label" for="p_useFailedPullback">Failed Pullback 청산 사용</label></div>
          <div class="cb-row"><input type="checkbox" id="p_useEodForceSell" checked><label class="cb-label" for="p_useEodForceSell">EOD Force Sell 사용</label></div>
          <div class="cb-row"><input type="checkbox" id="p_blockSGrade" checked><label class="cb-label" for="p_blockSGrade">S등급(90~94) 진입 차단</label></div>
          <div class="cb-row"><input type="checkbox" id="p_blockAGrade"><label class="cb-label" for="p_blockAGrade">A등급(85~89) 진입 차단</label></div>
        </div>
      </div>

      <!-- 7. 비용 조건 -->
      <div class="acc-section">
        <div class="acc-header" onclick="toggleAcc(this)">
          <div class="acc-hdr-l"><div class="acc-dot" style="background:#607090;"></div>비용 조건</div>
          <span class="acc-arrow">▼</span>
        </div>
        <div class="acc-body">
          <div class="p-grid">
            <div class="f-group">
              <label class="f-label">Slippage % (편도)</label>
              <input type="number" class="f-input" id="p_slippagePct" value="0.0" step="0.01" min="0">
            </div>
            <div class="f-group">
              <label class="f-label">Fee % (RT 합산)</label>
              <input type="number" class="f-input" id="p_feePct" value="0.015" step="0.001" min="0">
            </div>
            <div class="f-group" style="grid-column:span 2;">
              <label class="f-label">Tax % (KRX 증권거래세)</label>
              <input type="number" class="f-input" id="p_taxPct" value="0.18" step="0.01" min="0">
            </div>
          </div>
        </div>
      </div>

    </div><!-- sidebar-body -->

    <div class="sidebar-footer">
      <button class="btn btn-run" id="btnRun" onclick="runAll()">▶ 백테스트 실행</button>
      <button class="btn btn-ai" id="btnAiExport" onclick="downloadAiPrompt()" disabled style="margin-top:7px;">AI 분석 프롬프트 다운로드</button>
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
function p2bt(v){return String(v).padStart(2,'0');}
function tickClock() {
  const n = new Date();
  document.getElementById('clkT').textContent = p2bt(n.getHours())+':'+p2bt(n.getMinutes())+':'+p2bt(n.getSeconds());
  document.getElementById('clkD').textContent = n.getFullYear()+'-'+p2bt(n.getMonth()+1)+'-'+p2bt(n.getDate());
}
tickClock(); setInterval(tickClock, 1000);

/* ── Login ── */
(function(){
  var _B='<%= request.getContextPath() %>';
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

/* ── Market ── */
function setMarket(mkt) {
  document.getElementById('marketVal').value = mkt;
  document.getElementById('mktKrx').classList.toggle('active', mkt === 'KRX');
  document.getElementById('mktUs').classList.toggle('active',  mkt === 'US');
  document.getElementById('symbolInput').placeholder = mkt === 'KRX' ? '예) 005930  Enter로 추가' : '예) TSLA  Enter로 추가';
  symbols = []; renderTags();
  document.getElementById('amtLabel').textContent  = mkt === 'KRX' ? '주문금액 (원)' : '주문금액 (USD)';
  hideDropdown();
  // Top50 패널은 KRX에서만 표시
  var panel = document.getElementById('top50Panel');
  if (mkt === 'KRX') { panel.style.display = 'block'; fetchTop50(); }
  else               { panel.style.display = 'none'; }
}

/* ── Top50 Picker ── */
var top50Rows = [];
var top50Sel  = new Set();

function renderTop50() {
  var wrap = document.getElementById('top50List');
  var info = document.getElementById('top50SelInfo');
  info.textContent = '선택 ' + top50Sel.size;
  if (!top50Rows.length) {
    wrap.innerHTML = '<div style="padding:12px 10px;font-size:9px;color:var(--t3);font-family:var(--mono);">데이터 없음</div>';
    return;
  }
  wrap.innerHTML = top50Rows.map(function(r, i) {
    var sym  = String(r.symbol || '');
    var nm   = r.name || sym;
    var rate = Number(r.prdy_ctrt || 0);
    var rc   = i === 0 ? 'r1' : i === 1 ? 'r2' : i === 2 ? 'r3' : '';
    var sel  = top50Sel.has(sym);
    var rateClass = rate > 0 ? 'up' : rate < 0 ? 'dn' : 'fl';
    var rateStr   = (rate > 0 ? '+' : '') + rate.toFixed(2) + '%';
    var price = Number(r.stck_prpr || 0);
    return '<div class="top50-row' + (sel ? ' sel' : '') + '" onclick="top50Toggle(\'' + sym.replace(/'/g,"\\'")+  '\')">'
      + '<div class="top50-chk"></div>'
      + '<span class="top50-rank ' + rc + '">' + (i + 1) + '</span>'
      + '<span class="top50-sym">' + sym + '</span>'
      + '<span class="top50-nm" title="' + nm.replace(/"/g,'&quot;') + '">' + nm + '</span>'
      + '<span class="top50-rate ' + rateClass + '">' + rateStr + '</span>'
      + '</div>';
  }).join('');
}

function top50Toggle(sym) {
  if (!sym) return;
  top50Sel.has(sym) ? top50Sel.delete(sym) : top50Sel.add(sym);
  renderTop50();
}

function fetchTop50() {
  fetch(ctx + '/api/market/ranking?market=KR&exch=KRX')
    .then(function(r) { return r.json(); })
    .then(function(d) {
      top50Rows = (d.data || d.output || []).slice(0, 50);
      // 이전 선택 중 랭킹에서 빠진 종목 자동 해제
      var symsInRank = new Set(top50Rows.map(function(r){ return String(r.symbol||''); }));
      top50Sel.forEach(function(s){ if (!symsInRank.has(s)) top50Sel.delete(s); });
      renderTop50();
    })
    .catch(function() { top50Rows = []; renderTop50(); });
}

function top50ClearSel() {
  top50Sel.clear();
  renderTop50();
}

function top50AddToSymbols() {
  var n = parseInt(document.getElementById('top50N').value, 10) || 10;
  n = Math.min(Math.max(n, 1), 50);
  // 선택된 게 있으면 선택 우선, 없으면 Top N 자동 선택
  var toAdd;
  if (top50Sel.size > 0) {
    toAdd = Array.from(top50Sel);
  } else {
    toAdd = top50Rows.slice(0, n).map(function(r){ return String(r.symbol || ''); });
  }
  toAdd = toAdd.filter(function(s){ return /^\d{5,6}$/.test(s); });
  if (!toAdd.length) { toast('추가할 종목이 없습니다', 'err'); return; }
  toAdd.forEach(function(s){ addSymbol(s); });
  toast(toAdd.length + '개 종목 추가됨', 'ok');
}

// 초기 로드 시 KRX가 기본 선택이므로 바로 패널 표시 및 데이터 fetch
(function() {
  document.getElementById('top50Panel').style.display = 'block';
  fetchTop50();
  setInterval(fetchTop50, 60000);
})();

/* ── Period ── */
function setPeriod(days, el) {
  document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
  const end   = new Date();
  const start = new Date(); start.setDate(start.getDate() - days);
  document.getElementById('endDate').value   = fmtDate(end);
  document.getElementById('startDate').value = fmtDate(start);
}
function fmtDate(d) { return d.toISOString().slice(0, 10); }

(function(){
  var chip3m = document.querySelector('.period-chips .chip:last-child');
  setPeriod(90, chip3m);
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
  addSymbol(sym);
  document.getElementById('symbolInput').value = '';
  hideDropdown();
}
document.addEventListener('click', function(e) {
  if (!e.target.closest('.sym-wrap')) hideDropdown();
});

/* ── Params ── */
const PARAM_DEFAULTS = {
  // 공통 진입
  minHistoryBars:30, minHistoryMinutes:30, minPrice:500, vwapHardLimitPct:8.0,
  minTurnoverKrx:50000000, minAvgTurnoverKrx:30000000, minTurnoverUs:10000,
  useMarketFilter:false, buyCooldownSec:0, maxDailyEntryCount:2, maxSamePatternEntry:1,
  // PULLBACK 활성
  enablePullback:true,
  // PULLBACK 진입
  pullbackMinScore:80, vwapMaxGapPullbackPct:1.0,
  pullbackUpperPct:1.0, pullbackLowerPct:2.0,
  pullbackVolumeMult:1.0, pullbackVelocityShort:0.0010, pullbackVelocityMid:0.0,
  pullbackRequiredBullishBars:1,
  pullbackRequireAboveVwap:true, pullbackRequireVwapSlope:true, pullbackRequireRecentHighBreakout:false,
  // PULLBACK 청산
  pullbackStopPct:2.3, pullbackTpPct:3.2, pullbackTrailSt:2.2, pullbackTrailDrop:1.6,
  // BREAKOUT 활성
  enableBreakout:true,
  // BREAKOUT 진입
  breakoutMinScore:78, vwapMaxGapBreakoutPct:2.2,
  breakoutRetestLower:1.0, breakoutRetestUpper:0.1, breakoutStrongVolMult:2.0, breakoutKrxVolMult:1.8, volumeMult:1.5,
  breakoutRequireAcceleration:true, breakoutRequireMultiUptrend:true, breakoutOverheatBlock:true,
  // BREAKOUT 청산
  breakoutStopPct:2.0, breakoutTpPct:2.8, breakoutTrailSt:2.3, breakoutTrailDrop:1.5,
  // EARLY_MOMENTUM 활성
  enableEarlyMomentum:false,
  // EARLY_MOMENTUM 진입
  emMinScore:80, emVelocity:0.003, emVolumeMult:2.0, em3TrendUp:true,
  // EARLY_MOMENTUM 청산
  emStopPct:2.0, emTpPct:3.0,
  // STRONG_PULLBACK 활성
  enableStrongPullback:true,
  // STRONG_PULLBACK 진입
  spPullbackMinPct:1.2, spPullbackMaxPct:2.5,
  spVwapMinAbovePct:0.2, spVol3RatioMax:0.6, spBodyRatioMin:0.6, spMinScore:85,
  // STRONG_PULLBACK 청산
  spStopPct:1.8, spTpPct:3.0, spTrailSt:2.0, spTrailDrop:0.8,
  // VWAP_RECLAIM 활성
  enableVwapReclaim:true,
  // VWAP_RECLAIM 진입
  vrLookbackBars:5, vrVolMult:1.8, vrMinAboveVwapBars:3, vrMinScore:80,
  // VWAP_RECLAIM 청산
  vrStopPct:1.5, vrTpPct:2.0, vrTrailSt:1.5, vrTrailDrop:1.0,
  // 공통 청산
  emergencyStopPct:5.5, useVwapBreak:true, vwapBreakBuffer:0.5,
  useBreakevenGuard:true, breakevenPeak:1.5, breakevenLoss:-0.3,
  useFailedBreakout:true, useFailedPullback:true,
  useEodForceSell:true,
  // 진입 등급 필터
  blockSGrade:true, blockAGrade:false,
  // 비용
  slippagePct:0.0, feePct:0.015, taxPct:0.18
};
function resetParams() {
  Object.entries(PARAM_DEFAULTS).forEach(function(kv) {
    var el = document.getElementById('p_' + kv[0]);
    if (!el) return;
    if (el.type === 'checkbox') el.checked = !!kv[1];
    else el.value = kv[1];
  });
  toast('파라미터 초기화 완료', 'ok');
}
function collectParamValues() {
  var p = {};
  Object.keys(PARAM_DEFAULTS).forEach(function(k) {
    var el = document.getElementById('p_' + k);
    if (!el) return;
    if (el.type === 'checkbox') p[k] = el.checked ? 'true' : 'false';
    else p[k] = el.value;
  });
  return p;
}
function toggleAcc(hdr) {
  hdr.classList.toggle('is-open');
  var body = hdr.nextElementSibling;
  body.classList.toggle('is-open');
}

/* ── Multi-symbol tag management ── */
var symbols = [];
function addCurrentSymbol() {
  var v = document.getElementById('symbolInput').value.trim().toUpperCase();
  if (!v) return;
  addSymbol(v);
  document.getElementById('symbolInput').value = '';
  hideDropdown();
}
function addSymbol(sym) {
  sym = sym.trim().toUpperCase();
  if (!sym || symbols.indexOf(sym) >= 0) return;
  symbols.push(sym);
  renderTags();
}
function removeSymbol(idx) {
  symbols.splice(idx, 1);
  renderTags();
}
function renderTags() {
  var wrap = document.getElementById('symTags');
  var cnt  = document.getElementById('symCount');
  if (!symbols.length) { wrap.innerHTML = ''; cnt.textContent = ''; return; }
  cnt.textContent = symbols.length + '개';
  wrap.innerHTML = symbols.map(function(s, i) {
    return '<span class="sym-tag">' + s +
      '<span class="sym-tag-x" onclick="removeSymbol(' + i + ')">&#215;</span></span>';
  }).join('');
}
function getSymbolList() {
  var typed = document.getElementById('symbolInput').value.trim().toUpperCase();
  var all = symbols.slice();
  if (typed && all.indexOf(typed) < 0) all.push(typed);
  return all;
}

/* ── Build request ── */
function buildSymbolReq(sym) {
  var start  = document.getElementById('startDate').value;
  var end    = document.getElementById('endDate').value;
  var market = document.getElementById('marketVal').value;
  var buyAmt = parseFloat(document.getElementById('buyAmount').value) || 10000000;
  return Object.assign({ market: market, symbol: sym, startDate: start, endDate: end, buyAmount: String(buyAmt) }, collectParamValues());
}
function buildReq() {
  var syms  = getSymbolList();
  var start = document.getElementById('startDate').value;
  var end   = document.getElementById('endDate').value;
  if (!syms.length) { toast('종목코드를 입력하세요.', 'err'); return null; }
  if (!start || !end) { toast('기간을 선택하세요.', 'err'); return null; }
  return syms;
}

/* ── Collect (sequential per symbol) ── */
function collectBars() {
  var syms = buildReq();
  if (!syms) return;
  setButtons(true);
  var idx = 0;
  function collectNext() {
    if (idx >= syms.length) {
      setButtons(false); hideProgress();
      toast('전체 수집 완료 (' + syms.length + '개 종목)', 'ok');
      return;
    }
    var sym = syms[idx++];
    showProgress(0, '[' + idx + '/' + syms.length + '] ' + sym + ' 수집 중...');
    fetch(ctx + '/backtest/collectBars', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify(buildSymbolReq(sym))
    })
    .then(function(r) { return r.json(); })
    .then(function(d) {
      if (d.status !== 'STARTED') {
        toast(sym + ' 오류: ' + (d.message || ''), 'err');
        collectNext();
        return;
      }
      pollCollectOne(d.jobId, sym, collectNext);
    })
    .catch(function(e) { toast(sym + ' 실패: ' + e.message, 'err'); collectNext(); });
  }
  collectNext();
}
function pollCollectOne(jobId, sym, done) {
  clearInterval(pollTimer);
  pollTimer = setInterval(function() {
    fetch(ctx + '/backtest/collectStatus/' + jobId)
      .then(function(r) { return r.json(); })
      .then(function(s) {
        showProgress(s.progress || 0, sym + ' — ' + (s.message || ''));
        if (s.state === 'DONE' || s.state === 'ERROR') {
          clearInterval(pollTimer);
          if (s.state === 'DONE') toast(sym + ' ' + (s.inserted || 0) + '봉 수집', 'ok');
          else toast(sym + ' 수집 오류: ' + (s.message || ''), 'err');
          done();
        }
      });
  }, 2000);
}

/* ── Backtest (sequential, aggregate) ── */
var lastResults = [];
function runBacktest() {
  var syms = buildReq();
  if (!syms) return;
  setButtons(true);
  lastResults = [];
  document.getElementById('btContent').innerHTML =
    '<div class="empty-state"><div class="empty-icon">⏳</div>' +
    '<div class="empty-title">백테스트 실행 중...</div>' +
    '<div class="empty-sub">' + syms.length + '개 종목 순차 시뮬레이션 중...</div></div>';

  var idx = 0;
  function runNext() {
    if (idx >= syms.length) {
      setButtons(false); hideProgress();
      if (lastResults.length) {
        renderMultiResult(lastResults);
        document.getElementById('btnAiExport').disabled = false;
      } else {
        renderEmpty('결과 없음');
        document.getElementById('btnAiExport').disabled = true;
      }
      return;
    }
    var sym = syms[idx++];
    showProgress(Math.round((idx-1)/syms.length*100), '[' + idx + '/' + syms.length + '] ' + sym);
    fetch(ctx + '/backtest/run', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify(buildSymbolReq(sym))
    })
    .then(function(r) { return r.json(); })
    .then(function(d) {
      if (d.status === 'OK') lastResults.push(d);
      else toast(sym + ' 오류: ' + (d.message || ''), 'err');
      runNext();
    })
    .catch(function(e) { toast(sym + ' 실패: ' + e.message, 'err'); runNext(); });
  }
  runNext();
}

/* ── Render Result ── */
var allTrades = [], tradePage = 1;
const PAGE_SIZE = 25;

function computeExtStats(trades, buyAmt) {
  buyAmt = buyAmt || 0;
  var wins   = trades.filter(function(t) { return t.pnlPct > 0; });
  var losses = trades.filter(function(t) { return t.pnlPct <= 0; });
  var avgWin  = wins.length   > 0 ? wins.reduce(function(s,t)   { return s+t.pnlPct; }, 0) / wins.length   : 0;
  var avgLoss = losses.length > 0 ? losses.reduce(function(s,t) { return s+t.pnlPct; }, 0) / losses.length : 0;
  var rr = (avgLoss !== 0) ? Math.abs(avgWin / avgLoss) : (wins.length > 0 ? 999 : 0);
  var maxW=0, maxL=0, curW=0, curL=0;
  trades.forEach(function(t) {
    if (t.pnlPct > 0) { curW++; curL=0; if (curW>maxW) maxW=curW; }
    else               { curL++; curW=0; if (curL>maxL) maxL=curL; }
  });
  var netPnlAmt  = trades.reduce(function(s,t) { return s + t.pnlPct * (t.actualEntryAmt > 0 ? t.actualEntryAmt : buyAmt); }, 0);
  var winPnlAmt  = wins.reduce(function(s,t)   { return s + t.pnlPct * (t.actualEntryAmt > 0 ? t.actualEntryAmt : buyAmt); }, 0);
  var lossPnlAmt = losses.reduce(function(s,t) { return s + t.pnlPct * (t.actualEntryAmt > 0 ? t.actualEntryAmt : buyAmt); }, 0);
  return { avgWin:avgWin, avgLoss:avgLoss, rr:rr, maxConsecWin:maxW, maxConsecLoss:maxL,
           netPnlAmt:netPnlAmt, winPnlAmt:winPnlAmt, lossPnlAmt:lossPnlAmt };
}

function computeModeExtStats(trades) {
  var byMode = {};
  trades.forEach(function(t) {
    var m = t.entryMode || 'UNKNOWN';
    if (!byMode[m]) byMode[m] = [];
    byMode[m].push(t);
  });
  var result = {};
  Object.keys(byMode).forEach(function(m) {
    result[m] = computeExtStats(byMode[m]);
  });
  return result;
}

function fmtAmt(v, market) {
  var abs = Math.abs(Math.round(v));
  var sign = v >= 0 ? '+' : '-';
  if (market === 'US') return sign + '$' + abs.toLocaleString('en-US');
  return sign + abs.toLocaleString('ko-KR') + '원';
}

function renderKpisAmt(ext, market) {
  var net = fmtAmt(ext.netPnlAmt, market);
  var win = fmtAmt(ext.winPnlAmt, market);
  var los = fmtAmt(ext.lossPnlAmt, market);
  var netCls = ext.netPnlAmt >= 0 ? 'pos' : 'neg';
  var netColor = ext.netPnlAmt >= 0 ? 'emerald' : 'red';
  return '<div class="kpi-grid" style="grid-template-columns:repeat(3,1fr);margin-bottom:8px;">' +
    kpiCard2(netColor, '순 손익금', net, netCls, '주문금액 기준 순 수익·손실 합산') +
    kpiCard2('emerald', '총 수익금', win, 'pos', '수익 발생 거래 합산') +
    kpiCard2('red', '총 손실금', los, 'neg', '손실 발생 거래 합산') +
    '</div>';
}

/* ── Multi-symbol aggregate render ── */
function renderMultiResult(results) {
  if (results.length === 1) { renderResult(results[0]); return; }
  // Aggregate all trades
  allTrades = [];
  results.forEach(function(d) { allTrades = allTrades.concat(d.trades || []); });
  // Sort by entryTime
  allTrades.sort(function(a,b) { return (a.entryTime||'').localeCompare(b.entryTime||''); });
  tradePage = 1;
  var buyAmt = parseFloat(document.getElementById('buyAmount').value) || 10000000;
  var market = document.getElementById('marketVal').value || 'KRX';
  var ext = computeExtStats(allTrades, buyAmt);
  var modeExt = computeModeExtStats(allTrades);
  // Build synthetic combined result for KPI block
  var combined = buildCombinedResult(results, allTrades);
  document.getElementById('btContent').innerHTML =
    renderMultiMeta(results) +
    renderSymbolSummaryTable(results) +
    renderKpis(combined, ext) + renderKpisAmt(ext, market) + renderKpis2(ext) + renderChart() +
    renderStatRow(combined.modeStats || [], combined.exitStats || [], combined.rejectReasonSummary || [], modeExt) +
    renderScoreGradeRow(combined.scoreRangeStats || [], combined.gradeStats || []) +
    renderTradesCard();
  drawEquityChart(allTrades);
  renderTradeList();
}

function buildCombinedResult(results, allTrades) {
  // Aggregate server-side stats (mode/exit/score/grade) by merging lists
  function mergeStatLists(lists) {
    var map = {};
    lists.forEach(function(list) {
      (list||[]).forEach(function(item) {
        var k = item.label;
        if (!map[k]) map[k] = { label:k, count:0, pnlSum:0, winSum:0, lossSum:0 };
        map[k].count += (item.count || 0);
        // We'll recompute from allTrades below, just pass through counts for now
      });
    });
    return Object.values(map);
  }
  var total = allTrades.length, wins = allTrades.filter(function(t){return t.pnlPct>0;}).length;
  var sumPnl = allTrades.reduce(function(s,t){return s+t.pnlPct;},0);
  var sumWin = allTrades.filter(function(t){return t.pnlPct>0;}).reduce(function(s,t){return s+t.pnlPct;},0);
  var sumLoss = allTrades.filter(function(t){return t.pnlPct<=0;}).reduce(function(s,t){return s+Math.abs(t.pnlPct);},0);
  var cumPnl = allTrades.reduce(function(s,t){return s*(1+t.pnlPct);},1.0);
  var peak=1.0, maxDD=0.0, cur=1.0;
  allTrades.forEach(function(t){ cur*=(1+t.pnlPct); if(cur>peak)peak=cur; var dd=(peak-cur)/peak; if(dd>maxDD)maxDD=dd; });
  // Merge mode/exit/score/grade from all results
  var allModeStats = []; results.forEach(function(d){allModeStats.push(d.modeStats||[]);});
  var allExitStats = []; results.forEach(function(d){allExitStats.push(d.exitStats||[]);});
  var allRejects  = []; results.forEach(function(d){allRejects.push(d.rejectReasonSummary||[]);});
  return {
    status:'OK',
    market: results[0].market,
    symbol: results.map(function(d){return d.symbol;}).join(', '),
    startDate: results[0].startDate,
    endDate: results[0].endDate,
    totalBars: results.reduce(function(s,d){return s+(d.totalBars||0);},0),
    buyAmount: results[0].buyAmount,
    totalTrades: total, wins: wins,
    winRate: total>0 ? wins/total : 0,
    avgPnlPct: total>0 ? sumPnl/total : 0,
    cumulativePnlPct: cumPnl-1.0,
    maxDrawdown: maxDD,
    profitFactor: sumLoss>0 ? sumWin/sumLoss : (sumWin>0?9999:0),
    expectancy: (function(){ var l=total-wins, wr=total>0?wins/total:0; return wr*(wins>0?sumWin/wins:0)-(1-wr)*(l>0?sumLoss/l:0); }()),
    warnings: results[0].warnings || [],
    proxyReplayEnabled: results[0].proxyReplayEnabled,
    modeStats: mergeStatsFromTrades(allTrades, 'entryMode'),
    exitStats: mergeStatsFromTrades(allTrades, 'exitReason', true),
    rejectReasonSummary: mergeRejects(results),
    scoreRangeStats: buildScoreRangeStats(allTrades),
    gradeStats: buildGradeStats(allTrades),
    trades: allTrades
  };
}

function mergeStatsFromTrades(trades, key, groupByExitGroup) {
  var map = {};
  trades.forEach(function(t) {
    var k = groupByExitGroup ? exitGroupJs(t[key]) : (t[key]||'UNKNOWN');
    if (!map[k]) map[k] = [];
    map[k].push(t.pnlPct);
  });
  return Object.keys(map).map(function(k) {
    var pnls = map[k], cnt=pnls.length, w=pnls.filter(function(p){return p>0;}).length;
    var s=pnls.reduce(function(a,p){return a+p;},0);
    var win=pnls.filter(function(p){return p>0;}).reduce(function(a,p){return a+p;},0);
    var loss=pnls.filter(function(p){return p<=0;}).reduce(function(a,p){return a+Math.abs(p);},0);
    var wr=cnt>0?w/cnt:0, l=cnt-w, avgW=w>0?win/w:0, avgL=l>0?loss/l:0;
    var exp=wr*avgW-(1-wr)*avgL;
    var cum2=1.0,peak2=1.0,mdd2=0.0; pnls.forEach(function(p){cum2*=(1+p);if(cum2>peak2)peak2=cum2;var dd=(peak2-cum2)/peak2;if(dd>mdd2)mdd2=dd;});
    return {label:k,count:cnt,winRate:wr,avgPnlPct:cnt>0?s/cnt:0,
            cumulativePnlPct:pnls.reduce(function(a,p){return a+Math.log1p(p);},0),
            profitFactor:loss>0?win/loss:(win>0?9999:0),
            expectancy:exp,mdd:mdd2};
  });
}
function exitGroupJs(r) {
  if (!r) return 'UNKNOWN';
  if (r.startsWith('TAKE_PROFIT')) return 'TAKE_PROFIT';
  if (r.startsWith('TRAIL')||r==='BREAKEVEN_GUARD') return 'TRAIL';
  if (r.startsWith('STOP_LOSS')||r==='EMERGENCY_STOP'||r==='FAILED_BREAKOUT'||
      r==='FAILED_PULLBACK'||r==='EARLY_MOMENTUM_DEAD'||r==='VWAP_BREAK') return 'STOPLOSS';
  if (r.startsWith('TIME_STOP')||r==='EOD_FORCE_SELL') return 'TIME_STOP';
  if (r==='BACKTEST_END') return 'BACKTEST_END';
  return r;
}
function mergeRejects(results) {
  var map = {};
  results.forEach(function(d) {
    (d.rejectReasonSummary||[]).forEach(function(item) {
      map[item.reason] = (map[item.reason]||0) + (item.count||0);
    });
  });
  return Object.keys(map).sort(function(a,b){return map[b]-map[a];}).slice(0,10).map(function(k){return {reason:k,count:map[k]};});
}
function buildScoreRangeStats(trades) {
  var ranges = [['95-100',95,100],['90-94',90,94],['85-89',85,89],['80-84',80,84],['75-79',75,79],['<75',0,74]];
  return ranges.map(function(sr) {
    var pnls = trades.filter(function(t){var s=t.signalScore||0;return s>=sr[1]&&s<=sr[2];}).map(function(t){return t.pnlPct;});
    var cnt=pnls.length,w=pnls.filter(function(p){return p>0;}).length;
    var s=pnls.reduce(function(a,p){return a+p;},0);
    var win=pnls.filter(function(p){return p>0;}).reduce(function(a,p){return a+p;},0);
    var loss=pnls.filter(function(p){return p<=0;}).reduce(function(a,p){return a+Math.abs(p);},0);
    return {label:sr[0],count:cnt,winRate:cnt>0?w/cnt:0,avgPnlPct:cnt>0?s/cnt:0,profitFactor:loss>0?win/loss:(win>0?9999:0)};
  });
}
function buildGradeStats(trades) {
  return ['SS','S','A','B','C','D'].map(function(g) {
    var pnls = trades.filter(function(t){return t.signalGrade===g;}).map(function(t){return t.pnlPct;});
    var cnt=pnls.length,w=pnls.filter(function(p){return p>0;}).length;
    var s=pnls.reduce(function(a,p){return a+p;},0);
    var win=pnls.filter(function(p){return p>0;}).reduce(function(a,p){return a+p;},0);
    var loss=pnls.filter(function(p){return p<=0;}).reduce(function(a,p){return a+Math.abs(p);},0);
    return {label:g,count:cnt,winRate:cnt>0?w/cnt:0,avgPnlPct:cnt>0?s/cnt:0,profitFactor:loss>0?win/loss:(win>0?9999:0)};
  });
}

function renderMultiMeta(results) {
  var mkt = results[0].market === 'KRX' ? '🇰🇷 KRX' : '🇺🇸 US';
  var syms = results.map(function(d){return d.symbol;}).join(' · ');
  return '<div class="results-meta">' + mkt + ' · <span>' + syms + '</span>' +
    ' · ' + results[0].startDate + ' ~ ' + results[0].endDate + '</div>';
}

function renderSymbolSummaryTable(results) {
  var hdr = '<tr><th>종목</th><th class="r">거래수</th><th class="r">승률</th>' +
    '<th class="r">평균손익</th><th class="r">누적손익</th><th class="r">PF</th><th class="r">MDD</th></tr>';
  var body = results.map(function(d) {
    var wr  = (d.winRate*100).toFixed(1)+'%';
    var ap  = ((d.avgPnlPct||0)*100).toFixed(2)+'%';
    var cum = ((d.cumulativePnlPct||0)*100).toFixed(1)+'%';
    var pf  = (d.profitFactor||0)>=9999 ? '∞' : (d.profitFactor||0).toFixed(2);
    var mdd = ((d.maxDrawdown||0)*100).toFixed(1)+'%';
    var cls = (d.avgPnlPct||0)>=0?'pos':'neg';
    return '<tr><td style="font-weight:700;color:var(--lime)">' + d.symbol + '</td>' +
      '<td class="r">' + (d.totalTrades||0) + '</td>' +
      '<td class="r">' + wr + '</td>' +
      '<td class="r ' + cls + '">' + ap + '</td>' +
      '<td class="r ' + cls + '">' + cum + '</td>' +
      '<td class="r">' + pf + '</td>' +
      '<td class="r neg">' + mdd + '</td></tr>';
  }).join('');
  return '<div class="stat-card" style="margin-bottom:12px;"><div class="stat-card-title">종목별 요약</div>' +
    '<table class="tbl"><thead>' + hdr + '</thead><tbody>' + body + '</tbody></table></div>';
}

function renderResult(d) {
  allTrades = d.trades || [];
  tradePage = 1;
  var buyAmt = parseFloat(document.getElementById('buyAmount').value) || 10000000;
  var market = document.getElementById('marketVal').value || 'KRX';
  var ext = computeExtStats(allTrades, buyAmt);
  var modeExt = computeModeExtStats(allTrades);
  document.getElementById('btContent').innerHTML =
    renderMeta(d) + renderWarnings(d.warnings || []) + renderKpis(d, ext) + renderKpisAmt(ext, market) + renderKpis2(ext) + renderChart() +
    renderStatRow(d.modeStats || [], d.exitStats || [], d.rejectReasonSummary || [], modeExt) +
    renderScoreGradeRow(d.scoreRangeStats || [], d.gradeStats || []) +
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

function renderKpis(d, ext) {
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
    kpiCard('lime', '총 거래수', d.totalTrades + '<span style="font-size:13px;color:#a0aabb"> 건</span>', 'neu',
      '<b>' + d.wins + '</b>W / <b>' + (d.totalTrades - d.wins) + '</b>L') +
    kpiCard(d.winRate >= 0.5 ? 'emerald' : 'red', '승률', wr + '%', wrCls,
      '기대값 <b style="color:' + (d.expectancy >= 0 ? 'var(--emerald)' : 'var(--red)') + '">' + exp + '%</b>') +
    kpiCard(d.avgPnlPct >= 0 ? 'emerald' : 'red', '평균 수익률', avg + '%', avgCls,
      '누적 <b class="' + cumCls + '">' + cum + '%</b>') +
    kpiCard('gold', 'Max Drawdown', '-' + dd + '%', 'neg',
      'Profit Factor <b style="color:var(--gold)">' + pf + '</b>') +
    '</div>';
}

function renderKpis2(ext) {
  var aw  = ext.avgWin  ? (ext.avgWin  * 100).toFixed(2) : '0.00';
  var al  = ext.avgLoss ? (ext.avgLoss * 100).toFixed(2) : '0.00';
  var rr  = ext.rr >= 999 ? '∞' : ext.rr.toFixed(2);
  var mw  = ext.maxConsecWin;
  var ml  = ext.maxConsecLoss;
  return '<div class="kpi-grid-2">' +
    kpiCard2('emerald', '평균 승리', '+' + aw + '%', 'pos', '수익 발생 거래 평균') +
    kpiCard2('red',     '평균 손실',  al + '%', 'neg', '손실 발생 거래 평균') +
    kpiCard2('blue',    '손익비 (R:R)', rr + 'x', 'neu', '|평균승리| / |평균손실|') +
    kpiCard2('lime',    '최대 연속 승', mw + '연승', 'pos', '최대 연속 손실: ' + ml + '연패') +
    kpiCard2('gold',    '최대 연속 패', ml + '연패', 'neg', '최대 연속 승리: ' + mw + '연승') +
    '</div>';
}

function kpiCard2(color, label, value, valueCls, sub) {
  return '<div class="kpi-card ' + color + '">' +
    '<div class="kpi-label">' + label + '</div>' +
    '<div class="kpi-val ' + valueCls + '" style="font-size:18px">' + value + '</div>' +
    '<div class="kpi-sub">' + sub + '</div>' +
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

function renderStatRow(modes, exits, rejects, modeExt) {
  return '<div class="stat-row-3">' + renderModeStats(modes, modeExt) + renderExitStats(exits) + renderRejectSummary(rejects) + '</div>';
}
function renderScoreGradeRow(scoreRanges, grades) {
  return '<div class="stat-row-3">' + renderScoreRangeStats(scoreRanges) + renderGradeStats(grades) + '</div>';
}
function renderScoreRangeStats(rows) {
  if (!rows || !rows.length) return '';
  var hdr = '<tr><th>범위</th><th class="r">건수</th><th class="r">승률</th><th class="r">평균손익</th><th class="r">PF</th></tr>';
  var body = rows.map(function(r) {
    var wr = (r.winRate*100).toFixed(1)+'%';
    var ap = ((r.avgPnlPct||0)*100).toFixed(2)+'%';
    var pf = (r.profitFactor||0) >= 9999 ? '∞' : (r.profitFactor||0).toFixed(2);
    var cls = (r.avgPnlPct||0) >= 0 ? 'pos' : 'neg';
    return '<tr><td style="font-weight:600">' + r.label + '</td>' +
      '<td class="r">' + (r.count||0) + '</td>' +
      '<td class="r">' + wr + '</td>' +
      '<td class="r ' + cls + '">' + ap + '</td>' +
      '<td class="r">' + pf + '</td></tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-card-title">Score 범위별 성과</div>' +
    '<table class="tbl"><thead>' + hdr + '</thead><tbody>' + body + '</tbody></table></div>';
}
function renderGradeStats(rows) {
  if (!rows || !rows.length) return '';
  const gradeColor = {SS:'#f59e0b',S:'#10b981',A:'#3b82f6',B:'#6366f1',C:'#8b5cf6',D:'#6b7280'};
  var hdr = '<tr><th>등급</th><th class="r">건수</th><th class="r">승률</th><th class="r">평균손익</th><th class="r">PF</th></tr>';
  var body = rows.map(function(r) {
    var wr = (r.winRate*100).toFixed(1)+'%';
    var ap = ((r.avgPnlPct||0)*100).toFixed(2)+'%';
    var pf = (r.profitFactor||0) >= 9999 ? '∞' : (r.profitFactor||0).toFixed(2);
    var cls = (r.avgPnlPct||0) >= 0 ? 'pos' : 'neg';
    var col = gradeColor[r.label] || '#6b7280';
    return '<tr><td style="font-weight:700;color:' + col + '">' + r.label + '</td>' +
      '<td class="r">' + (r.count||0) + '</td>' +
      '<td class="r">' + wr + '</td>' +
      '<td class="r ' + cls + '">' + ap + '</td>' +
      '<td class="r">' + pf + '</td></tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-card-title">Grade별 성과</div>' +
    '<table class="tbl"><thead>' + hdr + '</thead><tbody>' + body + '</tbody></table></div>';
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
    'SP_COND_FAIL':            'STRONG_PULLBACK 조건 불충족',
    'VR_COND_FAIL':            'VWAP_RECLAIM 조건 불충족',
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
  if (!rejects || !rejects.length) return '<div class="stat-card"><div class="stat-title">거절 사유 분석</div><div style="font-family:var(--mono);font-size:10px;color:#607090;padding:10px 0;">데이터 없음</div></div>';
  var total = rejects.reduce(function(s, r) { return s + r.count; }, 0);
  var rows = rejects.map(function(r) {
    var pct  = total > 0 ? (r.count / total * 100).toFixed(1) : '0.0';
    var barW = total > 0 ? Math.round(r.count / total * 100) : 0;
    return '<tr>' +
      '<td style="font-size:10px">' + rejectLabel(r.reason) + '</td>' +
      '<td class="r" style="font-size:10px;color:#c0ccdc">' + r.count + '</td>' +
      '<td class="r" style="font-size:10px;color:#8090a8">' + pct + '%</td>' +
      '<td style="width:60px;padding-left:6px"><div style="height:4px;background:var(--t4);border-radius:2px">' +
        '<div style="height:100%;width:' + barW + '%;background:var(--gold);border-radius:2px"></div></div></td>' +
      '</tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-title">거절 사유 분석 <span style="font-size:8px;color:#8090a8;font-weight:400;letter-spacing:0">' + total + '봉</span></div>' +
    '<div class="trade-scroll">' +
    '<table class="tbl"><thead><tr><th>사유</th><th class="r">횟수</th><th class="r">비율</th><th></th></tr></thead>' +
    '<tbody>' + rows + '</tbody></table></div></div>';
}

function renderModeStats(modes, modeExt) {
  modeExt = modeExt || {};
  if (!modes.length) return '<div class="stat-card"><div class="stat-title">모드별 성과</div><div style="font-family:var(--mono);font-size:10px;color:#607090;padding:10px 0;">데이터 없음</div></div>';
  const rows = modes.map(function(m) {
    const w   = (m.winRate * 100).toFixed(0);
    const a   = (m.avgPnlPct * 100).toFixed(2);
    var ext   = modeExt[m.label] || {};
    var aw    = ext.avgWin  ? '+' + (ext.avgWin  * 100).toFixed(2) + '%' : '-';
    var al    = ext.avgLoss ? (ext.avgLoss * 100).toFixed(2) + '%' : '-';
    var rr    = (ext.rr != null) ? (ext.rr >= 999 ? '∞' : ext.rr.toFixed(1)) : '-';
    var pfV   = m.profitFactor != null ? (m.profitFactor >= 9998 ? '∞' : m.profitFactor.toFixed(2)) : '-';
    var expV  = m.expectancy  != null ? ((m.expectancy * 100).toFixed(2) + '%') : '-';
    var expCls= m.expectancy  != null && m.expectancy >= 0 ? 'pos' : 'neg';
    var mddV  = m.mdd != null ? ((m.mdd * 100).toFixed(1) + '%') : '-';
    return '<tr><td>' + modeBadge(m.label) + '</td>' +
      '<td class="r">' + m.count + '</td>' +
      '<td class="r ' + (m.winRate >= 0.5 ? 'pos' : 'neg') + '">' + w + '%</td>' +
      '<td class="r ' + (m.avgPnlPct >= 0 ? 'pos' : 'neg') + '">' + a + '%</td>' +
      '<td class="r pos">' + aw + '</td>' +
      '<td class="r neg">' + al + '</td>' +
      '<td class="r" style="color:var(--blue)">' + rr + 'x</td>' +
      '<td class="r" style="color:var(--gold)">' + pfV + '</td>' +
      '<td class="r ' + expCls + '">' + expV + '</td>' +
      '<td class="r neg">' + mddV + '</td></tr>';
  }).join('');
  return '<div class="stat-card"><div class="stat-title">모드별 성과</div>' +
    '<table class="tbl"><thead><tr><th>모드</th><th class="r">횟수</th><th class="r">승률</th><th class="r">평균</th>' +
    '<th class="r">평균승</th><th class="r">평균패</th><th class="r">R:R</th>' +
    '<th class="r">PF</th><th class="r">기대값</th><th class="r">MDD</th></tr></thead>' +
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
      var symCell = t.symbol ? '<td style="font-family:var(--mono);font-size:10px;font-weight:700;color:var(--lime)">' + t.symbol + '</td>' : '';
      return '<tr>' +
        '<td style="color:var(--t3)">' + (start+i+1) + '</td>' +
        symCell +
        '<td style="color:var(--t2);white-space:nowrap">' + fmtTs(t.entryTime) + '</td>' +
        '<td style="color:var(--t2);white-space:nowrap">' + fmtTs(t.exitTime) + '</td>' +
        '<td>' + modeBadge(t.entryMode) + '</td>' +
        '<td class="r">' + fmtPrice(t.entryPrice) + '</td>' +
        '<td class="r">' + fmtPrice(t.exitPrice) + '</td>' +
        '<td class="r ' + cls + '">' + (p >= 0 ? '+' : '') + (p*100).toFixed(2) + '%</td>' +
        '<td class="r" style="color:var(--t2)">' + (t.signalScore || '-') + ' ' + gradeBadge(t.signalGrade) + '</td>' +
        '<td>' + exitBadge(t.exitReason) + '</td>' +
        '<td class="r" style="color:var(--t3)">' + fmtHold(t.holdSeconds) + '</td>' +
        '</tr>';
    }).join('');
    var hasSymCol = allTrades.length > 0 && allTrades[0].symbol;
    var symTh = hasSymCol ? '<th>종목</th>' : '';
    wrap.innerHTML = '<table class="tbl">' +
      '<thead><tr><th>#</th>' + symTh + '<th>진입</th><th>청산</th><th>모드</th>' +
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
  const map = {
    PULLBACK:'mb-pullback', BREAKOUT:'mb-breakout', EARLY_MOMENTUM:'mb-early',
    STRONG_PULLBACK:'mb-sp', VWAP_RECLAIM:'mb-vr'
  };
  const lbl = {
    PULLBACK:'PULLBACK', BREAKOUT:'BREAKOUT', EARLY_MOMENTUM:'EARLY MOM',
    STRONG_PULLBACK:'STR-PB', VWAP_RECLAIM:'VWP-RCLM'
  };
  return '<span class="mode-badge ' + (map[m]||'mb-unknown') + '">' + (lbl[m]||m||'?') + '</span>';
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
function gradeBadge(g) {
  if (!g) return '';
  const col = {SS:'#f59e0b',S:'#10b981',A:'#3b82f6',B:'#6366f1',C:'#8b5cf6',D:'#6b7280'};
  return '<span style="font-size:10px;font-weight:700;color:' + (col[g]||'#6b7280') + '">[' + g + ']</span>';
}
function renderEmpty(msg) {
  document.getElementById('btContent').innerHTML =
    '<div class="empty-state"><div class="empty-icon">❌</div>' +
    '<div class="empty-title">오류 발생</div>' +
    '<div class="empty-sub">' + (msg||'알 수 없는 오류') + '</div></div>';
}

/* ── AI 분석 프롬프트 다운로드 ── */
function downloadAiPrompt() {
  if (!lastResults || !lastResults.length) { toast('먼저 백테스트를 실행하세요.', 'err'); return; }
  var params = buildSymbolReq(lastResults.map(function(r){return r.symbol;}).join(','));
  fetch(ctx + '/backtest/exportPrompt', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({ params: params, results: lastResults })
  })
  .then(function(r) { return r.blob(); })
  .then(function(blob) {
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url; a.download = 'backtest_ai_prompt.txt'; a.click();
    URL.revokeObjectURL(url);
  })
  .catch(function() { toast('다운로드 실패', 'err'); });
}

/* ── UI Helpers ── */
function setButtons(loading) {
  document.getElementById('btnRun').disabled = loading;
}
function showProgress(p, msg) {}
function hideProgress() {}

/* ── Run Modal ── */
var _rmTimer = null, _rmSec = 0;
function openRunModal() {
  _rmSec = 0;
  document.getElementById('rstep1').className = 'rm-pill active';
  document.getElementById('rstep2').className = 'rm-pill';
  document.getElementById('rmStatus').textContent = '분봉 수집 중';
  setRunProgress(0, '준비 중...', '');
  document.getElementById('runElapsed').textContent = '';
  document.getElementById('runModal').style.display = 'flex';
  clearInterval(_rmTimer);
  _rmTimer = setInterval(function() {
    _rmSec++;
    var m = Math.floor(_rmSec / 60), s = _rmSec % 60;
    document.getElementById('runElapsed').textContent =
      String(m).padStart(2,'0') + ':' + String(s).padStart(2,'0');
  }, 1000);
}
function closeRunModal() {
  clearInterval(_rmTimer);
  document.getElementById('runModal').style.display = 'none';
}
function setRunProgress(pct, log, sub, symInfo) {
  document.getElementById('runPbFill').style.width  = pct + '%';
  document.getElementById('runPct').textContent     = pct + '%';
  document.getElementById('runLog').textContent     = log  || '';
  document.getElementById('runLogSub').textContent  = sub  || '';
  document.getElementById('runSymInfo').textContent = symInfo || '';
}
function setRunStep2() {
  document.getElementById('rstep1').className = 'rm-pill done';
  document.getElementById('rstep2').className = 'rm-pill active';
  document.getElementById('rmStatus').textContent = '백테스트 실행 중';
}

/* ── runAll: 수집 → 백테스트 순차 실행 ── */
function runAll() {
  var syms = buildReq();
  if (!syms) return;
  document.getElementById('btnRun').disabled      = true;
  document.getElementById('btnAiExport').disabled = true;
  openRunModal();
  lastResults = [];

  var ci = 0;
  function collectNext() {
    if (ci >= syms.length) {
      setRunStep2();
      var bi = 0;
      function backtestNext() {
        if (bi >= syms.length) {
          closeRunModal();
          document.getElementById('btnRun').disabled = false;
          if (lastResults.length) {
            renderMultiResult(lastResults);
            document.getElementById('btnAiExport').disabled = false;
          } else {
            renderEmpty('결과 없음');
          }
          return;
        }
        var sym = syms[bi++];
        var pct = Math.round(((bi - 1) / syms.length) * 100);
        setRunProgress(pct, '▸ ' + sym + ' — 백테스트 실행 중',
          '', '[' + bi + ' / ' + syms.length + '] SIMULATING');
        fetch(ctx + '/backtest/run', {
          method:'POST', headers:{'Content-Type':'application/json'},
          body: JSON.stringify(buildSymbolReq(sym))
        })
        .then(function(r) { return r.json(); })
        .then(function(d) {
          if (d.status === 'OK') lastResults.push(d);
          else toast(sym + ' 오류: ' + (d.message || ''), 'err');
          backtestNext();
        })
        .catch(function(e) { toast(sym + ' 실패: ' + e.message, 'err'); backtestNext(); });
      }
      backtestNext();
      return;
    }
    var sym = syms[ci++];
    setRunProgress(0, '▸ ' + sym + ' — 분봉 수집 중...',
      '', '[' + ci + ' / ' + syms.length + '] COLLECTING');
    fetch(ctx + '/backtest/collectBars', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify(buildSymbolReq(sym))
    })
    .then(function(r) { return r.json(); })
    .then(function(d) {
      if (d.status !== 'STARTED') {
        toast(sym + ' 수집 오류: ' + (d.message || ''), 'err');
        collectNext(); return;
      }
      pollCollectModal(d.jobId, sym, ci, syms.length, collectNext);
    })
    .catch(function(e) { toast(sym + ' 실패: ' + e.message, 'err'); collectNext(); });
  }
  collectNext();
}
function pollCollectModal(jobId, sym, ci, total, done) {
  clearInterval(pollTimer);
  pollTimer = setInterval(function() {
    fetch(ctx + '/backtest/collectStatus/' + jobId)
      .then(function(r) { return r.json(); })
      .then(function(s) {
        var basePct = Math.round((ci - 1) / total * 100);
        var subPct  = Math.round((s.progress || 0) / total);
        setRunProgress(basePct + subPct,
          '▸ ' + sym + ' — ' + (s.message || '수집 중...'),
          '', '[' + ci + ' / ' + total + '] COLLECTING');
        if (s.state === 'DONE' || s.state === 'ERROR') {
          clearInterval(pollTimer);
          if (s.state === 'DONE') {
            setRunProgress(Math.round(ci / total * 100),
              '✓ ' + sym + ' — ' + (s.inserted || 0) + '봉 수집 완료',
              '', '[' + ci + ' / ' + total + '] DONE');
          } else {
            toast(sym + ' 수집 오류: ' + (s.message || ''), 'err');
          }
          setTimeout(done, 400);
        }
      });
  }, 2000);
}
function toast(msg, type) {
  const el = document.createElement('div');
  el.className = 'toast ' + (type === 'err' ? 'err' : 'ok');
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(function() { el.remove(); }, 3500);
}

/* ── Tooltip system ── */
var _gtip = null;
function _getGTip() {
  if (!_gtip) { _gtip = document.createElement('div'); _gtip.id = '_gtip'; document.body.appendChild(_gtip); }
  return _gtip;
}
var TOOLTIPS = {
  // 공통 진입
  p_vwapHardLimitPct:    '현재가가 VWAP 대비 이 % 초과 이격 시\n신규 진입 차단',
  p_minTurnoverKrx:      '현재봉 거래대금 최솟값(원)\n이 값 미만 봉에서는 진입 차단',
  p_minAvgTurnoverKrx:   '20봉 평균 거래대금 최솟값(원)\n구조적 유동성 부족 종목 차단',
  p_minTurnoverUs:       '하루 평균 거래대금 최솟값(USD)\n유동성 낮은 종목 진입 차단',
  p_buyCooldownSec:      '마지막 매수 후 다음 매수 허용까지\n대기 시간(초)',
  p_maxDailyEntryCount:  '하루 동안 같은 종목 최대 진입 횟수',
  p_maxSamePatternEntry: '같은 패턴(모드) 연속 진입 허용\n최대 횟수',
  p_useMarketFilter:     '시장 지수 하락 시 신규 매수 차단',
  // PULLBACK 진입
  p_pullbackMinScore:                  '풀백 신호 점수 최소값\n이 이상만 진입 후보로 인정',
  p_vwapMaxGapPullbackPct:             '매수가와 VWAP 최대 허용 이격(%)\n클수록 VWAP와 더 멀어도 허용',
  p_pullbackUpperPct:                  '현재가가 최근 고점에서 이 % 이내\n= 풀백 존 상단 조건',
  p_pullbackLowerPct:                  '현재가가 최근 고점에서 이 % 이상 하락\n= 풀백 존 하단 조건',
  p_pullbackVolumeMult:                '풀백 진입 시 평균 거래량 대비 최소 배수',
  p_pullbackVelocityShort:             '단기 가격 회복 속도 최소값\n낮을수록 느린 회복도 허용',
  p_pullbackVelocityMid:               '중기 속도 최소값\n0이면 조건 비활성',
  p_pullbackRequiredBullishBars:       '연속 양봉 최소 개수\n낮을수록 일찍 진입',
  p_pullbackRequireAboveVwap:          '현재가가 VWAP 위에 있어야 진입',
  p_pullbackRequireVwapSlope:          'VWAP 기울기 상승 중이어야 진입',
  p_pullbackRequireRecentHighBreakout: '최근 고점을 막 갱신한 상태여야 진입',
  // PULLBACK 청산
  p_pullbackStopPct:   '진입가 대비 손절 하락폭(%)',
  p_pullbackTpPct:     '진입가 대비 목표 수익률(%)',
  p_pullbackTrailSt:   '이 수익률 도달 후 트레일링 스탑 시작',
  p_pullbackTrailDrop: '트레일링: 최고점 대비 이 % 하락 시 청산',
  // BREAKOUT 진입
  p_breakoutMinScore:            '브레이크아웃 신호 점수 최소값',
  p_vwapMaxGapBreakoutPct:       '매수가와 VWAP 최대 허용 이격(%)',
  p_breakoutRetestLower:         '돌파 후 리테스트 되돌림 허용 하한(%)',
  p_breakoutRetestUpper:         '돌파 후 리테스트 되돌림 허용 상한(%)',
  p_breakoutStrongVolMult:       '강한 돌파 판단 거래량 배수\n이 이상이면 강한 브레이크아웃',
  p_breakoutRequireAcceleration: '가격 가속도(속도 증가세)가\n있어야 진입',
  p_breakoutRequireMultiUptrend: '단·중·장기 이동평균\n복수 상승 정렬 요구',
  p_breakoutOverheatBlock:       '급등 직후 봉은 진입 차단\n(과열 방지)',
  // BREAKOUT 청산
  p_breakoutStopPct:   '진입가 대비 손절 하락폭(%)',
  p_breakoutTpPct:     '진입가 대비 목표 수익률(%)',
  p_breakoutTrailSt:   '이 수익률 도달 후 트레일링 스탑 시작',
  p_breakoutTrailDrop: '트레일링: 최고점 대비 이 % 하락 시 청산',
  p_volumeMult:        '기본 거래량 필터 배수\n(공통 필터 — 이 배수 미만이면 진입 차단)',
  // EARLY_MOMENTUM 진입
  p_emMinScore:   '얼리 모멘텀 신호 점수 최소값',
  p_emVelocity:   '초기 급등 속도 최소값\n낮을수록 느린 모멘텀도 허용',
  p_emVolumeMult: '평균 거래량 대비 최소 배수',
  p_em3TrendUp:   '단·중·장기 3개 이동평균\n모두 상승 정렬 요구',
  // EARLY_MOMENTUM 청산
  p_emStopPct: '진입가 대비 손절 하락폭(%)',
  p_emTpPct:   '진입가 대비 목표 수익률(%)',
  // STRONG_PULLBACK 진입
  p_enableStrongPullback: '강한 당일 상승 종목의 눌림목 전략 활성화\n당일 급등 후 VWAP 위에서 적절히 눌릴 때 진입',
  p_spVwapMinAbovePct:'현재가가 VWAP보다 최소 이 % 이상 위에 있어야 함\n(필수 조건)',
  p_spPullbackMinPct:  '최근 15봉 고점 대비 최소 눌림 비율(%)\n이보다 덜 눌렸으면 진입 안 함',
  p_spPullbackMaxPct:  '최근 15봉 고점 대비 최대 눌림 비율(%)\n이보다 더 눌리면 추세 훼손으로 진입 안 함',
  p_spVol3RatioMax:    '최근 3봉 평균 거래량 / 10봉 평균 거래량 상한\n이보다 낮아야 거래량 감소 조건 충족 (+20pts)',
  p_spBodyRatioMin:    '현재 봉 몸통 비율 최소값 (양봉 강도)\nbodyRatio = (close-open)/(high-low) ≥ 이 값이면 +20pts',
  p_spMinScore:        '강한눌림목 전략 최소 진입 점수 (0~100)\n4개 연속값 항목 합산 — 조건을 얼마나 강하게 충족하는지 반영',
  // STRONG_PULLBACK 청산
  p_spStopPct:   '진입가 대비 손절 하락폭(%)',
  p_spTpPct:     '진입가 대비 고정 익절 목표(%)\n0이면 비활성',
  p_spTrailSt:   '이 수익률 도달 후 트레일링 스탑 시작',
  p_spTrailDrop: '트레일링: 최고점 대비 이 % 하락 시 청산',
  // VWAP_RECLAIM 진입
  p_enableVwapReclaim:  'VWAP 재탈환 전략 활성화\n한번 VWAP 아래로 갔다가 재탈환 시 진입',
  p_vrLookbackBars:     'VWAP 이탈 여부 확인할 과거 봉 수\n이 기간 내 VWAP 아래 봉이 있어야 함',
  p_vrVolMult:          '현재 거래량 ≥ 최근 5봉 평균 × 이 배수\n(필수 조건 — 재탈환 강도 확인)',
  p_vrMinAboveVwapBars: 'VWAP 위에서 연속으로 마감한 최소 봉 수\n(필수 조건)',
  p_vrMinScore:         'VWAP재탈환 전략 최소 진입 점수 (0~100)\n4개 연속값 항목 합산 — 거래량/안착봉/이격/속도 강도 반영',
  // VWAP_RECLAIM 청산
  p_vrStopPct:   '진입가 대비 손절 하락폭(%)',
  p_vrTpPct:     '진입가 대비 고정 익절 목표(%)\n0이면 비활성',
  p_vrTrailSt:   '이 수익률 도달 후 트레일링 스탑 시작',
  p_vrTrailDrop: '트레일링: 최고점 대비 이 % 하락 시 청산',
  // 공통 청산
  p_emergencyStopPct:  '어떤 조건과 무관하게 즉시 청산하는\n최대 손실 한도(%)',
  p_vwapBreakBuffer:   '현재가가 VWAP 아래로 이 % 초과 이탈 시\n청산 신호',
  p_breakevenPeak:     '이 수익률 이상 도달 후\nBreakeven Guard 활성화',
  p_breakevenLoss:     'Breakeven 활성 후\n이 손실률 도달 시 청산',
  p_useVwapBreak:      '현재가가 VWAP를 이탈하면 청산',
  p_useBreakevenGuard: '한때 충분히 올랐다가\n손실 전환 시 청산',
  p_useFailedBreakout: '브레이크아웃 실패 패턴\n감지 시 청산',
  p_useFailedPullback: '풀백 회복 실패 패턴\n감지 시 청산',
  p_useEodForceSell:   '장 마감 전 보유 포지션 강제 청산',
  p_blockSGrade:       'S등급(점수 90~94) 신호에서 진입 차단\n실증 분석에서 승률이 가장 낮은 구간',
  p_blockAGrade:       'A등급(점수 85~89) 신호에서 진입 차단\nS등급 차단 후 추가 필터링이 필요할 때 사용',
  // 비용
  p_slippagePct: '매수/매도 체결 슬리피지 추정치(편도)\n양방향 합산은 2배',
  p_feePct:      '매수+매도 수수료 합산 (왕복 기준)',
  p_taxPct:      '매도 시 부과되는 증권거래세\nKRX 전용, US는 0으로 설정'
};
(function() {
  Object.keys(TOOLTIPS).forEach(function(id) {
    var el = document.getElementById(id);
    if (!el) return;
    var target = null;
    if (el.type === 'checkbox') {
      target = document.querySelector('label[for="' + id + '"]');
    } else {
      var grp = el.closest && el.closest('.f-group');
      if (grp) target = grp.querySelector('.f-label');
    }
    if (target) target.setAttribute('data-tip', TOOLTIPS[id]);
  });
  document.addEventListener('mouseover', function(e) {
    var t = e.target.closest ? e.target.closest('[data-tip]') : null;
    if (!t) return;
    var tip = _getGTip();
    tip.textContent = t.getAttribute('data-tip');
    tip.style.display = 'block';
  });
  document.addEventListener('mouseout', function(e) {
    var t = e.target.closest ? e.target.closest('[data-tip]') : null;
    if (t && _gtip) _gtip.style.display = 'none';
  });
  document.addEventListener('mousemove', function(e) {
    if (_gtip && _gtip.style.display !== 'none') {
      var tx = e.clientX + 14, ty = e.clientY + 16;
      if (tx + _gtip.offsetWidth  > window.innerWidth)  tx = e.clientX - _gtip.offsetWidth  - 8;
      if (ty + _gtip.offsetHeight > window.innerHeight) ty = e.clientY - _gtip.offsetHeight - 8;
      _gtip.style.left = tx + 'px';
      _gtip.style.top  = ty + 'px';
    }
  });
})();
</script>
<!-- RUN MODAL -->
<div id="runModal" class="run-modal" style="display:none">
  <div class="run-modal-box">
    <div class="rm-top-bar"></div>
    <div class="rm-inner">
      <div class="rm-header">
        <div>
          <div class="rm-tag">Autotrade · Simulation Engine</div>
          <div class="rm-title" id="rmStatus">분봉 수집 중</div>
        </div>
        <div class="rm-time-box">
          <div class="rm-time-label">Elapsed</div>
          <div class="rm-elapsed" id="runElapsed">00:00</div>
        </div>
      </div>
      <div class="rm-big-pct" id="runPct">0%</div>
      <div class="rm-pb-track">
        <div class="rm-pb-fill" id="runPbFill" style="width:0%"></div>
      </div>
      <div class="rm-pills">
        <div class="rm-pill active" id="rstep1">
          <div class="rm-pill-dot"></div>Data Collection
        </div>
        <div class="rm-pill" id="rstep2">
          <div class="rm-pill-dot"></div>Simulation
        </div>
        <div style="flex:1"></div>
        <div class="rm-pill" style="border:none;color:rgba(255,255,255,.25);" id="runSymInfo"></div>
      </div>
      <div class="rm-log-box">
        <div class="rm-log" id="runLog">준비 중...</div>
        <div class="rm-log-sub" id="runLogSub"></div>
      </div>
    </div>
  </div>
</div>
</body>
</html>
