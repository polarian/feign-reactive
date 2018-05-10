/*
 * Copyright 2013-2016 the original author or authors.
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
import feign.hystrix.FallbackFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactivefeign.ReactiveHttpOptions;
import reactivefeign.ReactiveRetryPolicy;
import reactivefeign.client.statushandler.ReactiveStatusHandler;

import java.util.Objects;

import static reactivefeign.cloud.ReactiveFeignClientConfigurationProperties.FeignClientConfiguration;

@Data
@EqualsAndHashCode(callSuper = false)
class ReactiveFeignClientFactoryBean<T> implements FactoryBean<T>, InitializingBean,
		ApplicationContextAware {
	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
	 ***********************************/

	private Class<T> type;

	private String name;

	private String url;

	private String path;

	private boolean decode404;

	private ApplicationContext applicationContext;

	private Class<T> fallback;

	private Class<T> fallbackFactory;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.name, "Name must be set");
	}


	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	protected CloudReactiveFeign.Builder<T> feign(FeignContext context) {

		CloudReactiveFeign.Builder<T> builder = get(context, CloudReactiveFeign.Builder.class);

		configureFeign(context, builder);

		return builder;
	}

	protected void configureFeign(FeignContext context, CloudReactiveFeign.Builder<T> builder) {
		ReactiveFeignClientConfigurationProperties properties = applicationContext.getBean(ReactiveFeignClientConfigurationProperties.class);
		if (properties != null) {
			if (properties.isDefaultToProperties()) {
				configureUsingConfiguration(context, builder);
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.name), builder);
			} else {
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.name), builder);
				configureUsingConfiguration(context, builder);
			}
		} else {
			configureUsingConfiguration(context, builder);
		}
	}

	protected void configureUsingConfiguration(FeignContext context, CloudReactiveFeign.Builder<T> builder) {
		Contract contract = getOptional(context, Contract.class);
		if (contract != null) {
			builder.contract(contract);
		}
		ReactiveHttpOptions options = getOptional(context, ReactiveHttpOptions.class);
		if (options != null) {
			builder.options(options);
		}
		ReactiveRetryPolicy retryPolicy = getOptional(context, ReactiveRetryPolicy.class);
		if (retryPolicy != null) {
			builder.retryWhen(retryPolicy);
		}
		ReactiveStatusHandler statusHandler = getOptional(context, ReactiveStatusHandler.class);
		if (statusHandler != null) {
			builder.statusHandler(statusHandler);
		}
		if (decode404) {
			builder.decode404();
		}

		//load balance
		if (!StringUtils.hasText(this.url)) {
			RetryHandler retryHandler = getOptional(context, RetryHandler.class);
			if(retryHandler != null){
				builder.enableLoadBalancer(retryHandler);
			} else {
				builder.enableLoadBalancer();
			}
		}

		//hystrix
		CloudReactiveFeign.SetterFactory commandSetterFactory = getOptional(context, CloudReactiveFeign.SetterFactory.class);
		if(commandSetterFactory != null){
			builder.setHystrixCommandSetterFactory(commandSetterFactory);
		}
		if(fallbackFactory != null){
			FallbackFactory<? extends T> fallbackFactoryInstance = (FallbackFactory<? extends T>)
					getFromContext("fallbackFactory", name, context, fallbackFactory, FallbackFactory.class);
			builder.setFallbackFactory(fallbackFactoryInstance::create);
		}
		if(fallback != null){
			T fallbackInstance = getFromContext("fallback", name, context, fallback, type);
			builder.setFallback(fallbackInstance);
		}
	}

	protected void configureUsingProperties(FeignClientConfiguration config, CloudReactiveFeign.Builder<T> builder) {
		if (config == null) {
			return;
		}

		if (Objects.nonNull(config.getContract())) {
			builder.contract(getOrInstantiate(config.getContract()));
		}
		if (config.getHttpOptions() != null) {
			builder.options(getOrInstantiate(config.getHttpOptions()));
		}
		if (config.getRetryPolicy() != null) {
			builder.retryWhen(getOrInstantiate(config.getRetryPolicy()));
		}
		if (config.getStatusHandler() != null) {
			builder.statusHandler(getOrInstantiate(config.getStatusHandler()));
		}
		if (config.getDecode404() != null) {
			if (config.getDecode404()) {
				builder.decode404();
			}
		}

		//load balance
		if (!StringUtils.hasText(this.url)) {
			if (config.getRetryHandler() != null) {
				builder.enableLoadBalancer(getOrInstantiate(config.getRetryHandler()));
			} else {
				builder.enableLoadBalancer();
			}
		}

		//hystrix
		if(config.getCommandSetterFactory() != null){
			builder.setHystrixCommandSetterFactory(getOrInstantiate(config.getCommandSetterFactory()));
		}
		if(config.getFallbackFactory() != null){
			FallbackFactory fallbackFactory = getOrInstantiate(config.getFallbackFactory());
			builder.setFallbackFactory(throwable -> (T)fallbackFactory.create(throwable));
		}
		if(config.getFallback() != null){
			T fallback = (T)getOrInstantiate(config.getFallback());
			builder.setFallback(fallback);
		}
	}

	@Override
	public T getObject() throws Exception {
		FeignContext context = applicationContext.getBean(FeignContext.class);
		CloudReactiveFeign.Builder<T> builder = feign(context);
		return builder.target(type, targetUrl());
	}

	private String targetUrl() {
		String url;
		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			}
			else {
				url = this.name;
			}
			url += cleanPath();
		} else {
			if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
				this.url = "http://" + this.url;
			}
			url = this.url + cleanPath();
		}
		return url;
	}

	private String cleanPath() {
		String path = this.path.trim();
		if (StringUtils.hasLength(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	protected <T> T get(FeignContext context, Class<T> type) {
		T instance = context.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for "
					+ this.name);
		}
		return instance;
	}

	protected <T> T getOptional(FeignContext context, Class<T> type) {
		return context.getInstance(this.name, type);
	}

	private <T> T getFromContext(String fallbackMechanism, String feignClientName, FeignContext context,
								 Class<?> beanType, Class<T> targetType) {
		Object fallbackInstance = context.getInstance(feignClientName, beanType);
		if (fallbackInstance == null) {
			throw new IllegalStateException(String.format(
					"No " + fallbackMechanism + " instance of type %s found for feign client %s",
					beanType, feignClientName));
		}

		if (!targetType.isAssignableFrom(beanType)) {
			throw new IllegalStateException(
					String.format(
							"Incompatible " + fallbackMechanism + " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
							beanType, targetType, feignClientName));
		}
		return (T) fallbackInstance;
	}

	private <T> T getOrInstantiate(Class<T> tClass) {
		try {
			return applicationContext.getBean(tClass);
		} catch (NoSuchBeanDefinitionException e) {
			return BeanUtils.instantiateClass(tClass);
		}
	}


}
