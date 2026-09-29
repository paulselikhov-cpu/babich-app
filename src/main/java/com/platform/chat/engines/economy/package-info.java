@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.location",
                "chat.core.modules.domainEvent",
                "chat.core.modules.transactionLog",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.modules.coin;

import org.springframework.modulith.ApplicationModule;