@ApplicationModule(
        allowedDependencies = {
                "chat.domain.chatUser",
                "chat.domain.location",
                "chat.engines.domainEvent",
                "chat.engines.transactionLog",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.engines.economy;

import org.springframework.modulith.ApplicationModule;