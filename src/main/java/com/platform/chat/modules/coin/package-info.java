@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.core.modules.domainEvent",
                "chat.modules.domainEvent",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.modules.coin;

import org.springframework.modulith.ApplicationModule;