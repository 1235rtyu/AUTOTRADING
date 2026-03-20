package com.autotrading.controller;

import com.autotrading.model.WatchlistItem;
import com.autotrading.service.WatchlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public String list(Model model) {
        List<WatchlistItem> items = watchlistService.getWatchlist();
        model.addAttribute("items", items);
        return "watchlist";
    }

    @PostMapping("/add")
    public String add(@RequestParam("symbol") String symbol,
                      @RequestParam(name = "exchange", required = false) String exchange) {
        watchlistService.addSymbol(symbol, exchange);
        return "redirect:/watchlist";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") int id) {
        watchlistService.remove(id);
        return "redirect:/watchlist";
    }
}
