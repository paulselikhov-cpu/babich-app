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
-- Создать базовые комнаты для системных локаций
-- ===================================

-- Комната "Холл" для Мэрии
INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Холл мэрии', 'Главное помещение мэрии', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'CITY_HALL'
  AND NOT EXISTS (
        SELECT 1 FROM rooms r
        WHERE r.public_location_id = pl.id AND r.name = 'Холл мэрии'
    );

-- Комната "Площадь" для Площади Ленина
INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Площадь', 'Открытое пространство для собраний', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'LENIN_SQUARE'
  AND NOT EXISTS (
        SELECT 1 FROM rooms r
        WHERE r.public_location_id = pl.id AND r.name = 'Площадь'
    );

-- Комната "Тёплые трубы" для бомжей на Площади Ленина
INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Тёплые трубы', 'Место для ночлега бомжей. Здесь можно восстановить энергию сном.', true, false, 2
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'LENIN_SQUARE'
  AND NOT EXISTS (
        SELECT 1 FROM rooms r
        WHERE r.public_location_id = pl.id AND r.name = 'Тёплые трубы'
    );

-- Комнаты для остальных системных локаций (по одной базовой комнате)
INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Дежурная часть', 'Главное помещение участка', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'POLICE_STATION'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);

INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Общая камера', 'Место содержания заключённых', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'PRISON'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);

INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Операционный зал', 'Главный зал банка', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'BANK'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);

INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Склад', 'Хранилище товаров для бизнеса', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'WAREHOUSE'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);

INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Торговые ряды', 'Общий рынок района', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'GENERAL_MARKET'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);

INSERT INTO rooms (public_location_id, name, description, store_history, is_custom, order_index)
SELECT pl.id, 'Офис продаж', 'Оформление сделок с недвижимостью', true, false, 1
FROM public_locations pl
         JOIN districts d ON pl.district_id = d.id
WHERE d.code = 'CENTRAL' AND pl.type = 'REAL_ESTATE_MARKET'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.public_location_id = pl.id);
