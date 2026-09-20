@ApplicationModule(
        allowedDependencies = {"chat.modules.coin"},
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.transactionLog;

import org.springframework.modulith.ApplicationModule;