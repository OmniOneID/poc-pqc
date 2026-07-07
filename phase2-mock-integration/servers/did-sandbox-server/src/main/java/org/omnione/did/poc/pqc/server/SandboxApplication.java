/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SandboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(SandboxApplication.class, args);
        System.out.println("Server URL : " + ResponseMessage.Common.SERVER_URL);
    }
}

