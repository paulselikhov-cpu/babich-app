@ApplicationModule(
        allowedDependencies = {"chat.modules.domainEvent"},
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.domainEvent;

import org.springframework.modulith.ApplicationModule;