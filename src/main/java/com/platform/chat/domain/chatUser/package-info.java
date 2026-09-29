@ApplicationModule(
        allowedDependencies = {
                "chat.domain.location",
                "auth::userRepository",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.domain.chatUser;

import org.springframework.modulith.ApplicationModule;