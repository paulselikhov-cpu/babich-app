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
-- Миграция: добавление полей экономики в chat_users
-- Используем ALTER TABLE IF NOT EXISTS (работает только начиная с PostgreSQL 9.6+)
-- Если колонка уже есть — команда просто пропускается без ошибки
-- ===================================

-- xp_user
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS xp_user INTEGER NOT NULL DEFAULT 0;

-- energy
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS energy INTEGER NOT NULL DEFAULT 100;

-- energy_last_updated
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS energy_last_updated TIMESTAMP;

-- game_status
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS game_status VARCHAR(20) NOT NULL DEFAULT 'HOMELESS';

-- profession
ALTER TABLE chat_users ADD COLUMN IF NOT EXISTS profession VARCHAR(20) NOT NULL DEFAULT 'DVORNIK';

-- ===================================
-- Система районов (Districts) и общественных локаций
-- ===================================

-- Сначала создаём таблицу districts
CREATE TABLE IF NOT EXISTS districts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Создаём таблицу public_locations
CREATE TABLE IF NOT EXISTS public_locations (
    id BIGSERIAL PRIMARY KEY,
    district_id BIGINT NOT NULL REFERENCES districts(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Теперь добавляем поле district_id в locations (опционально — жилье может быть без района)
ALTER TABLE locations ADD COLUMN IF NOT EXISTS district_id BIGINT REFERENCES districts(id);

-- Добавить поле public_location_id в rooms (для системных локаций)
-- location_id теперь опциональный (либо location_id, либо public_location_id)
ALTER TABLE rooms ALTER COLUMN location_id DROP NOT NULL;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS public_location_id BIGINT REFERENCES public_locations(id);

-- ===================================
-- Начальные данные: дефолтный район + системные локации
-- ===================================

-- Создать дефолтный район "Центральный"
INSERT INTO districts (code, name, description)
VALUES ('CENTRAL', 'Центральный район', 'Первый район города с полным набором системных локаций')
    ON CONFLICT (code) DO NOTHING;

-- Создать 8 системных локаций для дефолтного района (согласно концепции, раздел 8)

-- 1. Мэрия
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'CITY_HALL', 'Мэрия', 'Управление районом. Здесь решаются все административные вопросы.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'CITY_HALL'
    );

-- 2. Площадь Ленина (народные голосования + комната "Тёплые трубы" для бомжей)
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'LENIN_SQUARE', 'Площадь Ленина', 'Место народных голосований и собраний. Здесь бомжи могут переночевать в комнате "Тёплые трубы".'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'LENIN_SQUARE'
    );

-- 3. Полицейский участок
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'POLICE_STATION', 'Полицейский участок', 'База civicRole. Сюда обращаются по вопросам правопорядка.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'POLICE_STATION'
    );

-- 4. Тюрьма
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'PRISON', 'Тюрьма', 'Место временного содержания нарушителей.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'PRISON'
    );

-- 5. Банк
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'BANK', 'Банк', 'Системная локация. Директор назначается мэрией. Вклады, ипотека и другие финансовые услуги.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'BANK'
    );

-- 6. Склад
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'WAREHOUSE', 'Склад', 'Доступен только владельцам бизнеса. Здесь можно закупить товары для своего бизнеса.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'WAREHOUSE'
    );

-- 7. Рынок (общий, "Авито")
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'GENERAL_MARKET', 'Рынок', 'Общий рынок района. Аренда прилавка доступна всем без участия мэрии.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'GENERAL_MARKET'
    );

-- 8. Рынок недвижимости
INSERT INTO public_locations (district_id, type, name, description)
SELECT id, 'REAL_ESTATE_MARKET', 'Рынок недвижимости', 'Купля-продажа локаций. Владелец назначается мэрией и берёт комиссию со сделок.'
FROM districts WHERE code = 'CENTRAL'
                  AND NOT EXISTS (
        SELECT 1 FROM public_locations pl
                  JOIN districts d ON pl.district_id = d.id
        WHERE d.code = 'CENTRAL' AND pl.type = 'REAL_ESTATE_MARKET'
    );

-- ===================================
-- Миграция 2026-07-25: объединение PublicLocation в Location
-- ===================================

-- Добавляем колонки в locations
ALTER TABLE locations ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE locations ADD COLUMN IF NOT EXISTS type VARCHAR(30);

-- Делаем district_id NOT NULL (теперь каждая локация привязана к району)
-- Сначала для существующих пользовательских локаций без района проставляем дефолтный
UPDATE locations SET district_id = (SELECT id FROM districts WHERE code = 'CENTRAL' LIMIT 1)
WHERE district_id IS NULL;
ALTER TABLE locations ALTER COLUMN district_id SET NOT NULL;

-- Делаем owner_id nullable (у системных локаций нет владельца)
ALTER TABLE locations ALTER COLUMN owner_id DROP NOT NULL;

-- Переносим данные из public_locations в locations
INSERT INTO locations (district_id, name, description, is_system, type, is_public, created_at)
SELECT pl.district_id, pl.name, pl.description, TRUE, pl.type, FALSE, NOW()
FROM public_locations pl
WHERE NOT EXISTS (
    SELECT 1 FROM locations l 
    WHERE l.district_id = pl.district_id AND l.type = pl.type AND l.is_system = TRUE
);

-- Обновляем rooms: переносим public_location_id → location_id
UPDATE rooms r
SET location_id = (
    SELECT l.id FROM locations l 
    JOIN public_locations pl ON pl.district_id = l.district_id AND pl.type = l.type
    WHERE pl.id = r.public_location_id AND l.is_system = TRUE
)
WHERE r.public_location_id IS NOT NULL;

-- Делаем location_id NOT NULL в rooms
ALTER TABLE rooms ALTER COLUMN location_id SET NOT NULL;

-- Удаляем public_location_id из rooms (больше не нужен)
ALTER TABLE rooms DROP COLUMN IF EXISTS public_location_id;

-- Удаляем таблицу public_locations
DROP TABLE IF EXISTS public_locations;

-- ===================================
-- Создаём комнаты для системных локаций (перенесено из DataInitializer)
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
SELECT l.id, 'Кабинет мэра', 'Личный кабинет главы района', true, false, 2
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
