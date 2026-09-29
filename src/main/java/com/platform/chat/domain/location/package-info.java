@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.room",
                "chat.base.district",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.location;

import org.springframework.modulith.ApplicationModule;