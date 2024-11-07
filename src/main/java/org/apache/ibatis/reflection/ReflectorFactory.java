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

/**
 * 反射工厂, 实现了 Reflector 对象的创建和缓存
 */
public interface ReflectorFactory {
    
    /**
     * ReflectorFactory 对象是否会缓存 Reflector 对象
     * @return true 表示会缓存 Reflector 对象, false 表示不会缓存 Reflector 对象
     */
    boolean isClassCacheEnabled();
    
    /**
     * 设置是否缓存 Reflector 对象
     * @param classCacheEnabled true 表示缓存 Reflector 对象, false 表示不缓存 Reflector 对象
     */
    void setClassCacheEnabled(boolean classCacheEnabled);
    
    /**
     * 创建指定 Class 对应的 Reflector 对象
     * @param type 类型
     * @return Reflector 对象
     */
    Reflector findForClass(Class<?> type);
}
