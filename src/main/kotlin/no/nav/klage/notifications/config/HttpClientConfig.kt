package no.nav.klage.notifications.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(group = "leader-election")
class HttpClientConfig