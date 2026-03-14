CREATE DATABASE IF NOT EXISTS order_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS inventory_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS shipment_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS customer_relations_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'kafka_challenge'@'%' IDENTIFIED BY 'kafka_challenge';

GRANT ALL PRIVILEGES ON order_service.* TO 'kafka_challenge'@'%';
GRANT ALL PRIVILEGES ON inventory_service.* TO 'kafka_challenge'@'%';
GRANT ALL PRIVILEGES ON shipment_service.* TO 'kafka_challenge'@'%';
GRANT ALL PRIVILEGES ON customer_relations_service.* TO 'kafka_challenge'@'%';

FLUSH PRIVILEGES;
