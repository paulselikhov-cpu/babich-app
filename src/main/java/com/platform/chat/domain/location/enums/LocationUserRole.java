package com.platform.chat.domain.location.enums;

/**
 * Роль персонажа внутри конкретной локации.
 * Не путать с глобальной ролью ChatRole!
 *
 * ChatRole.ADMIN = администратор всей платформы
 * LocationUserRole.OWNER = хозяин только своей локации
 */
public enum LocationUserRole {
    OWNER,      // создатель локации — полный контроль
    MODERATOR,  // "прописан" — может кикать, управлять комнатами
    MEMBER      // обычный участник
}