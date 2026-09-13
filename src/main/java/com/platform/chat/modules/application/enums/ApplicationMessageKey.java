package com.platform.chat.modules.application.enums;

import lombok.Getter;

@Getter
public enum ApplicationMessageKey {
    CHARACTER_NOT_FOUND("application.character.not-found"),
    PASSPORT_ALREADY_EXISTS("application.passport.already-exists"),
    PASSPORT_ISSUED("application.passport.issued"),
    WORK_LICENCE_ALREADY_EXISTS("application.work-licence.already-exists"),
    WORK_LICENCE_ISSUED("application.work-licence.issued"),
    CANCELED("application.canceled"),
    APPLICATION_ALREADY_PENDING("application.already-pending");

    private final String code;

    ApplicationMessageKey(String code) {
        this.code = code;
    }

}
