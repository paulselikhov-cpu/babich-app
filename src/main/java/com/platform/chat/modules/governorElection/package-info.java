@ApplicationModule(
        allowedDependencies = {
                "chat.core.modules.poll",
                "chat.core.modules.scheduledCheck",
                "chat.core.modules.domainEvent",
                "chat.base.chatUser",
                "chat.base.location",
                "chat.base.district",
                "chat.modules.domainEvent",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.modules.governorElection;

import org.springframework.modulith.ApplicationModule;