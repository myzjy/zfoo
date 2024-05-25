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

import com.zfoo.protocol.anno.Compatible;
import com.zfoo.protocol.generate.GenerateOperation;
import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.generate.GenerateProtocolNote;
import com.zfoo.protocol.generate.GenerateProtocolPath;
import com.zfoo.protocol.model.Pair;
import com.zfoo.protocol.registration.IProtocolRegistration;
import com.zfoo.protocol.registration.ProtocolRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.reflect.*;
import com.zfoo.protocol.util.ClassUtils;
import com.zfoo.protocol.util.FileUtils;
import com.zfoo.protocol.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zfoo.protocol.util.FileUtils.LS;
import static com.zfoo.protocol.util.StringUtils.*;

/**
 * @author godotg
 */
public abstract class GenerateLuaUtils {
    private static String protocolOutputRootPath = "LuaProtocol/";
    private static Map<ISerializer, ILuaSerializer> luaSerializerMap;

    public static ILuaSerializer luaSerializer(ISerializer serializer) {
        return luaSerializerMap.get(serializer);
    }

    public static void init(GenerateOperation generateOperation) {
        protocolOutputRootPath = FileUtils.joinPath(generateOperation.getProtocolPath(), protocolOutputRootPath);
        FileUtils.deleteFile(new File(protocolOutputRootPath));
        FileUtils.createDirectory(protocolOutputRootPath);
        luaSerializerMap = new HashMap<>();
        luaSerializerMap.put(BooleanSerializer.INSTANCE, new LuaBooleanSerializer());
        luaSerializerMap.put(ByteSerializer.INSTANCE, new LuaByteSerializer());
        luaSerializerMap.put(ShortSerializer.INSTANCE, new LuaShortSerializer());
        luaSerializerMap.put(IntSerializer.INSTANCE, new LuaIntSerializer());
        luaSerializerMap.put(LongSerializer.INSTANCE, new LuaLongSerializer());
        luaSerializerMap.put(FloatSerializer.INSTANCE, new LuaFloatSerializer());
        luaSerializerMap.put(DoubleSerializer.INSTANCE, new LuaDoubleSerializer());
        luaSerializerMap.put(StringSerializer.INSTANCE, new LuaStringSerializer());
        luaSerializerMap.put(ArraySerializer.INSTANCE, new LuaArraySerializer());
        luaSerializerMap.put(ListSerializer.INSTANCE, new LuaListSerializer());
        luaSerializerMap.put(SetSerializer.INSTANCE, new LuaSetSerializer());
        luaSerializerMap.put(MapSerializer.INSTANCE, new LuaMapSerializer());
        luaSerializerMap.put(ObjectProtocolSerializer.INSTANCE, new LuaObjectProtocolSerializer());
    }

    public static void clear() {
        luaSerializerMap = null;
        protocolOutputRootPath = null;
    }

    public static void createProtocolManager(List<IProtocolRegistration> protocolList) throws IOException {
        var list = List.of("lua/Buffer/ByteBuffer.lua", "lua/Buffer/Long.lua");
        for (var fileName : list) {
            var fileInputStream = ClassUtils.getFileFromClassPath(fileName);
            var createFile = new File(StringUtils.format("{}/{}", protocolOutputRootPath, StringUtils.substringAfterFirst(fileName, "lua/")));
            FileUtils.writeInputStreamToFile(createFile, fileInputStream);
        }
        // 生成Protocol.lua文件
        var protocolManagerTemplate = ClassUtils.getFileFromClassPathToString("lua/ProtocolManagerTemplate.lua");
        var fieldBuilder = new StringBuilder();
        var protocolBuilder = new StringBuilder();
        for (var protocol : protocolList) {
            var protocolId = protocol.protocolId();
            var name = protocol.protocolConstructor().getDeclaringClass().getSimpleName();
            var path = GenerateProtocolPath.getCapitalizeProtocolPath(protocolId);
            if (StringUtils.isBlank(path)) {
                fieldBuilder.append(TAB).append(StringUtils.format("local {} = require(\"LuaProtocol.{}\")", name, name)).append(LS);
            } else {
                fieldBuilder.append(TAB).append(StringUtils.format("local {} = require(\"LuaProtocol.{}.{}\")"
                        , name, path.replaceAll(StringUtils.SLASH, StringUtils.PERIOD), name)).append(LS);
            }
            protocolBuilder.append(TAB).append(StringUtils.format("protocols[{}] = {}", protocolId, name)).append(LS);
        }
        protocolManagerTemplate = StringUtils.format(protocolManagerTemplate, StringUtils.EMPTY_JSON, StringUtils.EMPTY_JSON, fieldBuilder.toString().trim(), protocolBuilder.toString().trim());
        FileUtils.writeStringToFile(new File(StringUtils.format("{}/{}", protocolOutputRootPath, "ProtocolManager.lua")), protocolManagerTemplate, true);
    }

