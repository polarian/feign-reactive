package reactivefeign.cloud;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE, classes = {
        AutoconfigurationViaConfigurationsTest.TestConfiguration.class})
@ActiveProfiles("test")
public class AutoconfigurationViaConfigurationsTest {

    @Autowired
    private FeignClientViaConfig feignClientViaConfig;

    @Test
    public void shouldUseFallback(){
        String result = feignClientViaConfig.get();
        int debug = 0;
    }

    @EnableFeignClients
    @Configuration
    public static class TestConfiguration {
    }

}
