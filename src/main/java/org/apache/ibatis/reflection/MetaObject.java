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
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * 元对象封装
 *
 * @author Clinton Begin
 */
public class MetaObject {

    /**
     * 原对象
     */
	private final Object originalObject;
    /**
     * 对象包装器
     */
	private final ObjectWrapper objectWrapper;
    /**
     * 对象工厂
     */
	private final ObjectFactory objectFactory;
    /**
     * 对象包装工厂
     */
	private final ObjectWrapperFactory objectWrapperFactory;
    /**
     * 反射工厂
     */
	private final ReflectorFactory reflectorFactory;

	private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
	                   ReflectorFactory reflectorFactory) {
		this.originalObject = object;
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;

		if (object instanceof ObjectWrapper) {
            // 如果对象本身已经是 ObjectWrapper 类型, 则直接赋给 objectWrapper
			this.objectWrapper = (ObjectWrapper) object;
		} else if (objectWrapperFactory.hasWrapperFor(object)) {
            // 如果有包装器, 调用 ObjectWrapperFactory.getWrapperFor
			this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
		} else if (object instanceof Map) {
            // 如果是 Map 类型, 返回 MapWrapper
			this.objectWrapper = new MapWrapper(this, (Map) object);
		} else if (object instanceof Collection) {
            // 如果是 Collection 类型, 返回 CollectionWrapper
			this.objectWrapper = new CollectionWrapper(this, (Collection) object);
		} else {
            // 默认返回 BeanWrapper
			this.objectWrapper = new BeanWrapper(this, object);
		}
	}

	public static MetaObject forObject(Object object, ObjectFactory objectFactory,
	                                   ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		if (object == null) {
            // 处理一下 null, 将 null 包装起来
			return SystemMetaObject.NULL_META_OBJECT;
		}
		return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}

	public Object getOriginalObject() {
		return originalObject;
	}

	public String findProperty(String propName, boolean useCamelCaseMapping) {
		return objectWrapper.findProperty(propName, useCamelCaseMapping);
	}

	public String[] getGetterNames() {
		return objectWrapper.getGetterNames();
	}

	public String[] getSetterNames() {
		return objectWrapper.getSetterNames();
	}

	public Class<?> getSetterType(String name) {
		return objectWrapper.getSetterType(name);
	}

	public Class<?> getGetterType(String name) {
		return objectWrapper.getGetterType(name);
	}

	public boolean hasSetter(String name) {
		return objectWrapper.hasSetter(name);
	}

	public boolean hasGetter(String name) {
		return objectWrapper.hasGetter(name);
	}

    /**
     * 取值
     */
	public Object getValue(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (!prop.hasNext()) {
			return objectWrapper.get(prop);
		}
		MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
		if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
            // 如果上层是 null 了, 那就结束返回 null
			return null;
		}

        // 继续看下一层, 递归调用 getValue
		return metaValue.getValue(prop.getChildren());
	}

    /**
     * 设值
     */
	public void setValue(String name, Object value) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				if (value == null) {
					// don't instantiate child path if value is null
                    // 如果上层是 null, 结束返回 null
					return;
				}
                // 否则需要 new 一个, 委派给 ObjectWrapper.instantiatePropertyValue
				metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
			}

            // 递归调用 setValue
			metaValue.setValue(prop.getChildren(), value);
		} else {
            // 委派给 ObjectWrapper.set
			objectWrapper.set(prop, value);
		}
	}

	public MetaObject metaObjectForProperty(String name) {
		Object value = getValue(name);
		return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
	}

	public ObjectWrapper getObjectWrapper() {
		return objectWrapper;
	}

	public boolean isCollection() {
		return objectWrapper.isCollection();
	}

	public void add(Object element) {
		objectWrapper.add(element);
	}

	public <E> void addAll(List<E> list) {
		objectWrapper.addAll(list);
	}

}
