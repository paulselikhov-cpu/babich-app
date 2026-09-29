@ApplicationModule(
        allowedDependencies = {
                "chat.domain.chatUser",
                "chat.domain.location",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.domain.district;

import org.springframework.modulith.ApplicationModule;