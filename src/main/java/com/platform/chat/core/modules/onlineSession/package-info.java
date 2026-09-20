@ApplicationModule(
        allowedDependencies = {"chat.base.chatUser", "chat.base.room", "chat.base.location", "chat.core.modules.message", "chat.core.modules.scheduledCheck"},
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.onlineSession;

import org.springframework.modulith.ApplicationModule;