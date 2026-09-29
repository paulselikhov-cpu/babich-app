@ApplicationModule(
        allowedDependencies = {
                "auth::JwtService"
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.engines.webSocket;

import org.springframework.modulith.ApplicationModule;
