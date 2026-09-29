@ApplicationModule(
        allowedDependencies = {
                "chat.engines.poll",
                "chat.engines.scheduledCheck",
                "chat.engines.domainEvent",
                "chat.domain.chatUser",
                "chat.domain.location",
                "chat.domain.district",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.features.governorElection;

import org.springframework.modulith.ApplicationModule;