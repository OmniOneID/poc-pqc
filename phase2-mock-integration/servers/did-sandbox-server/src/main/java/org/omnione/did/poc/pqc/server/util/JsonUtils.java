/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private static JsonObject sort(JsonObject jsonObject) {
        TreeMap<String, Object> map = new TreeMap<>(Comparator.naturalOrder());
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                map.put(entry.getKey(), JsonUtils.sort(value.getAsJsonObject()));
                continue;
            }
            map.put(entry.getKey(), value);
        }
        JsonObject sortedObject = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sortedObject.add((String)entry.getKey(), (JsonElement)entry.getValue());
        }
        return sortedObject;
    }

    public static String serializeAndSort(Object obj) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        JsonElement element = gson.toJsonTree(obj);
        return gson.toJson(JsonUtils.deepSort(element));
    }

    private static JsonElement deepSort(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject sorted = new JsonObject();
            element.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> sorted.add((String)e.getKey(), JsonUtils.deepSort((JsonElement)e.getValue())));
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            for (JsonElement item : element.getAsJsonArray()) {
                array.add(JsonUtils.deepSort(item));
            }
            return array;
        }
        return element;
    }

    public static String serializeAndSortVc(Object obj) {
        Map map = mapper.convertValue(obj, Map.class);
        Object sorted = JsonUtils.sortRecursively(map);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted);
        }
        catch (Exception e) {
            throw new RuntimeException("JSON \uc815\ub82c \uc2e4\ud328", e);
        }
    }

    private static Object sortRecursively(Object obj) {
        if (obj instanceof Map) {
            Map original = (Map)obj;
            LinkedHashMap<String, Object> sorted = new LinkedHashMap<String, Object>();
            if (original.containsKey("@context")) {
                sorted.put("@context", JsonUtils.sortRecursively(original.get("@context")));
            }
            original.keySet().stream().filter(k -> !"@context".equals(k)).sorted().forEach(k -> sorted.put((String)k, JsonUtils.sortRecursively(original.get(k))));
            return sorted;
        }
        if (obj instanceof List) {
            List originalList = (List)obj;
            ArrayList<Object> sortedList = new ArrayList<Object>();
            for (Object item : originalList) {
                sortedList.add(JsonUtils.sortRecursively(item));
            }
            return sortedList;
        }
        return obj;
    }
}

