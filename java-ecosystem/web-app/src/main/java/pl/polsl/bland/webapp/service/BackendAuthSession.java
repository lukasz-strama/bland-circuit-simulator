package pl.polsl.bland.webapp.service;

import com.vaadin.flow.spring.scopes.VaadinSessionScope;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import pl.polsl.bland.models.UserDto;

@Service
@Scope(scopeName = VaadinSessionScope.VAADIN_SESSION_SCOPE_NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BackendAuthSession {
    private String token;
    private UserDto user;

    public boolean isAuthenticated() {
        return token != null && !token.isBlank() && user != null;
    }

    public String token() {
        return token;
    }

    public UserDto user() {
        return user;
    }

    public void authenticate(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public void clear() {
        token = null;
        user = null;
    }
}
