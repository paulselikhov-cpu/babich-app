@ApplicationModule(
        allowedDependencies = {
                "chat.base.location",
                "chat.base.chatUser",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.room;

import org.springframework.modulith.ApplicationModule;