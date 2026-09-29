@ApplicationModule(allowedDependencies = {
        "chat.modules.coin",
        "chat.core.modules.transactionLog",
        "chat.base.chatUser",
}, type = ApplicationModule.Type.OPEN)
package com.platform.chat.modules.work;

import org.springframework.modulith.ApplicationModule;
