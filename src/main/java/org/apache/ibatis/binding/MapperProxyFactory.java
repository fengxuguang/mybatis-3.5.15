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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperProxy.MapperMethodInvoker;
import org.apache.ibatis.session.SqlSession;

/**
 * Mapper 代理工厂
 *
 * 通过 MapperProxyFactory#newInstance 来创建 MapperProxy 代理类, 对外提供实例化 MapperProxy 对象的操作
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

	/**
	 * mapper 接口类
	 */
	private final Class<T> mapperInterface;
	private final Map<Method, MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();

	public MapperProxyFactory(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	public Class<T> getMapperInterface() {
		return mapperInterface;
	}

	public Map<Method, MapperMethodInvoker> getMethodCache() {
		return methodCache;
	}

	/**
	 * 使用 JDK 动态代理完成接口代理对象的创建
	 */
	@SuppressWarnings("unchecked")
	protected T newInstance(MapperProxy<T> mapperProxy) {
		// 创建实现了 MapperInterface 接口的代理对象
		return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, mapperProxy);
	}

	/**
	 * 创建接口代理对象
	 */
	public T newInstance(SqlSession sqlSession) {
		// 创建 MapperProxy 对象, 每次调用都会创建新的 MapperProxy 对象
		final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
		return newInstance(mapperProxy);
	}

}
