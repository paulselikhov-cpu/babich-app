@ApplicationModule(
        allowedDependencies = {"chat.domain.chatUser"},
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.engines.notification;

import org.springframework.modulith.ApplicationModule;