@ApplicationModule(
        allowedDependencies = { "chat.domain.chatUser", "chat.domain.district", "chat.domain.location", "chat.engines.scheduledCheck", "chat.engines.domainEvent" },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.engines.poll;

import org.springframework.modulith.ApplicationModule;