@ApplicationModule(
        allowedDependencies = {
                "chat.domain.chatUser",
                "chat.domain.room",
                "chat.domain.district",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.domain.location;

import org.springframework.modulith.ApplicationModule;