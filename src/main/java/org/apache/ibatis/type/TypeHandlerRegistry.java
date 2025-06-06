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
package org.apache.ibatis.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;

/**
 * 类型注册器
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public final class TypeHandlerRegistry {

    /**
     * JdbcType - TypeHandler 对象
     * 用于将 Jdbc 类型转为 Java 类型
     */
	private final Map<JdbcType, TypeHandler<?>> jdbcTypeHandlerMap = new EnumMap<>(JdbcType.class);

	/**
	 * 记录了 Java 类型向指定 JdbcType 转换时, 需要使用 TypeHandler 对象
     * 用于将 Java 类型转为指定的 Jdbc 类型
	 */
	private final Map<Type, Map<JdbcType, TypeHandler<?>>> typeHandlerMap = new ConcurrentHashMap<>();
	private final TypeHandler<Object> unknownTypeHandler;

	/**
	 * 记录全部 TypeHandler 类型以及该类型相关的 TypeHandler 对象
	 */
	private final Map<Class<?>, TypeHandler<?>> allTypeHandlersMap = new HashMap<>();

	/**
	 * 空 TypeHandler 集合的标识
	 */
	private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();

	private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;

	/**
	 * The default constructor.
	 */
	public TypeHandlerRegistry() {
		this(new Configuration());
	}

	/**
	 * The constructor that pass the MyBatis configuration.
	 *
	 * @param configuration a MyBatis configuration
	 * @since 3.5.4
	 */
	public TypeHandlerRegistry(Configuration configuration) {
		this.unknownTypeHandler = new UnknownTypeHandler(configuration);

		register(Boolean.class, new BooleanTypeHandler());
		register(boolean.class, new BooleanTypeHandler());
		register(JdbcType.BOOLEAN, new BooleanTypeHandler());
		register(JdbcType.BIT, new BooleanTypeHandler());

		register(Byte.class, new ByteTypeHandler());
		register(byte.class, new ByteTypeHandler());
		register(JdbcType.TINYINT, new ByteTypeHandler());

		register(Short.class, new ShortTypeHandler());
		register(short.class, new ShortTypeHandler());
		register(JdbcType.SMALLINT, new ShortTypeHandler());

		register(Integer.class, new IntegerTypeHandler());
		register(int.class, new IntegerTypeHandler());
		register(JdbcType.INTEGER, new IntegerTypeHandler());

		register(Long.class, new LongTypeHandler());
		register(long.class, new LongTypeHandler());

		register(Float.class, new FloatTypeHandler());
		register(float.class, new FloatTypeHandler());
		register(JdbcType.FLOAT, new FloatTypeHandler());

		register(Double.class, new DoubleTypeHandler());
		register(double.class, new DoubleTypeHandler());
		register(JdbcType.DOUBLE, new DoubleTypeHandler());

		register(Reader.class, new ClobReaderTypeHandler());
		register(String.class, new StringTypeHandler());
		register(String.class, JdbcType.CHAR, new StringTypeHandler());
		register(String.class, JdbcType.CLOB, new ClobTypeHandler());
		register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
		register(String.class, JdbcType.LONGVARCHAR, new StringTypeHandler());
		register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
		register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
		register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
		register(JdbcType.CHAR, new StringTypeHandler());
		register(JdbcType.VARCHAR, new StringTypeHandler());
		register(JdbcType.CLOB, new ClobTypeHandler());
		register(JdbcType.LONGVARCHAR, new StringTypeHandler());
		register(JdbcType.NVARCHAR, new NStringTypeHandler());
		register(JdbcType.NCHAR, new NStringTypeHandler());
		register(JdbcType.NCLOB, new NClobTypeHandler());

		register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
		register(JdbcType.ARRAY, new ArrayTypeHandler());

		register(BigInteger.class, new BigIntegerTypeHandler());
		register(JdbcType.BIGINT, new LongTypeHandler());

		register(BigDecimal.class, new BigDecimalTypeHandler());
		register(JdbcType.REAL, new BigDecimalTypeHandler());
		register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
		register(JdbcType.NUMERIC, new BigDecimalTypeHandler());

		register(InputStream.class, new BlobInputStreamTypeHandler());
		register(Byte[].class, new ByteObjectArrayTypeHandler());
		register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
		register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
		register(byte[].class, new ByteArrayTypeHandler());
		register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
		register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
		register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
		register(JdbcType.BLOB, new BlobTypeHandler());

		register(Object.class, unknownTypeHandler);
		register(Object.class, JdbcType.OTHER, unknownTypeHandler);
		register(JdbcType.OTHER, unknownTypeHandler);

		register(Date.class, new DateTypeHandler());
		register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
		register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
		register(JdbcType.TIMESTAMP, new DateTypeHandler());
		register(JdbcType.DATE, new DateOnlyTypeHandler());
		register(JdbcType.TIME, new TimeOnlyTypeHandler());

		register(java.sql.Date.class, new SqlDateTypeHandler());
		register(java.sql.Time.class, new SqlTimeTypeHandler());
		register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

		register(String.class, JdbcType.SQLXML, new SqlxmlTypeHandler());

		register(Instant.class, new InstantTypeHandler());
		register(LocalDateTime.class, new LocalDateTimeTypeHandler());
		register(LocalDate.class, new LocalDateTypeHandler());
		register(LocalTime.class, new LocalTimeTypeHandler());
		register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());
		register(OffsetTime.class, new OffsetTimeTypeHandler());
		register(ZonedDateTime.class, new ZonedDateTimeTypeHandler());
		register(Month.class, new MonthTypeHandler());
		register(Year.class, new YearTypeHandler());
		register(YearMonth.class, new YearMonthTypeHandler());
		register(JapaneseDate.class, new JapaneseDateTypeHandler());

		// issue #273
		register(Character.class, new CharacterTypeHandler());
		register(char.class, new CharacterTypeHandler());
	}

	/**
	 * Set a default {@link TypeHandler} class for {@link Enum}. A default {@link TypeHandler} is
	 * {@link org.apache.ibatis.type.EnumTypeHandler}.
	 *
	 * @param typeHandler a type handler class for {@link Enum}
	 * @since 3.4.5
	 */
	public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
		this.defaultEnumTypeHandler = typeHandler;
	}

	public boolean hasTypeHandler(Class<?> javaType) {
		return hasTypeHandler(javaType, null);
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
		return hasTypeHandler(javaTypeReference, null);
	}

	/**
	 * 判断类型处理注册器中的类型处理器是否可以处理 javaType、jdbcType
	 */
	public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
		return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
		return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
	}

	public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
		return allTypeHandlersMap.get(handlerType);
	}

	public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
		return getTypeHandler((Type) type, null);
	}

	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
		return getTypeHandler(javaTypeReference, null);
	}

	public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
		return jdbcTypeHandlerMap.get(jdbcType);
	}

	public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
		return getTypeHandler((Type) type, jdbcType);
	}

	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
		return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
	}

	@SuppressWarnings("unchecked")
	private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
		if (ParamMap.class.equals(type)) {
			return null;
		}
		// 查找或初始化 Java 类型对应的 TypeHandler 集合
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
		TypeHandler<?> handler = null;
		if (jdbcHandlerMap != null) {
			// 根据 JdbcType 类型查找 TypeHandler 对象
			handler = jdbcHandlerMap.get(jdbcType);
			if (handler == null) {
				handler = jdbcHandlerMap.get(null);
			}
			if (handler == null) {
				// #591
				// 如果 jdbcHandlerMap 只注册了一个 TypeHandler, 则使用此 TypeHandler 对象
				handler = pickSoleHandler(jdbcHandlerMap);
			}
		}
		// type drives generics here
		return (TypeHandler<T>) handler;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
		// 查找指定 Java 类型对应 TypeHandler 集合
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(type);
		if (jdbcHandlerMap != null) {
			// 检测是否为空集合标识
			return NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap) ? null : jdbcHandlerMap;
		}
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			if (Enum.class.isAssignableFrom(clazz)) {
				if (clazz.isAnonymousClass()) {
					return getJdbcHandlerMap(clazz.getSuperclass());
				}
				jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(clazz, clazz);
				if (jdbcHandlerMap == null) {
					register(clazz, getInstance(clazz, defaultEnumTypeHandler));
					return typeHandlerMap.get(clazz);
				}
			} else {
				jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
			}
		}
		typeHandlerMap.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
		return jdbcHandlerMap;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForEnumInterfaces(Class<?> clazz, Class<?> enumClazz) {
		for (Class<?> iface : clazz.getInterfaces()) {
			Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(iface);
			if (jdbcHandlerMap == null) {
				jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(iface, enumClazz);
			}
			if (jdbcHandlerMap != null) {
				// Found a type handler registered to a super interface
				HashMap<JdbcType, TypeHandler<?>> newMap = new HashMap<>();
				for (Entry<JdbcType, TypeHandler<?>> entry : jdbcHandlerMap.entrySet()) {
					// Create a type handler instance with enum type as a constructor arg
					newMap.put(entry.getKey(), getInstance(enumClazz, entry.getValue().getClass()));
				}
				return newMap;
			}
		}
		return null;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null || Object.class.equals(superclass)) {
			return null;
		}
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(superclass);
		if (jdbcHandlerMap != null) {
			return jdbcHandlerMap;
		}
		return getJdbcHandlerMapForSuperclass(superclass);
	}

	private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
		TypeHandler<?> soleHandler = null;
		for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
			if (soleHandler == null) {
				soleHandler = handler;
			} else if (!handler.getClass().equals(soleHandler.getClass())) {
				// More than one type handlers registered.
				return null;
			}
		}
		return soleHandler;
	}

	public TypeHandler<Object> getUnknownTypeHandler() {
		return unknownTypeHandler;
	}

	public void register(JdbcType jdbcType, TypeHandler<?> handler) {
		jdbcTypeHandlerMap.put(jdbcType, handler);
	}

	//
	// REGISTER INSTANCE
	//

	// Only handler

	@SuppressWarnings("unchecked")
	public <T> void register(TypeHandler<T> typeHandler) {
		boolean mappedTypeFound = false;
		MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
		if (mappedTypes != null) {
			for (Class<?> handledType : mappedTypes.value()) {
				register(handledType, typeHandler);
				mappedTypeFound = true;
			}
		}
		// @since 3.1.0 - try to auto-discover the mapped type
		if (!mappedTypeFound && typeHandler instanceof TypeReference) {
			try {
				TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
				register(typeReference.getRawType(), typeHandler);
				mappedTypeFound = true;
			} catch (Throwable t) {
				// maybe users define the TypeReference with a different type and are not assignable, so just ignore it
			}
		}
		if (!mappedTypeFound) {
			register((Class<T>) null, typeHandler);
		}
	}

	// java type + handler

	public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
		register((Type) javaType, typeHandler);
	}

	/**
	 * 注册 TypeHandler 类
	 *
	 * @param javaType
	 * @param typeHandler
	 * @param <T>
	 */
	private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
		// 获取 TypeHandler 类是否有 MappedJdbcTypes 注解
		MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
		// 若有 MappedJdbcTypes 注解
		if (mappedJdbcTypes != null) {
			// 遍历 MappedJdbcTypes 注解内配置的 JdbcType 值
			for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
				register(javaType, handledJdbcType, typeHandler);
			}

			// 检测是否包含 includeNullJdbcType 属性
			if (mappedJdbcTypes.includeNullJdbcType()) {
				register(javaType, null, typeHandler);
			}
		} else {
			// 没有 MappedJdbcTypes 注解，则默认全部 JdbcType 都可以处理该 TypeHandler
			register(javaType, null, typeHandler);
		}
	}

	public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
		register(javaTypeReference.getRawType(), handler);
	}

	// java type + jdbc type + handler

	// Cast is required here
	@SuppressWarnings("cast")
	public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
		register((Type) type, jdbcType, handler);
	}

	/**
	 * 注册 TypeHandler 类
	 *
	 * @param javaType
	 * @param jdbcType
	 * @param handler
	 */
	private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
		// 检测是否明确指定了 TypeHandler 能够处理的 Java 类型
		if (javaType != null) {
			// 获取指定 Java 类型在 typeHandlerMap 集合中有对应的 TypeHandler 集合
			Map<JdbcType, TypeHandler<?>> map = typeHandlerMap.get(javaType);
			// 没有则新创建一个 TypeHandler 集合
			if (map == null || map == NULL_TYPE_HANDLER_MAP) {
				map = new HashMap<>();
			}
			// 添加到 typeHandlerMap 中
			map.put(jdbcType, handler);
			typeHandlerMap.put(javaType, map);
		}
		// 记录所有 TypeHandler 类型以及该类型相关的 TypeHandler 对象
		allTypeHandlersMap.put(handler.getClass(), handler);
	}

	//
	// REGISTER CLASS
	//

	// Only handler type

	/**
	 * 注册 TypeHandler 类
	 *
	 * @param typeHandlerClass
	 */
	public void register(Class<?> typeHandlerClass) {
		boolean mappedTypeFound = false;
		// 检测是否有 MappedTypes 注解
		MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
		if (mappedTypes != null) {
			// 遍历 MappedTypes 注解内配置的 Java 类型
			for (Class<?> javaTypeClass : mappedTypes.value()) {
				register(javaTypeClass, typeHandlerClass);
				mappedTypeFound = true;
			}
		}

		// 若 MappedTypes 注解内没有 Java 类型，则默认注册该 TypeHandler
		if (!mappedTypeFound) {
			register(getInstance(null, typeHandlerClass));
		}
	}

	// java type + handler type

	public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
		register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
	}

	public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
	}

	// java type + jdbc type + handler type

	public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
		register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
	}

	// Construct a handler (used also from Builders)

	@SuppressWarnings("unchecked")
	public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		if (javaTypeClass != null) {
			try {
				Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
				return (TypeHandler<T>) c.newInstance(javaTypeClass);
			} catch (NoSuchMethodException ignored) {
				// ignored
			} catch (Exception e) {
				throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
			}
		}
		try {
			Constructor<?> c = typeHandlerClass.getConstructor();
			return (TypeHandler<T>) c.newInstance();
		} catch (Exception e) {
			throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
		}
	}

	// scan

	/**
	 * 自动扫描指定包下的 TypeHandler 实现类并完成注册
	 *
	 * @param packageName 包路径
	 */
	public void register(String packageName) {
		// 创建 ResolverUtil 对象
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
		// 查找指定包下的 TypeHandler 接口实现类
		resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);

		// 获取到包下所有的类
		Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
		for (Class<?> type : handlerSet) {
			// 过滤内部类、接口和抽象类(包括 package-info.java 文件)
			// Ignore inner classes and interfaces (including package-info.java) and abstract classes
			if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
				register(type);
			}
		}
	}

	// get information

	/**
	 * Gets the type handlers.
	 *
	 * @return the type handlers
	 * @since 3.2.2
	 */
	public Collection<TypeHandler<?>> getTypeHandlers() {
		return Collections.unmodifiableCollection(allTypeHandlersMap.values());
	}

}
