@ApplicationModule(
        allowedDependencies = {
                "auth::entityUser",
                "auth::userRepository",
                "chat.core.modules.domainEvent",
                "chat.modules.application::ApplicationType",
                "chat.modules.domainEvent" },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.application;

import org.springframework.modulith.ApplicationModule;