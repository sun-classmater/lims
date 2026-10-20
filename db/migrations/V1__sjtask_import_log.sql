-- 抽样单导入记录表：以抽样单 xls 的 A1 文件标识为主键，防止重复导入
CREATE TABLE IF NOT EXISTS `sjtask_import_log` (
  `marker` varchar(128) NOT NULL COMMENT '抽样单文件标识（xls 的 A1 单元格）',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `row_count` int NOT NULL DEFAULT '0' COMMENT '实际导入数据行数',
  `created_by` varchar(64) DEFAULT NULL COMMENT '导入人工号',
  `created_at` datetime NOT NULL COMMENT '导入时间',
  PRIMARY KEY (`marker`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽样单导入记录（防重复导入）';
