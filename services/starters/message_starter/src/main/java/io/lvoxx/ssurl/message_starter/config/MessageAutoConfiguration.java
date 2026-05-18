package io.lvoxx.ssurl.message_starter.config;

import io.lvoxx.ssurl.common.util.Constants;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

@AutoConfiguration
public class MessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(Constants.Messages.BUNDLE_ERRORS, Constants.Messages.BUNDLE_COMMON);
        messageSource.setDefaultEncoding(Constants.Messages.ENCODING);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
