package io.lvoxx.ssurl.common.exception;

public class DomainBlacklistedException extends RuntimeException {
    public DomainBlacklistedException(String domain) {
        super("Domain is blacklisted: " + domain);
    }
}
