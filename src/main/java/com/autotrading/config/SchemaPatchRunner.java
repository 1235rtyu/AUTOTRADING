package com.autotrading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaPatchRunner implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(SchemaPatchRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        patchWatchlistExchangeColumn();
        patchWatchlistUniqueSymbol();
    }

    private void patchWatchlistExchangeColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_schema = DATABASE() AND table_name = 'tb_auto_watchlist' AND column_name = 'exchange'",
                    Integer.class
            );
            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.execute("ALTER TABLE tb_auto_watchlist ADD COLUMN exchange VARCHAR(8) NULL AFTER symbol");
            jdbcTemplate.update("UPDATE tb_auto_watchlist SET exchange='KRX' WHERE exchange IS NULL AND symbol REGEXP '^[0-9]'");
            jdbcTemplate.update("UPDATE tb_auto_watchlist SET exchange='NAS' WHERE exchange IS NULL AND symbol REGEXP '^[A-Za-z]'");
            logger.info("Schema patch applied: tb_auto_watchlist.exchange");
        } catch (Exception e) {
            logger.warn("Schema patch skipped for watchlist.exchange: {}", e.getMessage());
        }
    }

    private void patchWatchlistUniqueSymbol() {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = 'tb_auto_watchlist' AND index_name = 'uk_tb_auto_watchlist_symbol'",
                    Integer.class
            );
            if (exists != null && exists > 0) {
                return;
            }

            // Keep the oldest row for duplicated symbols before adding unique key.
            jdbcTemplate.execute(
                    "DELETE w1 FROM tb_auto_watchlist w1 " +
                            "INNER JOIN tb_auto_watchlist w2 ON w1.symbol = w2.symbol AND w1.id > w2.id"
            );
            jdbcTemplate.execute("ALTER TABLE tb_auto_watchlist ADD UNIQUE KEY uk_tb_auto_watchlist_symbol (symbol)");
            logger.info("Schema patch applied: tb_auto_watchlist UNIQUE(symbol)");
        } catch (Exception e) {
            logger.warn("Schema patch skipped for watchlist unique(symbol): {}", e.getMessage());
        }
    }
}
