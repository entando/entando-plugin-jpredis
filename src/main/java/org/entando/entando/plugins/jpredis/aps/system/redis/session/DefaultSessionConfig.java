package org.entando.entando.plugins.jpredis.aps.system.redis.session;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSessionActive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RedisSessionActive(false)
public class DefaultSessionConfig {

    @Bean
    public Filter springSessionRepositoryFilter() {
        // When Redis session is not active the springSessionRepositoryFilter
        // is replaced with a no-op filter
        return new Filter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                    FilterChain filterChain) throws IOException, ServletException {
                filterChain.doFilter(servletRequest, servletResponse);
            }

            @Override
            public void destroy() {
            }
        };
    }
}
