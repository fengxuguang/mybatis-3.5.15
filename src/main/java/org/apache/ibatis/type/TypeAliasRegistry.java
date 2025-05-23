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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * @author Clinton Begin
 */
public class TypeAliasRegistry {

	/**
	 * 类型别名缓存
	 */
	private final Map<String, Class<?>> typeAliases = new HashMap<>();

	public TypeAliasRegistry() {
		registerAlias("string", String.class);

		registerAlias("byte", Byte.class);
		registerAlias("char", Character.class);
		registerAlias("character", Character.class);
		registerAlias("long", Long.class);
		registerAlias("short", Short.class);
		registerAlias("int", Integer.class);
		registerAlias("integer", Integer.class);
		registerAlias("double", Double.class);
		registerAlias("float", Float.class);
		registerAlias("boolean", Boolean.class);

		registerAlias("byte[]", Byte[].class);
		registerAlias("char[]", Character[].class);
		registerAlias("character[]", Character[].class);
		registerAlias("long[]", Long[].class);
		registerAlias("short[]", Short[].class);
		registerAlias("int[]", Integer[].class);
		registerAlias("integer[]", Integer[].class);
		registerAlias("double[]", Double[].class);
		registerAlias("float[]", Float[].class);
		registerAlias("boolean[]", Boolean[].class);

		registerAlias("_byte", byte.class);
		registerAlias("_char", char.class);
		registerAlias("_character", char.class);
		registerAlias("_long", long.class);
		registerAlias("_short", short.class);
		registerAlias("_int", int.class);
		registerAlias("_integer", int.class);
		registerAlias("_double", double.class);
		registerAlias("_float", float.class);
		registerAlias("_boolean", boolean.class);

		registerAlias("_byte[]", byte[].class);
		registerAlias("_char[]", char[].class);
		registerAlias("_character[]", char[].class);
		registerAlias("_long[]", long[].class);
		registerAlias("_short[]", short[].class);
		registerAlias("_int[]", int[].class);
		registerAlias("_integer[]", int[].class);
		registerAlias("_double[]", double[].class);
		registerAlias("_float[]", float[].class);
		registerAlias("_boolean[]", boolean[].class);

		registerAlias("date", Date.class);
		registerAlias("decimal", BigDecimal.class);
		registerAlias("bigdecimal", BigDecimal.class);
		registerAlias("biginteger", BigInteger.class);
		registerAlias("object", Object.class);

		registerAlias("date[]", Date[].class);
		registerAlias("decimal[]", BigDecimal[].class);
		registerAlias("bigdecimal[]", BigDecimal[].class);
		registerAlias("biginteger[]", BigInteger[].class);
		registerAlias("object[]", Object[].class);

		registerAlias("map", Map.class);
		registerAlias("hashmap", HashMap.class);
		registerAlias("list", List.class);
		registerAlias("arraylist", ArrayList.class);
		registerAlias("collection", Collection.class);
		registerAlias("iterator", Iterator.class);

		registerAlias("ResultSet", ResultSet.class);
	}

	/**
	 * 获取对象
	 *
	 * @param string 字符串
	 * @param <T>
	 * @return Class 对象
	 */
	@SuppressWarnings("unchecked")
	// throws class cast exception as well if types cannot be assigned
	public <T> Class<T> resolveAlias(String string) {
		try {
			if (string == null) {
				return null;
			}
			// issue #748
			String key = string.toLowerCase(Locale.ENGLISH);
			Class<T> value;
			// 判断别名 typeAliases 缓存中是否存在
			if (typeAliases.containsKey(key)) {
				value = (Class<T>) typeAliases.get(key);
			} else {
				// 如果不存在，则通过反射获取
				value = (Class<T>) Resources.classForName(string);
			}
			return value;
		} catch (ClassNotFoundException e) {
			// 找不到这个类, 则抛出异常
			throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
		}
	}

	/**
	 * 注册包路径下的所有类型别名
	 *
	 * @param packageName 包路径
	 */
	public void registerAliases(String packageName) {
		registerAliases(packageName, Object.class);
	}

	/**
	 * 注册指定包路径下的所有类型别名
	 *
	 * @param packageName 包路径
	 * @param superType   父类
	 */
	public void registerAliases(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
		// 反射获取包路径下所有类
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
		for (Class<?> type : typeSet) {
			// Ignore inner classes and interfaces (including package-info.java)
			// Skip also inner classes. See issue #6
			// 忽略内部类和接口
			if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
				registerAlias(type);
			}
		}
	}

	/**
	 * 注册别名
     * 默认为简单类名, 优先从 Alias 注解获取
	 *
	 * @param type 类名
	 */
	public void registerAlias(Class<?> type) {
		// 获取简单类名
		String alias = type.getSimpleName();
		// 判断该类是否使用 @Alias 注解, 若使用了 @Alias 注解, 则用注解里 value 值作为别名
		Alias aliasAnnotation = type.getAnnotation(Alias.class);
		if (aliasAnnotation != null) {
			alias = aliasAnnotation.value();
		}
		// 否则使用简单类名作为别名
		registerAlias(alias, type);
	}

	/**
	 * 注册别名
	 *
	 * @param alias 别名
	 * @param value 类名
	 */
	public void registerAlias(String alias, Class<?> value) {
		if (alias == null) {
			throw new TypeException("The parameter alias cannot be null");
		}
		// issue #748
		// 别名转小写
		String key = alias.toLowerCase(Locale.ENGLISH);
		if (typeAliases.containsKey(key) && typeAliases.get(key) != null && !typeAliases.get(key).equals(value)) {
			throw new TypeException(
					"The alias '" + alias + "' is already mapped to the value '" + typeAliases.get(key).getName() + "'.");
		}
		typeAliases.put(key, value);
	}

	/**
	 * 注册别名
	 *
	 * @param alias 别名
	 * @param value 类字符串
	 */
	public void registerAlias(String alias, String value) {
		try {
			registerAlias(alias, Resources.classForName(value));
		} catch (ClassNotFoundException e) {
			throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
		}
	}

	/**
	 * Gets the type aliases.
	 *
	 * @return the type aliases
	 *
	 * @since 3.2.2
	 */
	/**
	 * 获取别名集合, 使用 Collections.unmodifiableMap() 返回只读集合
	 *
	 * @return 别名集合
	 */
	public Map<String, Class<?>> getTypeAliases() {
		return Collections.unmodifiableMap(typeAliases);
	}

}
