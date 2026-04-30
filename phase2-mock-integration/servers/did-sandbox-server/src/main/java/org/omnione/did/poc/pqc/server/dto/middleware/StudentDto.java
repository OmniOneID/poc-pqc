/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.dto.middleware;

import lombok.Generated;

public class StudentDto {
    private String studentNo;
    private String name;
    private String email;
    private String grade;
    private String entryYear;

    public StudentDto() {
    }

    public StudentDto(String studentNo, String name, String email, String grade, String entryYear) {
        this.studentNo = studentNo;
        this.name = name;
        this.email = email;
        this.grade = grade;
        this.entryYear = entryYear;
    }

    public String toString() {
        return String.format("Student[studentNo=%s, name=%s, eMail=%s, grade=%s, entryYear=%s]", this.studentNo, this.name, this.email, this.grade, this.entryYear);
    }

    @Generated
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setEmail(String email) {
        this.email = email;
    }

    @Generated
    public void setGrade(String grade) {
        this.grade = grade;
    }

    @Generated
    public void setEntryYear(String entryYear) {
        this.entryYear = entryYear;
    }

    @Generated
    public String getStudentNo() {
        return this.studentNo;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getEmail() {
        return this.email;
    }

    @Generated
    public String getGrade() {
        return this.grade;
    }

    @Generated
    public String getEntryYear() {
        return this.entryYear;
    }
}

