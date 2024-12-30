/*
 *    Copyright 2009-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

	private final Configuration config;

	/**
	 * 记录 Mapper 接口与对应 MapperProxyFactory 的映射关系
	 */
	private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new ConcurrentHashMap<>();

	public MapperRegistry(Configuration config) {
		this.config = config;
	}

	/**
	 * 获取 Mapper 接口对象
	 */
	@SuppressWarnings("unchecked")
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		// 获取指定 type 对应的 MapperProxyFactory 对象
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
		// 如果 mapperProxyFactory 为空, 则抛出异常
		if (mapperProxyFactory == null) {
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			// 创建实现了 type 接口的代理对象
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}

	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	/**
	 * 添加 Mapper 映射器
	 *
	 * @param type 映射器
	 * @param <T>  泛型
	 */
	public <T> void addMapper(Class<T> type) {
		// 判断 type 是否为接口
		if (type.isInterface()) {
			// 判断是否已经存在
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			// 加载完成标识
			boolean loadCompleted = false;
			try {
				// 将 Mapper 接口对应的 Class 对象和 MapperProxyFactory 对象添加到 knownMappers 集合中
				// Map<CLass<?>, MapperProxyFactory<?>> 存放的是接口类型, 和对应的工厂类的关系
				knownMappers.put(type, new MapperProxyFactory<>(type));
				// It's important that the type is added before the parser is run
				// otherwise the binding may automatically be attempted by the
				// mapper parser. If the type is already known, it won't try.
				// 创建 MapperAnnotationBuilder 对象用于解析
				MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				// 根据接口, 开始解析所有方法上的注解, 如: @Select、@Insert ... 等
				parser.parse();
				loadCompleted = true;
			} finally {
				// 如果加载过程中出现异常需要将这个 mapper 从 mybatis 中移除
				if (!loadCompleted) {
					knownMappers.remove(type);
				}
			}
		}
	}

	/**
	 * Gets the mappers.
	 *
	 * @return the mappers
	 * @since 3.2.2
	 */
	public Collection<Class<?>> getMappers() {
		return Collections.unmodifiableCollection(knownMappers.keySet());
	}

	/**
	 * Adds the mappers.
	 *
	 * @param packageName the package name
	 * @param superType   the super type
	 * @since 3.2.2
	 */
	public void addMappers(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		for (Class<?> mapperClass : mapperSet) {
			addMapper(mapperClass);
		}
	}

	/**
	 * Adds the mappers.
	 *
	 * @param packageName the package name
	 * @since 3.2.2
	 */
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}

}
