package com.autotrading.controller;

import com.autotrading.model.BacktestConfig;
import com.autotrading.service.BacktestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

@Controller
@RequestMapping("/backtest")
public class BacktestController {

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @GetMapping
    public String index() {
        return "backtest";
    }

    @PostMapping("/collectBars")
    @ResponseBody
    public Map<String, Object> collectBars(@RequestBody Map<String, String> req) {
        return backtestService.startCollect(
                req.getOrDefault("market",    "KRX"),
                req.getOrDefault("symbol",    "").trim().toUpperCase(),
                req.getOrDefault("startDate", ""),
                req.getOrDefault("endDate",   "")
        );
    }

    @GetMapping("/collectStatus/{jobId}")
    @ResponseBody
    public Map<String, Object> collectStatus(@PathVariable String jobId) {
        return backtestService.getCollectStatus(jobId);
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, Object> run(@RequestBody Map<String, String> req) {
        double buyAmount = 600_000.0;
        try { buyAmount = Double.parseDouble(req.getOrDefault("buyAmount", "600000")); }
        catch (NumberFormatException ignored) {}

        return backtestService.runBacktest(
                req.getOrDefault("market",    "KRX"),
                req.getOrDefault("symbol",    "").trim().toUpperCase(),
                req.getOrDefault("startDate", ""),
                req.getOrDefault("endDate",   ""),
                buyAmount,
                parseConfig(req)
        );
    }

    /** US symbol autocomplete via Yahoo Finance */
    @GetMapping("/searchSymbol")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> searchSymbol(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "US") String market) {
        if (q.trim().length() < 1 || !"US".equalsIgnoreCase(market))
            return Collections.emptyList();
        try {
            String enc = URLEncoder.encode(q.trim(), "UTF-8");
            String urlStr = "https://query1.finance.yahoo.com/v1/finance/search?q=" + enc
                    + "&quotesCount=8&newsCount=0&enableFuzzyQuery=false&enableCb=false";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            if (conn.getResponseCode() != 200) return Collections.emptyList();

            Map<?, ?> root   = new ObjectMapper().readValue(conn.getInputStream(), Map.class);
            List<?>   quotes = (List<?>) root.get("quotes");
            if (quotes == null) return Collections.emptyList();

            List<Map<String, String>> result = new ArrayList<>();
            for (Object o : quotes) {
                Map<?, ?> qm  = (Map<?, ?>) o;
                String    sym = (String) qm.get("symbol");
                if (sym == null || sym.contains(".")) continue;
                String name = qm.get("shortname") != null ? (String) qm.get("shortname")
                            : qm.get("longname")  != null ? (String) qm.get("longname") : "";
                String exch = qm.get("exchDisp") != null ? (String) qm.get("exchDisp") : "";
                String type = qm.get("quoteType") != null ? (String) qm.get("quoteType") : "";
                Map<String, String> item = new LinkedHashMap<>();
                item.put("symbol", sym);
                item.put("name",   name);
                item.put("exchange", exch);
                item.put("type",   type);
                result.add(item);
                if (result.size() >= 6) break;
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private BacktestConfig parseConfig(Map<String, String> req) {
        BacktestConfig cfg = new BacktestConfig();
        tryInt(req, "pullbackMinScore",      v -> cfg.pullbackMinScore      = v);
        tryInt(req, "breakoutMinScore",      v -> cfg.breakoutMinScore      = v);
        tryDbl(req, "vwapMaxGapBreakoutPct", v -> cfg.vwapMaxGapBreakoutPct = v);
        tryDbl(req, "vwapMaxGapPullbackPct", v -> cfg.vwapMaxGapPullbackPct = v);
        tryDbl(req, "pullbackUpperPct",      v -> cfg.pullbackUpperPct      = v);
        tryDbl(req, "pullbackLowerPct",      v -> cfg.pullbackLowerPct      = v);
        tryDbl(req, "volumeMult",            v -> cfg.volumeMult            = v);
        tryDbl(req, "minTurnoverKrx",        v -> cfg.minTurnoverKrx        = v);
        tryDbl(req, "minTurnoverUs",         v -> cfg.minTurnoverUs         = v);
        tryDbl(req, "stopLossPct",           v -> cfg.stopLossPct           = v);
        tryDbl(req, "takeProfitPct",         v -> cfg.takeProfitPct         = v);
        tryDbl(req, "trailStartPct",         v -> cfg.trailStartPct         = v);
        tryDbl(req, "trailDropPct",          v -> cfg.trailDropPct          = v);
        tryInt(req, "vwapBreakGraceSec",     v -> cfg.vwapBreakGraceSec     = v);
        tryInt(req, "softTimeStopSec",       v -> cfg.softTimeStopSec       = v);
        tryInt(req, "midTimeStopSec",        v -> cfg.midTimeStopSec        = v);
        tryInt(req, "hardTimeStopSec",       v -> cfg.hardTimeStopSec       = v);
        tryInt(req, "maxDailyEntryCount",    v -> cfg.maxDailyEntryCount    = v);
        tryInt(req, "maxSamePatternEntry",   v -> cfg.maxSamePatternEntry   = v);
        tryDbl(req, "slippagePct",           v -> cfg.slippagePct           = v);
        tryDbl(req, "feePct",                v -> cfg.feePct                = v);
        return cfg;
    }

    private void tryInt(Map<String, String> req, String key, IntConsumer fn) {
        String v = req.get(key);
        if (v != null && !v.isBlank()) try { fn.accept(Integer.parseInt(v.trim())); } catch (Exception ignored) {}
    }

    private void tryDbl(Map<String, String> req, String key, DoubleConsumer fn) {
        String v = req.get(key);
        if (v != null && !v.isBlank()) try { fn.accept(Double.parseDouble(v.trim())); } catch (Exception ignored) {}
    }
}
