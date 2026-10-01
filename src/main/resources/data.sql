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
INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Кухня', 'Место для посиделок за чаем', 1, 'DEFAULT'
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Кухня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Зал', 'Главная комната', 2, 'DEFAULT'
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Зал'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Спальня', 'Тихое место для отдыха', 3, 'DEFAULT'
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Спальня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Туалет', 'Сами знаете', 4, 'DEFAULT'
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Туалет'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Прихожая', 'Вход в квартиру', 5, 'DEFAULT'
FROM location_templates WHERE code = 'KHRUSHCHEVKA'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'KHRUSHCHEVKA' AND tr.name = 'Прихожая'
    );

-- ===================================
-- Комнаты шаблона Частный дом
-- ===================================
INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Кухня', 'Большая кухня', 1, 'DEFAULT'
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Кухня'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Гостиная', 'Просторная гостиная', 2, 'DEFAULT'
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Гостиная'
    );

INSERT INTO template_rooms (template_id, name, description, order_index, code)
SELECT id, 'Двор', 'Место для отдыха на улице', 3, 'DEFAULT'
FROM location_templates WHERE code = 'PRIVATE_HOUSE'
                          AND NOT EXISTS (
        SELECT 1 FROM template_rooms tr
                          JOIN location_templates lt ON tr.template_id = lt.id
        WHERE lt.code = 'PRIVATE_HOUSE' AND tr.name = 'Двор'
    );

-- ===================================
-- Дефолтный район
-- ===================================
INSERT INTO districts (code, name, description)
VALUES ('CENTRAL', 'Центральный район', 'Первый район города с полным набором системных локаций')
    ON CONFLICT (code) DO NOTHING;

-- ===================================
-- Системные локации: type = 'SYSTEM', конкретный вид — в колонке code.
-- Это те же коды, что в SystemLocationCode (бэкенд). Уникальность кода —
-- составная (district_id, code): у каждого района своя «Площадь Ленина».
-- ===================================

-- 1. Мэрия
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'CITY_HALL', 'SYSTEM', 'Мэрия', 'Управление районом. Здесь решаются все административные вопросы.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'CITY_HALL'
    );

-- 2. Площадь Ленина
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'LENIN_SQUARE', 'SYSTEM', 'Площадь Ленина', 'Место народных голосований и собраний. Здесь бомжи могут переночевать в комнате "Тёплые трубы".', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'LENIN_SQUARE'
    );

-- 3. Полицейский участок
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'POLICE_STATION', 'SYSTEM', 'Полицейский участок', 'База civic_role. Сюда обращаются по вопросам правопорядка.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'POLICE_STATION'
    );

-- 4. Тюрьма
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'PRISON', 'SYSTEM', 'Тюрьма', 'Место временного содержания нарушителей.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'PRISON'
    );

-- 5. Банк
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'BANK', 'SYSTEM', 'Банк', 'Системная локация. Директор назначается мэрией. Вклады, ипотека и другие финансовые услуги.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'BANK'
    );

-- 6. Склад
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'WAREHOUSE', 'SYSTEM', 'Склад', 'Доступен только владельцам бизнеса. Здесь можно закупить товары для своего бизнеса.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'WAREHOUSE'
    );

-- 7. Магазин
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'GENERAL_MARKET', 'SYSTEM', 'Магазин', 'Главный магазин района', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'GENERAL_MARKET'
    );

-- 8. Рынок недвижимости
INSERT INTO locations (district_id, code, type, name, description, is_public, xp_location, created_at)
SELECT d.id, 'REAL_ESTATE_MARKET', 'SYSTEM', 'Рынок недвижимости', 'Купля-продажа локаций. Владелец назначается мэрией и берёт комиссию со сделок.', FALSE, 0, NOW()
FROM districts d WHERE d.code = 'CENTRAL'
                   AND NOT EXISTS (
        SELECT 1 FROM locations l WHERE l.district_id = d.id AND l.code = 'REAL_ESTATE_MARKET'
    );

-- ===================================
-- Комнаты для системных локаций
-- ===================================

-- Мэрия: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Приёмная', 'Место ожидания приёма', true, false, 0, 'RECEPTION'
FROM locations l WHERE l.code = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмная');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Зал заседаний', 'Зал для совещаний', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Зал заседаний');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Кабинет губернатора', 'Личный кабинет главы района', true, false, 2, 'GOVERNORS_OFFICE'
FROM locations l WHERE l.code = 'CITY_HALL'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет губернатора');

-- Площадь Ленина: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Площадь', 'Открытое пространство для собраний', true, false, 0, 'LENIN_SQUARE'
FROM locations l WHERE l.code = 'LENIN_SQUARE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Площадь');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Тёплые трубы', 'Место для ночлега бомжей', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'LENIN_SQUARE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Тёплые трубы');

-- Полицейский участок: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Дежурная часть', 'Главное помещение участка', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Дежурная часть');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Кабинет следователя', 'Место допросов', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет следователя');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Обходной лист', 'Помещение для оформления', true, false, 2, 'DEFAULT'
FROM locations l WHERE l.code = 'POLICE_STATION'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Обходной лист');

-- Тюрьма: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Приёмный изолятор', 'Первичное размещение', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмный изолятор');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Камера', 'Общая камера', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Камера');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Комната свиданий', 'Место для встреч', true, false, 2, 'DEFAULT'
FROM locations l WHERE l.code = 'PRISON'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Комната свиданий');

-- Банк: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Операционный зал', 'Главный зал банка', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Операционный зал');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Хранилище', 'Место хранения ценностей', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Хранилище');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Кабинет директора', 'Кабинет управляющего банком', true, false, 2, 'DEFAULT'
FROM locations l WHERE l.code = 'BANK'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет директора');

-- Склад: 3 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Приёмка', 'Зона приёма товара', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Приёмка');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Стеллажи', 'Основное хранилище', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Стеллажи');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Офис', 'Административное помещение', true, false, 2, 'DEFAULT'
FROM locations l WHERE l.code = 'WAREHOUSE'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Офис');

-- Магазин: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Торговый зал', 'Основная торговая площадь', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'GENERAL_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Торговый зал');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Подсобка', 'Подсобное помещение', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'GENERAL_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Подсобка');

-- Рынок недвижимости: 2 комнаты
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Зал ожидания', 'Зона ожидания для клиентов', true, false, 0, 'DEFAULT'
FROM locations l WHERE l.code = 'REAL_ESTATE_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Зал ожидания');
INSERT INTO rooms (location_id, name, description, store_history, is_custom, order_index, code)
SELECT l.id, 'Кабинет риелтора', 'Офис для сделок с недвижимостью', true, false, 1, 'DEFAULT'
FROM locations l WHERE l.code = 'REAL_ESTATE_MARKET'
                   AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.location_id = l.id AND r.name = 'Кабинет риелтора');
