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