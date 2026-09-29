@ApplicationModule(
        allowedDependencies = {
                "chat.base.location",
                "auth::userRepository",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.chatUser;

import org.springframework.modulith.ApplicationModule;