    public static void createLuaProtocolFile(ProtocolRegistration registration) throws IOException {
        // 初始化index
        GenerateProtocolFile.index.set(0);
        var protocolId = registration.protocolId();
        var registrationConstructor = registration.getConstructor();
        var protocolClazzName = registrationConstructor.getDeclaringClass().getSimpleName();
        var protocolTemplate = ClassUtils.getFileFromClassPathToString("lua/ProtocolTemplate.lua");
        var classNote = GenerateProtocolNote.classNote(protocolId, CodeLanguage.Lua);
        var valueOfMethod = valueOfMethod(registration);
        var valueKey = valueKey(registration);
        var writePacket = writePacket(registration);
        var readPacket = readPacket(registration);
        protocolTemplate = StringUtils.format(protocolTemplate,
                                              protocolClazzName, // 1
                                              protocolClazzName, // 2
                                              protocolClazzName, // 3
                                              protocolClazzName, // 4
                                              valueKey.getValue().trim(), // 5
                                              protocolClazzName, // 6
                                              protocolClazzName, // 6
                                              valueOfMethod.getKey().trim(), // 7
                                              valueOfMethod.getValue().trim(), // 8
                                              protocolClazzName, // 9
                                              protocolId, //10
                                              protocolClazzName,
                                              writePacket.trim(),
                                              protocolClazzName,
                                              protocolClazzName,
                                              valueListAndMap(registration),
                                              readPacket.trim(),
                                              // get
                                              createGetPacket(registration,protocolClazzName),
                                              protocolClazzName,
                                              StringUtils.EMPTY_JSON);
        var protocolOutputPath = StringUtils.format("{}/{}/{}.lua"
                , protocolOutputRootPath, GenerateProtocolPath.getCapitalizeProtocolPath(protocolId), protocolClazzName);
        FileUtils.writeStringToFile(new File(protocolOutputPath), protocolTemplate, true);
    }

    private static String valueListAndMap(ProtocolRegistration registration) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var luaBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldName = field.getName();
            var fieldRegistration = fieldRegistrations[i];
            luaSerializer(fieldRegistration.serializer()).listObjectString(luaBuilder, field.getName(), 1, field, fieldRegistration);
        }
        return luaBuilder.toString();
    }

    private static Pair<String, String> valueOfMethod(ProtocolRegistration registration) {
        var protocolId = registration.getId();
        var fields = registration.getFields();
        var valueOfParams = StringUtils.joinWith(", ", Arrays.stream(fields).map(it -> it.getName()).toList().toArray());
        var luaBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldName = field.getName();
            // 生成注释
            var fieldNote = GenerateProtocolNote.fieldNote(protocolId, fieldName, CodeLanguage.Lua);
            if (StringUtils.isNotBlank(fieldNote)) {
                luaBuilder.append(TAB + TAB).append(fieldNote).append(LS);
            }
            luaBuilder.append(TAB).append(StringUtils.format("self.{} = {}", fieldName, fieldName));
            // 生成类型的注释
            luaBuilder.append(" -- ").append(field.getGenericType().getTypeName()).append(LS);
        }
        return new Pair<>(valueOfParams, luaBuilder.toString());
    }

    private static Pair<String, String> valueKey(ProtocolRegistration registration) {
        var protocolId = registration.getId();
        var fields = registration.getFields();
        var valueOfParams = StringUtils.joinWith(", ", Arrays.stream(fields).map(Field::getName).toList().toArray());
        var fieldRegistrations = registration.getFieldRegistrations();
        var luaBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldName = field.getName();
            var fieldRegistration = fieldRegistrations[i];
            //luaBuilder.append(TAB).append(StringUtils.format("self.{} = nil", fieldName, fieldName));
            // 生成类型的注释
            var fieldNote = GenerateProtocolNote.fieldNote(protocolId, fieldName, CodeLanguage.Lua);
            if (StringUtils.isNotBlank(fieldNote)) {
                luaBuilder.append(TAB).append(fieldNote).append(LS);
            }
            //var objectString = StringUtils.format("self.{} = {}", field.getName(), field.getName(), i <= fields.length - 2 ? DOUHAO : EMPTY);
            luaSerializer(fieldRegistration.serializer()).initWriterObject(luaBuilder, field.getName(), 1, field, fieldRegistration, fieldNote);
        }
        return new Pair<>(valueOfParams, luaBuilder.toString());
    }

    private static String createGetPacket(ProtocolRegistration registration,String protocolClazzName) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var luaBuilder = new StringBuilder();
        var protocolId = registration.getId();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldName = field.getName();
            var fieldRegistration = fieldRegistrations[i];
            var fieldNote = GenerateProtocolNote.fieldNote(protocolId, fieldName, CodeLanguage.Lua);
            luaSerializer(fieldRegistration.serializer()).createGetWriterObject(luaBuilder, fieldNote, 1, field, fieldRegistration,protocolClazzName);
        }
        return luaBuilder.toString();
    }

    private static String writePacket(ProtocolRegistration registration) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var luaBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldRegistration = fieldRegistrations[i];
            luaBuilder.append(TAB.repeat(2));
            var objectString = StringUtils.format("{} = self.{}{}", field.getName(), field.getName(), i <= fields.length - 2 ? DOUHAO : EMPTY);
            luaSerializer(fieldRegistration.serializer()).writeObject(luaBuilder, objectString, 1, field, fieldRegistration);
        }
        return luaBuilder.toString();
    }

    private static String readPacket(ProtocolRegistration registration) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var luaBuilder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldRegistration = fieldRegistrations[i];
            luaBuilder.append(TAB.repeat(2));
            var objectString = StringUtils.format("data.{}{}", field.getName(), i <= fields.length - 2 ? DOUHAO : EMPTY);
            luaSerializer(fieldRegistration.serializer()).readObject(luaBuilder, 1, field, fieldRegistration, i <= fields.length - 2 ? DOUHAO : EMPTY);
        }
        return luaBuilder.toString();
    }
}
