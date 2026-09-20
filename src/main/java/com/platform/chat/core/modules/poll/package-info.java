@ApplicationModule(
        allowedDependencies = { "chat.base.chatUser", "chat.base.district", "chat.base.location", "chat.core.modules.scheduledCheck", "chat.core.modules.domainEvent", "chat.modules.domainEvent" },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.poll;

import org.springframework.modulith.ApplicationModule;