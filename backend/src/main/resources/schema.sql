CREATE TABLE IF NOT EXISTS `hospital` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hospital_name` VARCHAR(120) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hospital_name` (`hospital_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `material_unit_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `pack_particle_qty` DECIMAL(18,3) NOT NULL,
  `particle_unit_price` DECIMAL(18,6) NOT NULL DEFAULT 0,
  `effective_from` DATE NOT NULL,
  `effective_to` DATE DEFAULT NULL,
  `version_no` VARCHAR(32) NOT NULL DEFAULT 'v1',
  PRIMARY KEY (`id`),
  KEY `idx_material_code_effective` (`material_code`, `effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stocktake_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hospital_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `summary_date` DATE NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `calculation_version` VARCHAR(32) NOT NULL DEFAULT 'v1',
  `source_batch_id` VARCHAR(64) DEFAULT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary_business` (`hospital_id`, `material_id`, `summary_date`, `deleted_at`),
  KEY `idx_summary_date` (`summary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stocktake_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `summary_id` BIGINT NOT NULL,
  `summary_date` DATE NOT NULL,
  `hospital_id` BIGINT NOT NULL,
  `hospital_name` VARCHAR(120) NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `drug_name` VARCHAR(120) NOT NULL,
  `bag_count` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `whole_particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `loose_particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `actual_stock_particles` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `hospital_shortage_particles` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `hospital_particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `book_stock_particles` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `factory_increase_bags` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `hospital_loss_bags` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `particle_unit_price` DECIMAL(18,6) NOT NULL DEFAULT 0,
  `hospital_loss_amount` DECIMAL(18,2) NOT NULL DEFAULT 0,
  `factory_increase_amount` DECIMAL(18,2) NOT NULL DEFAULT 0,
  `guidance_start_date` DATE DEFAULT NULL,
  `guidance_end_date` DATE DEFAULT NULL,
  `monthly_avg_consumption` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `current_month_demand_bags` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `available_months` DECIMAL(18,3) DEFAULT NULL,
  `thirty_day_purchase_bags` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `calculation_version` VARCHAR(32) NOT NULL DEFAULT 'v1',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_detail_query` (`summary_date`, `hospital_id`, `material_code`, `deleted_at`),
  KEY `idx_detail_drug_name` (`drug_name`),
  CONSTRAINT `fk_detail_summary` FOREIGN KEY (`summary_id`) REFERENCES `stocktake_summary` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `operation_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `module` VARCHAR(64) NOT NULL,
  `operation_type` VARCHAR(32) NOT NULL,
  `request_summary` TEXT,
  `affected_count` INT NOT NULL DEFAULT 0,
  `result` VARCHAR(20) NOT NULL,
  `failure_reason` VARCHAR(500) DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_module_time` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `export_task` (
  `id` CHAR(36) NOT NULL,
  `module` VARCHAR(64) NOT NULL,
  `format` VARCHAR(16) NOT NULL DEFAULT 'CSV',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  `row_count` INT NOT NULL DEFAULT 0,
  `file_name` VARCHAR(255) DEFAULT NULL,
  `query_summary` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` DATETIME DEFAULT NULL,
  `failure_reason` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_export_task_time` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 要货指导的来源流水。当前先提供最小可用数据结构，后续医院供货、终端销量页面直接复用。
CREATE TABLE IF NOT EXISTS `opening_inventory_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hospital_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `opening_date` DATE NOT NULL,
  `particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'VALIDATED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'STOCKTAKE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_opening_inventory_business` (`hospital_id`, `material_id`, `opening_date`, `deleted_at`),
  KEY `idx_opening_inventory_lookup` (`hospital_id`, `material_id`, `opening_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hospital_supply_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hospital_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `supply_date` DATE NOT NULL,
  `particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'VALIDATED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'SEED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_supply_lookup` (`hospital_id`, `material_id`, `supply_date`, `status`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `terminal_sales_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hospital_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'VALIDATED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'SEED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sales_lookup` (`hospital_id`, `material_id`, `start_date`, `end_date`, `status`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 其余业务模块统一记录表。字段覆盖需求文档中的核心字段，扩展字段以 JSON 保存，便于后续接入正式主数据。
CREATE TABLE IF NOT EXISTS `module_business_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `module_code` VARCHAR(40) NOT NULL,
  `business_date` DATE DEFAULT NULL,
  `start_date` DATE DEFAULT NULL,
  `end_date` DATE DEFAULT NULL,
  `opening_date` DATE DEFAULT NULL,
  `effective_from` DATE DEFAULT NULL,
  `effective_to` DATE DEFAULT NULL,
  `hospital_id` BIGINT DEFAULT NULL,
  `hospital_name` VARCHAR(120) DEFAULT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `particle_id` VARCHAR(64) DEFAULT NULL,
  `batch_no` VARCHAR(64) DEFAULT NULL,
  `drug_name` VARCHAR(120) DEFAULT NULL,
  `enterprise_name` VARCHAR(120) DEFAULT NULL,
  `national_name` VARCHAR(120) DEFAULT NULL,
  `spec_qty_per_bag` DECIMAL(18,3) DEFAULT NULL,
  `particle_qty` DECIMAL(18,3) NOT NULL DEFAULT 0,
  `standard_particle_qty` DECIMAL(18,3) DEFAULT NULL,
  `tablet_qty` DECIMAL(18,3) DEFAULT NULL,
  `bag_count` DECIMAL(18,3) DEFAULT NULL,
  `particle_unit_price` DECIMAL(18,6) DEFAULT NULL,
  `currency` VARCHAR(8) DEFAULT 'CNY',
  `expiry_date` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `validation_status` VARCHAR(20) NOT NULL DEFAULT 'VALID',
  `lock_status` VARCHAR(20) NOT NULL DEFAULT 'UNLOCKED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  `extra_json` JSON DEFAULT NULL,
  `version_no` INT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_business` (`module_code`,`hospital_id`,`material_id`,`business_date`,`source_type`),
  KEY `idx_module_record_query` (`module_code`,`business_date`,`hospital_id`,`material_code`,`deleted_at`),
  KEY `idx_module_record_dates` (`module_code`,`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `herbal_granule_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `record_id` BIGINT NOT NULL, `alias_code` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hospital_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `category_code` VARCHAR(40) NOT NULL, `category_name` VARCHAR(80) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0, `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', PRIMARY KEY (`id`), UNIQUE KEY (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `hospital` ADD COLUMN `parent_id` BIGINT NULL AFTER `id`;
ALTER TABLE `hospital` ADD COLUMN `name` VARCHAR(120) NULL;
ALTER TABLE `hospital` ADD COLUMN `path` VARCHAR(500) NULL;
ALTER TABLE `hospital` ADD COLUMN `level_no` INT NOT NULL DEFAULT 1;
ALTER TABLE `hospital` ADD COLUMN `category_code` VARCHAR(40) NULL;
ALTER TABLE `hospital` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0;
ALTER TABLE `hospital` ADD COLUMN `hospital_code` VARCHAR(64) NULL;
ALTER TABLE `hospital` ADD COLUMN `owner_name` VARCHAR(64) NULL;
ALTER TABLE `hospital` ADD COLUMN `owner_phone` VARCHAR(32) NULL;
ALTER TABLE `hospital` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE `hospital` ADD COLUMN `version_no` INT NOT NULL DEFAULT 1;
UPDATE `hospital` SET `name`=`hospital_name` WHERE `name` IS NULL;

INSERT IGNORE INTO `hospital_category` (`category_code`,`category_name`,`sort_order`) VALUES
 ('GENERAL','综合医院',1),('TCM','中医院',2),('COMMUNITY','社区/基层',3);

INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`standard_particle_qty`,`bag_count`,`particle_unit_price`,`status`,`validation_status`,`source_type`)
SELECT 'hospital-supply', d.summary_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.hospital_particle_qty,d.hospital_particle_qty,d.bag_count,d.particle_unit_price,'ACTIVE','VALID','SEED'
FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`start_date`,`end_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`status`,`validation_status`,`source_type`)
SELECT 'terminal-sales', d.summary_date,d.guidance_start_date,d.summary_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.monthly_avg_consumption,'ACTIVE','VALID','SEED'
FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`standard_particle_qty`,`bag_count`,`expiry_date`,`status`,`validation_status`,`source_type`)
SELECT 'whole-inventory',d.summary_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.whole_particle_qty,d.whole_particle_qty,d.bag_count,NULL,'ACTIVE','VALID','STOCKTAKE' FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`standard_particle_qty`,`expiry_date`,`status`,`validation_status`,`source_type`)
SELECT 'loose-inventory',d.summary_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.loose_particle_qty,d.loose_particle_qty,NULL,'ACTIVE','VALID','STOCKTAKE' FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`standard_particle_qty`,`tablet_qty`,`status`,`validation_status`,`source_type`)
SELECT 'hospital-inventory',d.summary_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.hospital_particle_qty,d.hospital_particle_qty,d.hospital_shortage_particles,'ACTIVE','VALID','STOCKTAKE' FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`business_date`,`opening_date`,`hospital_id`,`hospital_name`,`material_id`,`material_code`,`drug_name`,`particle_qty`,`standard_particle_qty`,`bag_count`,`lock_status`,`status`,`validation_status`,`source_type`)
SELECT 'opening-inventory',d.summary_date,d.guidance_start_date,d.hospital_id,d.hospital_name,d.material_id,d.material_code,d.drug_name,d.actual_stock_particles,d.actual_stock_particles,d.bag_count,'UNLOCKED','ACTIVE','VALID','STOCKTAKE' FROM stocktake_detail d WHERE d.deleted_at IS NULL;
INSERT IGNORE INTO `module_business_record` (`module_code`,`effective_from`,`hospital_id`,`material_id`,`material_code`,`particle_id`,`enterprise_name`,`national_name`,`drug_name`,`spec_qty_per_bag`,`particle_unit_price`,`status`,`source_type`)
SELECT 'herbal-granules',NULL,NULL,d.material_id,d.material_code,CAST(d.material_id AS CHAR),'以岭药业',d.drug_name,d.drug_name,100,d.particle_unit_price,'ACTIVE','SEED' FROM stocktake_detail d WHERE d.deleted_at IS NULL AND NOT EXISTS (SELECT 1 FROM module_business_record x WHERE x.module_code='herbal-granules' AND x.material_id=d.material_id AND x.deleted_at IS NULL);

INSERT IGNORE INTO `hospital` (`id`, `hospital_name`) VALUES
  (1, '河北北方学院附属第一医院'),
  (2, '石家庄市中医院'),
  (3, '保定市第一中心医院');

INSERT IGNORE INTO `material_unit_config`
  (`id`, `material_id`, `material_code`, `pack_particle_qty`, `particle_unit_price`, `effective_from`, `version_no`) VALUES
  (1, 1001, '02.220001', 100, 2.56, '2025-01-01', 'v1'),
  (2, 1002, '02.220002', 100, 2.37, '2025-01-01', 'v1'),
  (3, 1003, '02.220003', 100, 1.80, '2025-01-01', 'v1'),
  (4, 1004, '02.220004', 100, 1.60, '2025-01-01', 'v1'),
  (5, 1005, '02.220005', 100, 2.13, '2025-01-01', 'v1'),
  (6, 1006, '02.220006', 100, 1.58, '2025-01-01', 'v1'),
  (7, 1007, '02.220007', 100, 0.57, '2025-01-01', 'v1'),
  (8, 1008, '02.220008', 100, 31.10, '2025-01-01', 'v1'),
  (9, 1009, '02.220009', 100, 0.54, '2025-01-01', 'v1'),
  (10, 1010, '02.220010', 100, 1.32, '2025-01-01', 'v1');

INSERT IGNORE INTO `stocktake_summary`
  (`id`, `hospital_id`, `material_id`, `summary_date`, `status`, `calculation_version`) VALUES
  (1, 1, 1001, '2025-12-26', 'CONFIRMED', 'v1'),
  (2, 1, 1002, '2025-12-26', 'CONFIRMED', 'v1'),
  (3, 1, 1003, '2025-12-26', 'CONFIRMED', 'v1'),
  (4, 1, 1004, '2025-12-26', 'CONFIRMED', 'v1'),
  (5, 1, 1005, '2025-12-26', 'CONFIRMED', 'v1'),
  (6, 1, 1006, '2025-12-26', 'CONFIRMED', 'v1'),
  (7, 1, 1007, '2025-12-26', 'CONFIRMED', 'v1'),
  (8, 1, 1008, '2025-12-26', 'CONFIRMED', 'v1'),
  (9, 1, 1009, '2025-12-26', 'CONFIRMED', 'v1'),
  (10, 1, 1010, '2025-12-26', 'CONFIRMED', 'v1');

INSERT IGNORE INTO `stocktake_detail`
  (`id`, `summary_id`, `summary_date`, `hospital_id`, `hospital_name`, `material_id`, `material_code`, `drug_name`,
   `bag_count`, `whole_particle_qty`, `loose_particle_qty`, `actual_stock_particles`, `hospital_shortage_particles`,
   `hospital_particle_qty`, `book_stock_particles`, `factory_increase_bags`, `hospital_loss_bags`, `particle_unit_price`,
   `hospital_loss_amount`, `factory_increase_amount`, `guidance_start_date`, `guidance_end_date`, `monthly_avg_consumption`,
   `current_month_demand_bags`, `available_months`, `thirty_day_purchase_bags`, `calculation_version`, `status`) VALUES
  (1, 1, '2025-12-26', 1, '河北北方学院附属第一医院', 1001, '02.220001', '茯苓皮', 2, 200, 268.57, 468.57, 136, 14088, 604.57, 0.21, -136, 2.56, -348.16, 0.54, '2025-08-20', '2025-12-26', 1.29, 0.02, 242.16, 0, 'v1', 'CONFIRMED'),
  (2, 2, '2025-12-26', 1, '河北北方学院附属第一医院', 1002, '02.220002', '香加皮', 1, 100, 244.20, 344.20, 9, 1229, 344.20, 0.05, -9, 2.37, -21.33, 0.12, '2025-08-20', '2025-12-26', 1.20, 0.02, 191.22, 0, 'v1', 'CONFIRMED'),
  (3, 3, '2025-12-26', 1, '河北北方学院附属第一医院', 1003, '02.220003', '醋五灵脂', 2, 200, 166.67, 366.67, 15, 1936, 366.67, 0.20, -15, 1.80, -27.00, 0.36, '2025-08-20', '2025-12-26', 1.61, 0.02, 151.83, 0, 'v1', 'CONFIRMED'),
  (4, 4, '2025-12-26', 1, '河北北方学院附属第一医院', 1004, '02.220004', '益智仁', 0, 0, 166.33, 166.33, 0, 105, 166.33, 0.04, 0, 1.60, 0, 0.06, '2025-08-20', '2025-12-26', 1.05, 0.02, 105.61, 0, 'v1', 'CONFIRMED'),
  (5, 5, '2025-12-26', 1, '河北北方学院附属第一医院', 1005, '02.220005', '茵草', 0, 0, 263.90, 263.90, -2, 15, 263.90, 0.27, 2, 2.13, 4.26, 0.58, '2025-08-20', '2025-12-26', 2.14, 0.03, 82.21, 0, 'v1', 'CONFIRMED'),
  (6, 6, '2025-12-26', 1, '河北北方学院附属第一医院', 1006, '02.220006', '浸骨碎补', 7, 700, 194.93, 894.93, 47, 5655, 894.93, 0.97, -47, 1.58, -74.26, 1.53, '2025-08-20', '2025-12-26', 7.79, 0.12, 76.59, 0, 'v1', 'CONFIRMED'),
  (7, 7, '2025-12-26', 1, '河北北方学院附属第一医院', 1007, '02.220007', '虎杖', 2, 200, 153.26, 353.26, 12, 1570, 353.26, 0.39, -12, 0.57, -6.84, 0.22, '2025-08-20', '2025-12-26', 3.13, 0.05, 75.24, 0, 'v1', 'CONFIRMED'),
  (8, 8, '2025-12-26', 1, '河北北方学院附属第一医院', 1008, '02.220008', '桑螵蛸', 0, 0, 101.20, 101.20, 9, 1058, 101.20, 0.11, -9, 31.10, -279.90, 3.42, '2025-08-20', '2025-12-26', 0.90, 0.01, 74.96, 0, 'v1', 'CONFIRMED'),
  (9, 9, '2025-12-26', 1, '河北北方学院附属第一医院', 1009, '02.220009', '酒黄芩', 13, 1300, 233.75, 1533.75, 20, 3529, 1533.75, 2.51, -20, 0.54, -10.80, 1.36, '2025-08-20', '2025-12-26', 15.08, 0.23, 67.81, 0, 'v1', 'CONFIRMED'),
  (10, 10, '2025-12-26', 1, '河北北方学院附属第一医院', 1010, '02.220010', '苏木', 1, 100, 184.50, 284.50, 23, 2624, 284.50, 0.65, -23, 1.32, -30.36, 0.86, '2025-08-20', '2025-12-26', 3.11, 0.05, 60.99, 0, 'v1', 'CONFIRMED');

-- 演示来源数据：按现有盘点快照生成一条期初、一条供货和一条销量来源。
-- 这些记录不是新的业务录入入口，只是保证首次启动即可验证完整库存链路；后续人工/导入数据可继续写入这三张表。
INSERT IGNORE INTO `opening_inventory_snapshot`
  (`id`, `hospital_id`, `material_id`, `material_code`, `opening_date`, `particle_qty`, `status`, `source_type`)
SELECT 100000 + d.id, d.hospital_id, d.material_id, d.material_code,
       COALESCE(d.guidance_start_date, d.summary_date), d.actual_stock_particles, 'VALIDATED', 'STOCKTAKE'
FROM `stocktake_detail` d
WHERE d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM `opening_inventory_snapshot` oi WHERE oi.hospital_id=d.hospital_id AND oi.material_id=d.material_id AND oi.opening_date=COALESCE(d.guidance_start_date, d.summary_date) AND oi.deleted_at IS NULL);

INSERT IGNORE INTO `hospital_supply_record`
  (`id`, `hospital_id`, `material_id`, `material_code`, `supply_date`, `particle_qty`, `status`, `source_type`)
SELECT 200000 + d.id, d.hospital_id, d.material_id, d.material_code,
       d.summary_date, 0, 'VALIDATED', 'SEED'
FROM `stocktake_detail` d
WHERE d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM `hospital_supply_record` hs WHERE hs.hospital_id=d.hospital_id AND hs.material_id=d.material_id AND hs.supply_date=d.summary_date AND hs.source_type='SEED' AND hs.deleted_at IS NULL);

INSERT IGNORE INTO `terminal_sales_record`
  (`id`, `hospital_id`, `material_id`, `material_code`, `start_date`, `end_date`, `particle_qty`, `status`, `source_type`)
SELECT 300000 + d.id, d.hospital_id, d.material_id, d.material_code,
       COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 30 DAY)),
       d.summary_date,
       ROUND(d.monthly_avg_consumption * (DATEDIFF(d.summary_date, COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 30 DAY))) + 1) / 30, 3),
       'VALIDATED', 'SEED'
FROM `stocktake_detail` d
WHERE d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM `terminal_sales_record` ts WHERE ts.hospital_id=d.hospital_id AND ts.material_id=d.material_id AND ts.start_date=COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 30 DAY)) AND ts.end_date=d.summary_date AND ts.source_type='SEED' AND ts.deleted_at IS NULL);
