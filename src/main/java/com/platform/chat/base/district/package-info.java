@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.location",
                "chat.core.modules.poll",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.district;

import org.springframework.modulith.ApplicationModule;