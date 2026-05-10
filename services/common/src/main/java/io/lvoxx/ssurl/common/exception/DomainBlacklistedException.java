package io.lvoxx.ssurl.common.exception;

public class DomainBlacklistedException extends AppException {
    public DomainBlacklistedException(String domain) {
        super("Domain is blacklisted: " + domain, domain);
    }
}
