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
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.ibatis.reflection.ArrayUtil;

/**
 * @author Clinton Begin
 */
public class CacheKey implements Cloneable, Serializable {
    
    private static final long serialVersionUID = 1146682552656046210L;
    
    public static final CacheKey NULL_CACHE_KEY = new CacheKey() {
        
        private static final long serialVersionUID = 1L;
        
        @Override
        public void update(Object object) {
            throw new CacheException("Not allowed to update a null cache key instance.");
        }
        
        @Override
        public void updateAll(Object[] objects) {
            throw new CacheException("Not allowed to update a null cache key instance.");
        }
    };
    
    private static final int DEFAULT_MULTIPLIER = 37;
    private static final int DEFAULT_HASHCODE = 17;
    
    private final int multiplier;
    /**
     * 用于判重
     */
    private int hashcode;
    private long checksum;
    private int count;
    // 8/21/2017 - Sonarlint flags this as needing to be marked transient. While true if content is not serializable, this
    // is not always true and thus should not be marked transient.
    /**
     * 存储 SQL 信息, 用于生成 hashcode
     */
    private List<Object> updateList;
    
    /**
     * 构造函数
     */
    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLIER;
        this.count = 0;
        this.updateList = new ArrayList<>();
    }
    
    public CacheKey(Object[] objects) {
        this();
        updateAll(objects);
    }
    
    public int getUpdateCount() {
        return updateList.size();
    }
    
    /**
     * CacheKey 设置进 updateList 属性中
     */
    public void update(Object object) {
        int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
        
        count++;
        checksum += baseHashCode;
        baseHashCode *= count;
        
        // 每添加一个 Object, 都重新生成 hashCode
        // 生成 hashCode 用于快速判重, 判断是否为同一个 CacheKey, 重写了 Object 的 equals 方法
        hashcode = multiplier * hashcode + baseHashCode;
        
        // 将值设置进 updateList 中
        updateList.add(object);
    }
    
    public void updateAll(Object[] objects) {
        for (Object o : objects) {
            update(o);
        }
    }
    
    /**
     * 重写 equals 方法
     * <p>
     * 哈希值(乘法哈希)、校验值(加法哈希)、要素个数任何一个不相等, 都不是同一个查询, 最后循环比较要素, 防止哈希碰撞
     * </p>
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CacheKey)) {
            return false;
        }
        
        final CacheKey cacheKey = (CacheKey) object;
        
        if ((hashcode != cacheKey.hashcode) || (checksum != cacheKey.checksum) || (count != cacheKey.count)) {
            return false;
        }
        
        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            if (!ArrayUtil.equals(thisObject, thatObject)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        return hashcode;
    }
    
    @Override
    public String toString() {
        StringJoiner returnValue = new StringJoiner(":");
        returnValue.add(String.valueOf(hashcode));
        returnValue.add(String.valueOf(checksum));
        updateList.stream().map(ArrayUtil::toString).forEach(returnValue::add);
        return returnValue.toString();
    }
    
    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey clonedCacheKey = (CacheKey) super.clone();
        clonedCacheKey.updateList = new ArrayList<>(updateList);
        return clonedCacheKey;
    }
    
}