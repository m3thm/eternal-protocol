USE eternal_protocol;

-- Temporarily disable FK checks so deletion order doesn't matter
SET FOREIGN_KEY_CHECKS = 0;
 
TRUNCATE TABLE order_items;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE commissions;
TRUNCATE TABLE orders;
TRUNCATE TABLE product_images;
TRUNCATE TABLE products;
 
-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE products MODIFY category enum('ACCESSORIES','MENS','WOMENS') NOT NULL;
ALTER TABLE products MODIFY sub_category varchar(255) NOT NULL;
ALTER TABLE products MODIFY status enum('ACTIVE','COMING_SOON') NOT NULL DEFAULT 'ACTIVE';

