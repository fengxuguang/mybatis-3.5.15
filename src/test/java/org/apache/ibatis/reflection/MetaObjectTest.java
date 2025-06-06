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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.domain.misc.CustomBeanWrapper;
import org.apache.ibatis.domain.misc.CustomBeanWrapperFactory;
import org.apache.ibatis.domain.misc.RichType;
import org.junit.jupiter.api.Test;

class MetaObjectTest {

	@Test
	void shouldGetAndSetField() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richField", "foo");
		assertEquals("foo", meta.getValue("richField"));
	}

	@Test
	void shouldGetAndSetNestedField() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richType.richField", "foo");
		assertEquals("foo", meta.getValue("richType.richField"));
	}

	@Test
	void shouldGetAndSetProperty() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richProperty", "foo");
		assertEquals("foo", meta.getValue("richProperty"));
	}

	@Test
	void shouldGetAndSetNestedProperty() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richType.richProperty", "foo");
		assertEquals("foo", meta.getValue("richType.richProperty"));
	}

	@Test
	void shouldGetAndSetMapPair() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richMap.key", "foo");
		assertEquals("foo", meta.getValue("richMap.key"));
	}

	@Test
	void shouldGetAndSetMapPairUsingArraySyntax() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richMap[key]", "foo");
		assertEquals("foo", meta.getValue("richMap[key]"));
	}

	@Test
	void shouldGetAndSetNestedMapPair() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richType.richMap.key", "foo");
		assertEquals("foo", meta.getValue("richType.richMap.key"));
	}

	@Test
	void shouldGetAndSetNestedMapPairUsingArraySyntax() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richType.richMap[key]", "foo");
		assertEquals("foo", meta.getValue("richType.richMap[key]"));
	}

	@Test
	void shouldGetAndSetListItem() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richList[0]", "foo");
		assertEquals("foo", meta.getValue("richList[0]"));
	}

	@Test
	void shouldGetAndSetNestedListItem() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		meta.setValue("richType.richList[0]", "foo");
		assertEquals("foo", meta.getValue("richType.richList[0]"));
	}

	@Test
	void shouldGetReadablePropertyNames() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		String[] readables = meta.getGetterNames();
		assertEquals(5, readables.length);
		for (String readable : readables) {
			assertTrue(meta.hasGetter(readable));
			assertTrue(meta.hasGetter("richType." + readable));
		}
		assertTrue(meta.hasGetter("richType"));
	}

	@Test
	void shouldGetWriteablePropertyNames() {
		RichType rich = new RichType();
		MetaObject meta = SystemMetaObject.forObject(rich);
		String[] writeables = meta.getSetterNames();
		assertEquals(5, writeables.length);
		for (String writeable : writeables) {
			assertTrue(meta.hasSetter(writeable));
			assertTrue(meta.hasSetter("richType." + writeable));
		}
		assertTrue(meta.hasSetter("richType"));
	}

	@Test
	void shouldSetPropertyOfNullNestedProperty() {
		MetaObject richWithNull = SystemMetaObject.forObject(new RichType());
		richWithNull.setValue("richType.richProperty", "foo");
		assertEquals("foo", richWithNull.getValue("richType.richProperty"));
	}

	@Test
	void shouldSetPropertyOfNullNestedPropertyWithNull() {
		MetaObject richWithNull = SystemMetaObject.forObject(new RichType());
		richWithNull.setValue("richType.richProperty", null);
		assertNull(richWithNull.getValue("richType.richProperty"));
	}

	@Test
	void shouldGetPropertyOfNullNestedProperty() {
		MetaObject richWithNull = SystemMetaObject.forObject(new RichType());
		assertNull(richWithNull.getValue("richType.richProperty"));
	}

	@Test
	void shouldVerifyHasReadablePropertiesReturnedByGetReadablePropertyNames() {
		MetaObject object = SystemMetaObject.forObject(new Author());
		for (String readable : object.getGetterNames()) {
			assertTrue(object.hasGetter(readable));
		}
	}

	@Test
	void shouldVerifyHasWriteablePropertiesReturnedByGetWriteablePropertyNames() {
		MetaObject object = SystemMetaObject.forObject(new Author());
		for (String writeable : object.getSetterNames()) {
			assertTrue(object.hasSetter(writeable));
		}
	}

	@Test
	void shouldSetAndGetProperties() {
		MetaObject object = SystemMetaObject.forObject(new Author());
		object.setValue("email", "test");
		assertEquals("test", object.getValue("email"));

	}

	@Test
	void shouldVerifyPropertyTypes() {
		MetaObject object = SystemMetaObject.forObject(new Author());
		assertEquals(6, object.getSetterNames().length);
		assertEquals(int.class, object.getGetterType("id"));
		assertEquals(String.class, object.getGetterType("username"));
		assertEquals(String.class, object.getGetterType("password"));
		assertEquals(String.class, object.getGetterType("email"));
		assertEquals(String.class, object.getGetterType("bio"));
		assertEquals(Section.class, object.getGetterType("favouriteSection"));
	}

	@Test
	void shouldDemonstrateDeeplyNestedMapProperties() {
		HashMap<String, String> map = new HashMap<>();
		MetaObject metaMap = SystemMetaObject.forObject(map);

		assertTrue(metaMap.hasSetter("id"));
		assertTrue(metaMap.hasSetter("name.first"));
		assertTrue(metaMap.hasSetter("address.street"));

		assertFalse(metaMap.hasGetter("id"));
		assertFalse(metaMap.hasGetter("name.first"));
		assertFalse(metaMap.hasGetter("address.street"));

		metaMap.setValue("id", "100");
		metaMap.setValue("name.first", "Clinton");
		metaMap.setValue("name.last", "Begin");
		metaMap.setValue("address.street", "1 Some Street");
		metaMap.setValue("address.city", "This City");
		metaMap.setValue("address.province", "A Province");
		metaMap.setValue("address.postal_code", "1A3 4B6");

		assertTrue(metaMap.hasGetter("id"));
		assertTrue(metaMap.hasGetter("name.first"));
		assertTrue(metaMap.hasGetter("address.street"));

		assertEquals(3, metaMap.getGetterNames().length);
		assertEquals(3, metaMap.getSetterNames().length);

		@SuppressWarnings("unchecked")
		Map<String, String> name = (Map<String, String>) metaMap.getValue("name");
		@SuppressWarnings("unchecked")
		Map<String, String> address = (Map<String, String>) metaMap.getValue("address");

		assertEquals("Clinton", name.get("first"));
		assertEquals("1 Some Street", address.get("street"));
	}

	@Test
	void shouldDemonstrateNullValueInMap() {
		HashMap<String, String> map = new HashMap<>();
		MetaObject metaMap = SystemMetaObject.forObject(map);
		assertFalse(metaMap.hasGetter("phone.home"));

		metaMap.setValue("phone", null);
		assertTrue(metaMap.hasGetter("phone"));
		// hasGetter returns true if the parent exists and is null.
		assertTrue(metaMap.hasGetter("phone.home"));
		assertTrue(metaMap.hasGetter("phone.home.ext"));
		assertNull(metaMap.getValue("phone"));
		assertNull(metaMap.getValue("phone.home"));
		assertNull(metaMap.getValue("phone.home.ext"));

		metaMap.setValue("phone.office", "789");
		assertFalse(metaMap.hasGetter("phone.home"));
		assertFalse(metaMap.hasGetter("phone.home.ext"));
		assertEquals("789", metaMap.getValue("phone.office"));
		assertNotNull(metaMap.getValue("phone"));
		assertNull(metaMap.getValue("phone.home"));
	}

	@Test
	void shouldNotUseObjectWrapperFactoryByDefault() {
		MetaObject meta = SystemMetaObject.forObject(new Author());
		assertTrue(!meta.getObjectWrapper().getClass().equals(CustomBeanWrapper.class));
	}

	@Test
	void shouldUseObjectWrapperFactoryWhenSet() {
		MetaObject meta = MetaObject.forObject(new Author(), SystemMetaObject.DEFAULT_OBJECT_FACTORY,
				new CustomBeanWrapperFactory(), new DefaultReflectorFactory());
		assertEquals(CustomBeanWrapper.class, meta.getObjectWrapper().getClass());

		// Make sure the old default factory is in place and still works
		meta = SystemMetaObject.forObject(new Author());
		assertNotEquals(CustomBeanWrapper.class, meta.getObjectWrapper().getClass());
	}

	@Test
	void shouldMethodHasGetterReturnTrueWhenListElementSet() {
		List<Object> param1 = new ArrayList<>();
		param1.add("firstParam");
		param1.add(222);
		param1.add(new Date());

		Map<String, Object> parametersEmulation = new HashMap<>();
		parametersEmulation.put("param1", param1);
		parametersEmulation.put("filterParams", param1);

		MetaObject meta = SystemMetaObject.forObject(parametersEmulation);

		assertEquals(param1.get(0), meta.getValue("filterParams[0]"));
		assertEquals(param1.get(1), meta.getValue("filterParams[1]"));
		assertEquals(param1.get(2), meta.getValue("filterParams[2]"));

		assertTrue(meta.hasGetter("filterParams[0]"));
		assertTrue(meta.hasGetter("filterParams[1]"));
		assertTrue(meta.hasGetter("filterParams[2]"));
	}

}
