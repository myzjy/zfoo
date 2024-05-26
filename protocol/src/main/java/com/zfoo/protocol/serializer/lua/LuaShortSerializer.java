/*
 * Copyright (C) 2020 The zfoo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.zfoo.protocol.serializer.lua;

import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.registration.field.IFieldRegistration;
import com.zfoo.protocol.util.StringUtils;

import java.lang.reflect.Field;

import static com.zfoo.protocol.util.FileUtils.LS;
import static com.zfoo.protocol.util.StringUtils.TAB;

/**
 * @author godotg
 */
public class LuaShortSerializer implements ILuaSerializer {
    @Override
    public void writeObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("{}", objectStr)).append(LS);
    }

    @Override
    public void initWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration, String fieldNote) {
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("---@type number").append(LS);
        builder.append(TAB.repeat(1));
        builder.append(StringUtils.format("self.{} = 0", objectStr)).append(LS);
    }

    @Override
    public String listObjectString(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        return "";
    }

    @Override
    public void createGetWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration, String protocolClazzName) {
        String result = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        builder.append(StringUtils.format("--- {}", objectStr)).append(LS);
        builder.append(StringUtils.format("---@return number {}", objectStr)).append(LS);
        builder.append(StringUtils.format("function {}:get{}()", protocolClazzName, result)).append(LS);
        builder.append(TAB);
        builder.append(StringUtils.format("return self.{}", field.getName())).append(LS);
        builder.append("end").append(LS);
    }

    @Override
    public String readObject(StringBuilder builder, int deep, Field field, IFieldRegistration fieldRegistration, String objectStr) {
        String result = "result" + GenerateProtocolFile.index.getAndIncrement();
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("data.{}{}", field.getName(), objectStr)).append(LS);
        return result;
    }

    @Override
    public void createReturnWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        builder.append(StringUtils.format("---@param {} number {}", field.getName(), objectStr));
        if (deep == 0) {
            builder.append(LS);
        }
    }
}
