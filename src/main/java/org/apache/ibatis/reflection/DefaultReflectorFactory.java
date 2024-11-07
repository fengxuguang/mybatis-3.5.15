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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ibatis.util.MapUtil;

/**
 * Mybatis 默认的反射工厂实现
 */
public class DefaultReflectorFactory implements ReflectorFactory {
    /**
     * 是否允许缓存标识, 默认 true
     */
    private boolean classCacheEnabled = true;
    /**
     * Reflector 对象的缓存容器
     */
    private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();
    
    public DefaultReflectorFactory() {
    }
    
    @Override
    public boolean isClassCacheEnabled() {
        return classCacheEnabled;
    }
    
    @Override
    public void setClassCacheEnabled(boolean classCacheEnabled) {
        this.classCacheEnabled = classCacheEnabled;
    }
    
    @Override
    public Reflector findForClass(Class<?> type) {
        // 允许缓存
        if (classCacheEnabled) {
            // 缓存中存在, 优先从 reflectorMap 缓存中获取, 缓存中不存在, 创建 Reflector 对象返回并添加进 reflectorMap 缓存中
            // synchronized (type) removed see issue #461
            return MapUtil.computeIfAbsent(reflectorMap, type, Reflector::new);
        }
        // 不允许缓存, 创建 Reflector 对象并返回
        return new Reflector(type);
    }
    
}
