package com.platform.chat.enums;

/**
 * Роль персонажа внутри конкретной локации.
 * Не путать с глобальной ролью ChatRole!
 *
 * ChatRole.ADMIN = администратор всей платформы
 * LocationMemberRole.OWNER = хозяин только своей локации
 */
public enum LocationMemberRole {
    OWNER,      // создатель локации — полный контроль
    MODERATOR,  // "прописан" — может кикать, управлять комнатами
    MEMBER      // обычный участник
}