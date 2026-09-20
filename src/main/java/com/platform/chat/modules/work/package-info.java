@ApplicationModule(allowedDependencies = {
        "chat.modules.coin",
        "chat.base.chatUser",
}, type = ApplicationModule.Type.OPEN)
package com.platform.chat.modules.work;

import org.springframework.modulith.ApplicationModule;
