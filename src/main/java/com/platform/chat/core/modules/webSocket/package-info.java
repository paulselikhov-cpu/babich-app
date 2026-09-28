@ApplicationModule(
        allowedDependencies = {
                "auth::JwtService"
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.webSocket;

import org.springframework.modulith.ApplicationModule;
