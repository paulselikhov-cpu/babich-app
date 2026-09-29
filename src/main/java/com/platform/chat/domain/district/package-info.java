@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.location",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.district;

import org.springframework.modulith.ApplicationModule;