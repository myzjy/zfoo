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
import com.zfoo.protocol.registration.field.ListField;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.CutDownListSerializer;
import com.zfoo.protocol.util.StringUtils;

import java.lang.reflect.Field;

import static com.zfoo.protocol.util.FileUtils.LS;
import static com.zfoo.protocol.util.StringUtils.TAB;

/**
 * @author godotg
 */
public class LuaListSerializer implements ILuaSerializer {
    @Override
    public void writeObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        GenerateProtocolFile.addTab(builder, deep);
        if (CutDownListSerializer.getInstance().writeObject(builder, objectStr, field, fieldRegistration, CodeLanguage.Lua)) {
            return;
        }
        ListField listField = (ListField) fieldRegistration;
        builder.append(StringUtils.format("if {} == null then", objectStr)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("buffer:writeInt(0)").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("else").append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("buffer:writeInt(#{})", objectStr)).append(LS);
        String index = "index" + GenerateProtocolFile.index.getAndIncrement();
        String element = "element" + GenerateProtocolFile.index.getAndIncrement();
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {}, {} in pairs({}) do", index, element, objectStr)).append(LS);
        GenerateLuaUtils.luaSerializer(listField.getListElementRegistration().serializer())
                        .writeObject(builder, element, deep + 2, field, listField.getListElementRegistration());
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("end").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("end").append(LS);
    }

    @Override
    public void initWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration, String fieldNote) {
        GenerateProtocolFile.addTab(builder, deep);
        var typeName = field.getGenericType().getTypeName();
        var typeNameList = typeName.split("\\.");
        var filedName = StringUtils.replacePattern(typeNameList[typeNameList.length - 1], ">", "");
        builder.append(StringUtils.format("---@type  table<number,{}>", filedName)).append(LS);
        builder.append(TAB.repeat(1));
        builder.append(StringUtils.format("self.{} = {}", objectStr)).append(LS);
    }

    @Override
    public void createGetWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration, String protocolClazzName) {
        var typeName = field.getGenericType().getTypeName();
        var typeNameList = typeName.split("\\.");
        var filedName = StringUtils.replacePattern(typeNameList[typeNameList.length - 1], ">", "");
        String result = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);

        builder.append(StringUtils.format("--- {}", objectStr)).append(LS);
        builder.append(StringUtils.format("---@type  table<number,{}> {}", filedName, objectStr)).append(LS);
        builder.append(StringUtils.format("function {}:get{}()", protocolClazzName, result)).append(LS);
        builder.append(TAB);
        builder.append(StringUtils.format("return self.{}", field.getName())).append(LS);
        builder.append("end").append(LS);
    }

    @Override
    public String listObjectString(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        var typeName = field.getGenericType().getTypeName();
        var typeNameList = typeName.split("\\.");
        var filedName = StringUtils.replacePattern(typeNameList[typeNameList.length - 1], ">", "");
        builder.append(TAB);
        builder.append(StringUtils.format("local {} = {}", field.getName())).append(LS);
        builder.append(TAB);
        builder.append(StringUtils.format("for index, value in ipairs(data.{}) do", field.getName())).append(LS);
        builder.append(TAB.repeat(2));
        builder.append(StringUtils.format("local {}Packet = {}()", field.getName(), filedName)).append(LS);
        builder.append(TAB.repeat(2));
        builder.append(StringUtils.format("local packetData = {}Packet:read(value)", field.getName())).append(LS);
        builder.append(TAB.repeat(2));
        builder.append(StringUtils.format("table.insert({},packetData)", field.getName())).append(LS);
        builder.append(TAB);
        builder.append("end").append(LS);
        return "";
    }

    @Override
    public String readObject(StringBuilder builder, int deep, Field field, IFieldRegistration fieldRegistration, String objectStr) {
        var result = "result" + GenerateProtocolFile.index.getAndIncrement();
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("{}{}", field.getName(), objectStr)).append(LS);
        return result;
    }

    @Override
    public void createReturnWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        var typeName = field.getGenericType().getTypeName();
        var typeNameList = typeName.split("\\.");
        var filedName = StringUtils.replacePattern(typeNameList[typeNameList.length - 1], ">", "");
        builder.append(StringUtils.format("---@param {} table<number,{}> {}", field.getName(), filedName, objectStr));
        if (deep == 0) {
            builder.append(LS);
        }
    }
}
