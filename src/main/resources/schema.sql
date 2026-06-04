CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    avatar_data MEDIUMTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type TINYINT NOT NULL,
    icon VARCHAR(30) DEFAULT 'tag',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    UNIQUE KEY idx_category_user_name_type (user_id, name, type),
    INDEX idx_category_user_type_sort (user_id, type, sort_order)
);

CREATE TABLE IF NOT EXISTS `transaction` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    type TINYINT NOT NULL,
    record_date DATE NOT NULL,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category(id),
    INDEX idx_transaction_user_date_type (user_id, record_date, type),
    INDEX idx_transaction_user_category_date (user_id, category_id, record_date)
);

INSERT INTO app_user (username, password)
SELECT 'admin', '{noop}123456'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE username = 'admin'
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '工资', 1, 'briefcase', 1 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '工资' AND c.type = 1
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '奖金', 1, 'gift', 2 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '奖金' AND c.type = 1
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '兼职', 1, 'clock', 3 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '兼职' AND c.type = 1
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '理财', 1, 'chart', 4 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '理财' AND c.type = 1
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '餐饮', 2, 'food', 1 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '餐饮' AND c.type = 2
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '交通', 2, 'car', 2 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '交通' AND c.type = 2
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '购物', 2, 'cart', 3 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '购物' AND c.type = 2
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '娱乐', 2, 'game', 4 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '娱乐' AND c.type = 2
);

INSERT INTO category (user_id, name, type, icon, sort_order)
SELECT u.id, '医疗', 2, 'medical', 5 FROM app_user u
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM category c WHERE c.user_id = u.id AND c.name = '医疗' AND c.type = 2
);

INSERT INTO `transaction` (user_id, category_id, amount, type, record_date, remark)
SELECT u.id, c.id, 5200.00, 1, '2026-05-05', '演示工资收入'
FROM app_user u JOIN category c ON c.user_id = u.id AND c.name = '工资' AND c.type = 1
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM `transaction` t WHERE t.user_id = u.id AND t.remark = '演示工资收入'
);

INSERT INTO `transaction` (user_id, category_id, amount, type, record_date, remark)
SELECT u.id, c.id, 86.50, 2, '2026-05-06', '演示餐饮支出'
FROM app_user u JOIN category c ON c.user_id = u.id AND c.name = '餐饮' AND c.type = 2
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM `transaction` t WHERE t.user_id = u.id AND t.remark = '演示餐饮支出'
);

INSERT INTO `transaction` (user_id, category_id, amount, type, record_date, remark)
SELECT u.id, c.id, 42.00, 2, '2026-05-08', '演示交通支出'
FROM app_user u JOIN category c ON c.user_id = u.id AND c.name = '交通' AND c.type = 2
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM `transaction` t WHERE t.user_id = u.id AND t.remark = '演示交通支出'
);

INSERT INTO `transaction` (user_id, category_id, amount, type, record_date, remark)
SELECT u.id, c.id, 320.00, 2, '2026-05-12', '演示购物支出'
FROM app_user u JOIN category c ON c.user_id = u.id AND c.name = '购物' AND c.type = 2
WHERE u.username = 'admin' AND NOT EXISTS (
    SELECT 1 FROM `transaction` t WHERE t.user_id = u.id AND t.remark = '演示购物支出'
);
