/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.dto.middleware;

import java.util.List;
import lombok.Generated;

public class CertDto {
    private String grade;
    private String semester;
    private List<Subject> subjects;

    public CertDto(String grade, String semester, List<Subject> subjects) {
        this.grade = grade;
        this.semester = semester;
        this.subjects = subjects;
    }

    @Generated
    public void setGrade(String grade) {
        this.grade = grade;
    }

    @Generated
    public void setSemester(String semester) {
        this.semester = semester;
    }

    @Generated
    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    @Generated
    public String getGrade() {
        return this.grade;
    }

    @Generated
    public String getSemester() {
        return this.semester;
    }

    @Generated
    public List<Subject> getSubjects() {
        return this.subjects;
    }

    public static class Subject {
        private String name;
        private int credit;
        private String grade;

        public Subject(String name, int credit, String grade) {
            this.name = name;
            this.credit = credit;
            this.grade = grade;
        }

        @Generated
        public String getName() {
            return this.name;
        }

        @Generated
        public int getCredit() {
            return this.credit;
        }

        @Generated
        public String getGrade() {
            return this.grade;
        }

        @Generated
        public void setName(String name) {
            this.name = name;
        }

        @Generated
        public void setCredit(int credit) {
            this.credit = credit;
        }

        @Generated
        public void setGrade(String grade) {
            this.grade = grade;
        }
    }
}

