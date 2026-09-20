@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.core.modules.domainEvent",
                "chat.core.modules.transactionLog",
                "chat.core.modules.notification",
                "chat.modules.coin",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.modules.domainEvent;

import org.springframework.modulith.ApplicationModule;