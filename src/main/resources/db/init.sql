-- 用户表 (Customer & Admin)
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) UNIQUE,
  `role` ENUM('CUSTOMER', 'ADMIN') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  -- ... 更多字段 ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
  -- ... 更多字段 ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ... 更多表 ...