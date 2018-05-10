/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactivefeign.cloud;

import com.netflix.client.RetryHandler;
import feign.Contract;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hystrix.FallbackFactory;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactivefeign.ReactiveHttpOptions;
import reactivefeign.ReactiveRetryPolicy;
import reactivefeign.client.statushandler.ReactiveStatusHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("feign.client")
@EqualsAndHashCode
public class ReactiveFeignClientConfigurationProperties {

	private boolean defaultToProperties = true;

	private String defaultConfig = "default";

	private Map<String, FeignClientConfiguration> config = new HashMap<>();

	public boolean isDefaultToProperties() {
		return defaultToProperties;
	}

	public void setDefaultToProperties(boolean defaultToProperties) {
		this.defaultToProperties = defaultToProperties;
	}

	public String getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(String defaultConfig) {
		this.defaultConfig = defaultConfig;
	}

	public Map<String, FeignClientConfiguration> getConfig() {
		return config;
	}

	public void setConfig(Map<String, FeignClientConfiguration> config) {
		this.config = config;
	}

	@EqualsAndHashCode
	public static class FeignClientConfiguration {

		private Class<ReactiveHttpOptions> httpOptions;

		private Class<ReactiveRetryPolicy> retryPolicy;

		private Class<ReactiveStatusHandler> statusHandler;

		private Class<RetryHandler> retryHandler;

		private Boolean decode404;

		private Class<Contract> contract;

		private Class<CloudReactiveFeign.SetterFactory> commandSetterFactory;

		private Class<FallbackFactory> fallbackFactory;

		private Class<?> fallback;

		public Class<ReactiveHttpOptions> getHttpOptions() {
			return httpOptions;
		}

		public void setHttpOptions(Class<ReactiveHttpOptions> httpOptions) {
			this.httpOptions = httpOptions;
		}

		public Class<ReactiveRetryPolicy> getRetryPolicy() {
			return retryPolicy;
		}

		public void setRetryPolicy(Class<ReactiveRetryPolicy> retryPolicy) {
			this.retryPolicy = retryPolicy;
		}

		public Class<ReactiveStatusHandler> getStatusHandler() {
			return statusHandler;
		}

		public void setStatusHandler(Class<ReactiveStatusHandler> statusHandler) {
			this.statusHandler = statusHandler;
		}

		public Class<RetryHandler> getRetryHandler() {
			return retryHandler;
		}

		public void setRetryHandler(Class<RetryHandler> retryHandler) {
			this.retryHandler = retryHandler;
		}

		public Boolean getDecode404() {
			return decode404;
		}

		public void setDecode404(Boolean decode404) {
			this.decode404 = decode404;
		}

		public Class<Contract> getContract() {
			return contract;
		}

		public void setContract(Class<Contract> contract) {
			this.contract = contract;
		}

		public Class<CloudReactiveFeign.SetterFactory> getCommandSetterFactory() {
			return commandSetterFactory;
		}

		public void setCommandSetterFactory(Class<CloudReactiveFeign.SetterFactory> commandSetterFactory) {
			this.commandSetterFactory = commandSetterFactory;
		}

		public Class<FallbackFactory> getFallbackFactory() {
			return fallbackFactory;
		}

		public void setFallbackFactory(Class<FallbackFactory> fallbackFactory) {
			this.fallbackFactory = fallbackFactory;
		}

		public Class<?> getFallback() {
			return fallback;
		}

		public void setFallback(Class<?> fallback) {
			this.fallback = fallback;
		}
	}

}
