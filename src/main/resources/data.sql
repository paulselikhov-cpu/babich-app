-- ===================================
-- Шаблоны локаций (начальные данные)
-- ON CONFLICT DO NOTHING = если запись уже есть — пропустить
-- это важно чтобы при каждом перезапуске Spring не дублировал данные
-- ===================================

-- Хрущевка — бесплатная, нерасширяемая
INSERT INTO location_templates (code, name, description, price_coin, is_extendable, extend_price_coin)
VALUES ('KHRUSHCHEVKA', 'Хрущевка', 'Уютная советская квартира', 0, false, null)
ON CONFLICT (code) DO NOTHING;

-- Частный дом — платный, расширяемый
INSERT INTO location_templates (code, name, description, price_coin, is_extendable, extend_price_coin)
VALUES ('PRIVATE_HOUSE', 'Частный дом', 'Просторный дом который можно достраивать', 100, true, 10)
ON CONFLICT (code) DO NOTHING;

-- ===================================
-- Комнаты шаблона Хрущевка
-- subquery вместо хардкода id — находим id шаблона по коду
-- ===================================
INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Кухня', 'Место для посиделок за чаем', 1
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Кухня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Зал', 'Главная комната', 2
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Зал'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Спальня', 'Тихое место для отдыха', 3
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Спальня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Туалет', 'Сами знаете', 4
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Туалет'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Прихожая', 'Вход в квартиру', 5
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Прихожая'
    );

-- ===================================
-- Комнаты шаблона Частный дом
-- ===================================
INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Кухня', 'Большая кухня', 1
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Кухня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Гостиная', 'Просторная гостиная', 2
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Гостиная'
    );

INSERT INTO template_rooms (template_id, name, description, order_index)
SELECT id, 'Двор', 'Место для отдыха на улице', 3
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Двор'
    );

-- ===================================
-- Дефолтные значения экономики в chat_users
-- (страховка на случай если Hibernate ещё не успел создать колонки —
--  в норме они уже есть за счёт полей в @Entity ChatUser)
-- ===================================
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS xp_user BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS energy INTEGER NOT NULL DEFAULT 100;
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS energy_last_updated TIMESTAMP;
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS game_status VARCHAR(20) NOT NULL DEFAULT 'HOMELESS';
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS profession VARCHAR(30) NOT NULL DEFAULT 'DVORNIK';

-- ===================================
-- Дефолтный район
-- ===================================
INSERT INTO districts (code, name, description)
VALUES ('CENTRAL', 'Центральный район', 'Первый район города с полным набором системных локаций')
ON CONFLICT (code) DO NOTHING;

-- ===================================
-- Системные локации (напрямую в locations, is_system = true)
-- ===================================

-- 1. Мэрия
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'CITY_HALL', 'Мэрия', 'Управление районом. Здесь решаются все административные вопросы.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'CITY_HALL'
    );

-- 2. Площадь Ленина
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'LENIN_SQUARE', 'Площадь Ленина', 'Место народных голосований и собраний. Здесь бомжи могут переночевать в комнате "Тёплые трубы".', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'LENIN_SQUARE'
    );

-- 3. Полицейский участок
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'POLICE_STATION', 'Полицейский участок', 'База civicRole. Сюда обращаются по вопросам правопорядка.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'POLICE_STATION'
    );

-- 4. Тюрьма
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'PRISON', 'Тюрьма', 'Место временного содержания нарушителей.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'PRISON'
    );

-- 5. Банк
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'BANK', 'Банк', 'Системная локация. Директор назначается мэрией. Вклады, ипотека и другие финансовые услуги.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'BANK'
    );

-- 6. Склад
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'WAREHOUSE', 'Склад', 'Доступен только владельцам бизнеса. Здесь можно закупить товары для своего бизнеса.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'WAREHOUSE'
    );

-- 7. Рынок (общий, "Авито")
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'GENERAL_MARKET', 'Рынок', 'Общий рынок района. Аренда прилавка доступна всем без участия мэрии.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'GENERAL_MARKET'
    );

-- 8. Рынок недвижимости
INSERT INTO locations (district_id, type, name, description, is_system, is_public, created_at)
SELECT d.id, 'REAL_ESTATE_MARKET', 'Рынок недвижимости', 'Купля-продажа локаций. Владелец назначается мэрией и берёт комиссию со сделок.', TRUE, FALSE, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.is_system = TRUE AND l.type = 'REAL_ESTATE_MARKET'
    );

-- ===================================
-- Комнаты для системных локаций
-- ===================================

-- Мэрия: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Приёмная', 'Место ожидания приёма', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмная');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Зал заседаний', 'Зал для совещаний', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Зал заседаний');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Кабинет губернатора', 'Личный кабинет главы района', true, false, 2
FROM locations l WHERE l.is_system = TRUE AND l.type = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет мэра');

-- Площадь Ленина: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Площадь', 'Открытое пространство для собраний', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'LENIN_SQUARE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Площадь');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Тёплые трубы', 'Место для ночлега бомжей', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'LENIN_SQUARE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Тёплые трубы');

-- Полицейский участок: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Дежурная часть', 'Главное помещение участка', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Дежурная часть');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Кабинет следователя', 'Место допросов', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет следователя');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Обходной лист', 'Помещение для оформления', true, false, 2
FROM locations l WHERE l.is_system = TRUE AND l.type = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Обходной лист');

-- Тюрьма: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Приёмный изолятор', 'Первичное размещение', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмный изолятор');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Камера', 'Общая камера', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Камера');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Комната свиданий', 'Место для встреч', true, false, 2
FROM locations l WHERE l.is_system = TRUE AND l.type = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Комната свиданий');

-- Банк: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Операционный зал', 'Главный зал банка', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Операционный зал');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Хранилище', 'Место хранения ценностей', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Хранилище');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Кабинет директора', 'Кабинет управляющего банком', true, false, 2
FROM locations l WHERE l.is_system = TRUE AND l.type = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет директора');

-- Склад: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Приёмка', 'Зона приёма товара', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмка');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Стеллажи', 'Основное хранилище', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Стеллажи');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Офис', 'Административное помещение', true, false, 2
FROM locations l WHERE l.is_system = TRUE AND l.type = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Офис');

-- Рынок: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Торговый зал', 'Основная торговая площадь', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'GENERAL_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Торговый зал');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Подсобка', 'Подсобное помещение', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'GENERAL_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Подсобка');

-- Рынок недвижимости: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Зал ожидания', 'Зона ожидания для клиентов', true, false, 0
FROM locations l WHERE l.is_system = TRUE AND l.type = 'REAL_ESTATE_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Зал ожидания');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index)
SELECT l.id, 'Кабинет риелтора', 'Офис для сделок с недвижимостью', true, false, 1
FROM locations l WHERE l.is_system = TRUE AND l.type = 'REAL_ESTATE_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет риелтора');
