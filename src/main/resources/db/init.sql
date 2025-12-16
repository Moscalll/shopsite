
-- 1. 用户表 (User)
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) UNIQUE,
  -- 角色枚举：CUSTOMER, MERCHANT, ADMIN
  `role` ENUM('CUSTOMER', 'MERCHANT', 'ADMIN') NOT NULL, 
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 商品分类表 (Category)
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL UNIQUE,
    `description` TEXT,
    `image_url` VARCHAR(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 商品表 (Product)
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT NOT NULL,
    `is_available` BOOLEAN NOT NULL DEFAULT TRUE,
    `image_url` VARCHAR(255),
    `category_id` BIGINT NOT NULL,
    `merchant_id` BIGINT NOT NULL, -- 关联到 User 表
    PRIMARY KEY (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
    FOREIGN KEY (`merchant_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 5. 购物车项表 (CartItem)
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,     
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL, 
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_product_unique` (`user_id`, `product_id`), 
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`), 
    FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 订单表 (Order -> shop_order)
CREATE TABLE IF NOT EXISTS `shop_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL, -- 关联到 User 表
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `order_date` DATETIME NOT NULL,
    -- 订单状态枚举
    `status` ENUM('PENDING_PAYMENT', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED') NOT NULL, 
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 订单项表 (OrderItem)
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL, -- 关联到 shop_order 表
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `price_at_order` DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`order_id`) REFERENCES `shop_order` (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 消息表 (Message)
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `create_time` DATETIME NOT NULL,
    `is_read` BOOLEAN NOT NULL,
    `related_order_id` BIGINT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 销售日志表 (SalesLog)
CREATE TABLE IF NOT EXISTS `sales_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT,
    `action_type` VARCHAR(50),
    `product_id` BIGINT,
    `log_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 收藏表 (Favorite) 
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    -- 确保一个用户不会重复收藏同一个商品
    UNIQUE KEY `user_product_unique` (`user_id`, `product_id`), 
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


