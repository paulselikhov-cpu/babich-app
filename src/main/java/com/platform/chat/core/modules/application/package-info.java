@ApplicationModule(allowedDependencies = { "auth::entityUser", "auth::userRepository", "chat", "chat.core" })
package com.platform.chat.core.modules.application;

import org.springframework.modulith.ApplicationModule;