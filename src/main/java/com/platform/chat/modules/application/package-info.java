@ApplicationModule(allowedDependencies = { "auth::entityUser", "auth::userRepository", "chat", "chat.core" })
package com.platform.chat.modules.application;

import org.springframework.modulith.ApplicationModule;