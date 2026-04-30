/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataStore {
    private static final Logger logger = LoggerFactory.getLogger(DataStore.class);
    private final Map<String, String> store = new ConcurrentHashMap<String, String>();

    public void save(String studentId, String certificate) {
        this.store.put(studentId, certificate);
    }

    public String consume(String studentId) {
        return this.store.remove(studentId);
    }
}

