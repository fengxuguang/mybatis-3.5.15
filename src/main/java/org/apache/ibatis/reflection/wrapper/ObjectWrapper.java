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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对象包装器
 *
 * @author Clinton Begin
 */
public interface ObjectWrapper {

	Object get(PropertyTokenizer prop);

	void set(PropertyTokenizer prop, Object value);

    /**
     * 查找属性
     */
	String findProperty(String name, boolean useCamelCaseMapping);

    /**
     * 取得 getter 的名字列表
     */
	String[] getGetterNames();

    /**
     * 取得 setter 的名字列表
     */
	String[] getSetterNames();

    /**
     * 取得 setter 的类型
     */
	Class<?> getSetterType(String name);

    /**
     * 取得 getter 的类型
     */
	Class<?> getGetterType(String name);

    /**
     * 判断是否有 set 方法
     */
	boolean hasSetter(String name);

    /**
     * 判断是否有 get 方法
     */
	boolean hasGetter(String name);

	MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

	boolean isCollection();

	void add(Object element);

	<E> void addAll(List<E> element);

}
