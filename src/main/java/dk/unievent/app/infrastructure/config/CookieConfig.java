package dk.unievent.app.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "unievent.security.cookies")
@Validated
public class CookieConfig {

    @NotBlank
    private String accessName;

    @NotBlank
    private String refreshName;

    @NotBlank
    private String csrfName;

    @Positive
    private int accessMaxAge;

    @Positive
    private int refreshMaxAge;

    @Positive
    private int csrfMaxAge;

    private boolean secure = true;

    @NotBlank
    private String sameSite = "Strict";

    private String domain = "";

    @NotBlank
    private String path = "/";

    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String getRefreshName() {
        return refreshName;
    }

    public void setRefreshName(String refreshName) {
        this.refreshName = refreshName;
    }

    public String getCsrfName() {
        return csrfName;
    }

    public void setCsrfName(String csrfName) {
        this.csrfName = csrfName;
    }

    public int getAccessMaxAge() {
        return accessMaxAge;
    }

    public void setAccessMaxAge(int accessMaxAge) {
        this.accessMaxAge = accessMaxAge;
    }

    public int getRefreshMaxAge() {
        return refreshMaxAge;
    }

    public void setRefreshMaxAge(int refreshMaxAge) {
        this.refreshMaxAge = refreshMaxAge;
    }

    public int getCsrfMaxAge() {
        return csrfMaxAge;
    }

    public void setCsrfMaxAge(int csrfMaxAge) {
        this.csrfMaxAge = csrfMaxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
