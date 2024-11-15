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
package org.apache.ibatis.session;

/**
 * SIMPLE: 为每个语句执行创建一个新的预处理语句
 * REUSE: 复用预处理语句
 * BATCH: 批量执行所有更新语句
 *
 * @author Clinton Begin
 */
public enum ExecutorType {

  SIMPLE,

  REUSE,

  BATCH

}