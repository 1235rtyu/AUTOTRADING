package com.autotrading.service;

import com.autotrading.model.DashboardData;

public interface DashboardService {
    DashboardData load(int limit);
}
