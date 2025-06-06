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

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.util.MapUtil;

/**
 * 映射器代理类
 * <p>
 * 实现 InvocationHandler 实现动态代理
 * <p>
 * 通过 MapperProxy 代理类包装对数据库的操作, 当前类的创建通过 MapperProxyFactory#newInstance 创建
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = -4724728412955527868L;
	private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
			                                         | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
	private static final Constructor<Lookup> lookupConstructor;
	private static final Method privateLookupInMethod;

	/**
	 * 会话对象 SqlSession
	 */
	private final SqlSession sqlSession;

	/**
	 * mapper 接口对应的 Class 对象
	 */
	private final Class<T> mapperInterface;

	/**
	 * 用于缓存 MapperMethod 对象,
     * key 是 mapper 接口中方法对应的 Method 对象
     * value 是对应的 MapperMethod 对象
     * MapperMethod 对象会完成 SQL 参数转换及 SQL
     *
	 * 语句的执行功能
	 */
	private final Map<Method, MapperMethodInvoker> methodCache;

	/**
	 * 构造函数
	 */
	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}

	static {
		Method privateLookupIn;
		try {
			privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
		} catch (NoSuchMethodException e) {
			privateLookupIn = null;
		}
		privateLookupInMethod = privateLookupIn;

		Constructor<Lookup> lookup = null;
		if (privateLookupInMethod == null) {
			// JDK 1.8
			try {
				lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
				lookup.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(
						"There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.",
						e);
			} catch (Exception e) {
				lookup = null;
			}
		}
		lookupConstructor = lookup;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			// 如果目标方法为 Object 中的方法(toString、hasCode、equals、getClass...), 则直接调用目标方法
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			}

			return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}

	private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
		try {
			return MapUtil.computeIfAbsent(methodCache, method, m -> {
				// 判断方法是否是接口中默认实现的方法, 否则进入
				if (!m.isDefault()) {
					return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
				}
				try {
					if (privateLookupInMethod == null) {
						return new DefaultMethodInvoker(getMethodHandleJava8(method));
					}
					return new DefaultMethodInvoker(getMethodHandleJava9(method));
				} catch (IllegalAccessException | InstantiationException | InvocationTargetException
				         | NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (RuntimeException re) {
			Throwable cause = re.getCause();
			throw cause == null ? re : cause;
		}
	}

	private MethodHandle getMethodHandleJava9(Method method)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		final Class<?> declaringClass = method.getDeclaringClass();
		return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
				declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
				declaringClass);
	}

	private MethodHandle getMethodHandleJava8(Method method)
			throws IllegalAccessException, InstantiationException, InvocationTargetException {
		final Class<?> declaringClass = method.getDeclaringClass();
		return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES).unreflectSpecial(method, declaringClass);
	}

	interface MapperMethodInvoker {
		Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
	}

	private static class PlainMethodInvoker implements MapperMethodInvoker {
		private final MapperMethod mapperMethod;

		public PlainMethodInvoker(MapperMethod mapperMethod) {
			this.mapperMethod = mapperMethod;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
			return mapperMethod.execute(sqlSession, args);
		}
	}

	private static class DefaultMethodInvoker implements MapperMethodInvoker {
		private final MethodHandle methodHandle;

		public DefaultMethodInvoker(MethodHandle methodHandle) {
			this.methodHandle = methodHandle;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
			return methodHandle.bindTo(proxy).invokeWithArguments(args);
		}
	}
}
