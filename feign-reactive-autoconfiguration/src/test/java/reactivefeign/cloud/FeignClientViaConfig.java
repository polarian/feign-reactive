package reactivefeign.cloud;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;

@FeignClient(name = "test-feign", configuration = FeignClientViaConfig.TestConfiguration.class)
public interface FeignClientViaConfig {

    String get();

    class TestConfiguration {

        @Bean
        FeignClientViaConfig fallback(){
            return new FeignClientViaConfig() {
                @Override
                public String get() {
                    return "fallback value";
                }
            };
        }
    }

}
