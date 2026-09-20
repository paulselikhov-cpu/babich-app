@ApplicationModule(
        allowedDependencies = {"chat.base.chatUser"},
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.notification;

import org.springframework.modulith.ApplicationModule;