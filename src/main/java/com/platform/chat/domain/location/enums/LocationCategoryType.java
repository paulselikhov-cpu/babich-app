package com.platform.chat.domain.location.enums;

/**
 * Категория локации. В отличие от {@link PublicLocationType} (системные типы),
 * категория проставляется всем локациям при создании.
 * Системным локациям категория проставляется при инициализации района.
 */
public enum LocationCategoryType {
    BUSINESS,   // бизнес-локация (заведение)
    HOME        // жилая локация (квартира)
}