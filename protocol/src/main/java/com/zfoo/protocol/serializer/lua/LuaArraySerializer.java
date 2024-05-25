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
import com.zfoo.protocol.registration.field.ArrayField;
import com.zfoo.protocol.registration.field.IFieldRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.CutDownArraySerializer;
import com.zfoo.protocol.util.StringUtils;

import java.lang.reflect.Field;

import static com.zfoo.protocol.util.FileUtils.LS;
import static com.zfoo.protocol.util.StringUtils.TAB;

/**
 * @author godotg
 */
public class LuaArraySerializer implements ILuaSerializer {
    @Override
    public void writeObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        GenerateProtocolFile.addTab(builder, deep);
        if (CutDownArraySerializer.getInstance().writeObject(builder, objectStr, field, fieldRegistration, CodeLanguage.Lua)) {
            return;
        }
        ArrayField arrayField = (ArrayField) fieldRegistration;
        builder.append(StringUtils.format("if {} == null then", objectStr)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("buffer:writeInt(0)").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("else").append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("buffer:writeInt(#{});", objectStr)).append(LS);
        String index = "index" + GenerateProtocolFile.index.getAndIncrement();
        String element = "element" + GenerateProtocolFile.index.getAndIncrement();
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {}, {} in pairs({}) do", index, element, objectStr)).append(LS);
        GenerateLuaUtils.luaSerializer(arrayField.getArrayElementRegistration().serializer())
                        .writeObject(builder, element, deep + 2, field, arrayField.getArrayElementRegistration());
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("end").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("end").append(LS);
    }

    @Override
    public void initWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration, String fieldNote) {
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(TAB.repeat(1));
        builder.append(StringUtils.format("self.{} = {}", objectStr)).append(LS);
    }

    @Override
    public void createGetWriterObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration,String protocolClazzName) {
        GenerateProtocolFile.addTab(builder, deep);
        var typeName = field.getGenericType().getTypeName();
        var typeNameList = typeName.split("\\.");
        var filedName = StringUtils.replacePattern(typeNameList[typeNameList.length - 1], ">", "");
        builder.append(StringUtils.format("{}", objectStr)).append(LS);
        builder.append(StringUtils.format("---@return {} {}", filedName, objectStr)).append(LS);
        builder.append(StringUtils.format("function {}:get{}()", filedName, filedName)).append(LS);
        builder.append(TAB);
        builder.append(StringUtils.format("return self.{}", field.getName())).append(LS);
        builder.append("end").append(LS);
    }

    @Override
    public String listObjectString(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        return "";
    }

    @Override
    public String readObject(StringBuilder builder, int deep, Field field, IFieldRegistration fieldRegistration, String objectStr) {
        GenerateProtocolFile.addTab(builder, deep);
        var cutDown = CutDownArraySerializer.getInstance().readObject(builder, field, fieldRegistration, CodeLanguage.Lua);
        if (cutDown != null) {
            return cutDown;
        }
        var arrayField = (ArrayField) fieldRegistration;
        var result = "result" + GenerateProtocolFile.index.getAndIncrement();
        builder.append(StringUtils.format("local {} = {}", result)).append(LS);
        var i = "index" + GenerateProtocolFile.index.getAndIncrement();
        var size = "size" + GenerateProtocolFile.index.getAndIncrement();
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("local {} = buffer:readInt()", size)).append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("if {} > 0 then", size)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {} = 1, {} do", i, size)).append(LS);
        String readObject = GenerateLuaUtils.luaSerializer(arrayField.getArrayElementRegistration().serializer())
                                            .readObject(builder, deep + 2, field, arrayField.getArrayElementRegistration(), objectStr);
        GenerateProtocolFile.addTab(builder, deep + 2);
        builder.append(StringUtils.format("table.insert({}, {})", result, readObject)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("end").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("end").append(LS);
        return result;
    }
}
