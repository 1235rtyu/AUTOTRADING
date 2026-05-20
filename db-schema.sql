CREATE DATABASE IF NOT EXISTS auto_sy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auto_sy;

CREATE TABLE IF NOT EXISTS tb_auto_watchlist (
  id INT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  exchange VARCHAR(8) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tb_auto_watchlist_symbol (symbol)
);

CREATE TABLE IF NOT EXISTS tb_auto_position (
  id INT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  avg_price DOUBLE NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tb_auto_position_symbol (symbol)
);

CREATE TABLE IF NOT EXISTS tb_auto_order_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  side VARCHAR(10) NOT NULL,
  quantity INT NOT NULL,
  price DOUBLE NOT NULL,
  reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_auto_price_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  price DOUBLE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_auto_strategy (
  id INT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  strategy_type VARCHAR(100),
  status VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 트레이드 원장: 매수~매도 완료 1건 = 1 row
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_trade_history (
  id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  trade_date          DATE             NOT NULL,
  symbol              VARCHAR(20)      NOT NULL,
  market              VARCHAR(5)       NOT NULL COMMENT 'KRX or US',
  entry_mode          VARCHAR(20)      NOT NULL DEFAULT 'UNKNOWN',

  entry_price         DECIMAL(18,4)    NOT NULL,
  entry_qty           INT UNSIGNED     NOT NULL,
  entry_time          DATETIME(3)      NOT NULL,
  entry_signal_score  SMALLINT UNSIGNED    NULL,
  entry_vwap_dist_pct DECIMAL(8,5)         NULL COMMENT 'VWAP 이격률 (진입 시)',
  entry_velocity_sht  DECIMAL(10,6)        NULL COMMENT 'velocityShort (진입 시)',

  exit_price          DECIMAL(18,4)    NOT NULL,
  exit_qty            INT UNSIGNED     NOT NULL,
  exit_time           DATETIME(3)      NOT NULL,
  exit_reason         VARCHAR(60)      NOT NULL,
  exit_type           VARCHAR(12)      NOT NULL DEFAULT 'NONE',

  hold_seconds        INT UNSIGNED     NOT NULL DEFAULT 0,
  pnl_amount          DECIMAL(18,2)    NOT NULL DEFAULT 0.00 COMMENT '실현손익(원/USD)',
  pnl_pct             DECIMAL(10,6)    NOT NULL DEFAULT 0.000000 COMMENT '(exit-entry)/entry',
  weighted_pnl        DECIMAL(10,6)    NOT NULL DEFAULT 0.000000 COMMENT '주문금액 대비 가중손익',
  peak_pnl_pct        DECIMAL(10,6)        NULL COMMENT '보유 중 최고 수익률',
  fee_amount          DECIMAL(18,2)    NOT NULL DEFAULT 0.00,
  slippage_pct        DECIMAL(10,6)    NOT NULL DEFAULT 0.000000,
  is_partial          TINYINT(1)       NOT NULL DEFAULT 0,

  created_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_th_date_mode   (trade_date, entry_mode),
  INDEX idx_th_symbol_date (symbol, trade_date),
  INDEX idx_th_market_date (market, trade_date),
  INDEX idx_th_exit_reason (exit_reason),
  INDEX idx_th_exit_type   (exit_type, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 모드별 일별 집계
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_trade_mode_daily_stats (
  stat_date           DATE             NOT NULL,
  market              VARCHAR(5)       NOT NULL,
  entry_mode          VARCHAR(20)      NOT NULL,

  trade_count         INT UNSIGNED     NOT NULL DEFAULT 0,
  win_count           INT UNSIGNED     NOT NULL DEFAULT 0,
  loss_count          INT UNSIGNED     NOT NULL DEFAULT 0,

  win_rate            DECIMAL(8,5)     NOT NULL DEFAULT 0.00000  COMMENT '0~1',
  avg_profit_pct      DECIMAL(10,6)    NOT NULL DEFAULT 0.000000 COMMENT '승리 평균 수익률',
  avg_loss_pct        DECIMAL(10,6)    NOT NULL DEFAULT 0.000000 COMMENT '패배 평균 손실률 (음수)',
  expectancy_pct      DECIMAL(10,6)    NOT NULL DEFAULT 0.000000 COMMENT '(W%×avgProfit)+(L%×avgLoss)',
  total_pnl_pct       DECIMAL(10,6)    NOT NULL DEFAULT 0.000000,
  total_pnl_amount    DECIMAL(18,2)    NOT NULL DEFAULT 0.00,
  avg_hold_seconds    INT UNSIGNED     NOT NULL DEFAULT 0,

  avg_vwap_dist_pct   DECIMAL(8,5)         NULL,
  avg_velocity_sht    DECIMAL(10,6)        NULL,
  breakout_fail_cnt   INT UNSIGNED         NULL,

  updated_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (stat_date, market, entry_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
