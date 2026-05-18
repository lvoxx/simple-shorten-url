package io.lvoxx.ssurl.common.config;

import io.lvoxx.ssurl.common.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.seruco.encoding.base62.Base62;

@Configuration
public class Base62Config {

    @Bean(name = Constants.Beans.BASE62)
    Base62 base62() {
        return Base62.createInstance();
    }
}
