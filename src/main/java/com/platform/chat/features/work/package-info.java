@ApplicationModule(allowedDependencies = {
        "chat.engines.economy",
        "chat.engines.transactionLog",
        "chat.domain.chatUser",
}, type = ApplicationModule.Type.OPEN)
package com.platform.chat.features.work;

import org.springframework.modulith.ApplicationModule;
