/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.dto.middleware;

import java.util.List;
import lombok.Generated;

public class StudentCertDto {
    private String studentNo;
    private int year;
    private int semester;
    private List<Subject> subjects;
    private double GPA;

    public StudentCertDto(String studentNo, int year, int semester, List<Subject> subjects, double GPA) {
        this.studentNo = studentNo;
        this.year = year;
        this.semester = semester;
        this.subjects = subjects;
        this.GPA = GPA;
    }

    @Generated
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    @Generated
    public void setYear(int year) {
        this.year = year;
    }

    @Generated
    public void setSemester(int semester) {
        this.semester = semester;
    }

    @Generated
    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    @Generated
    public void setGPA(double GPA) {
        this.GPA = GPA;
    }

    @Generated
    public String getStudentNo() {
        return this.studentNo;
    }

    @Generated
    public int getYear() {
        return this.year;
    }

    @Generated
    public int getSemester() {
        return this.semester;
    }

    @Generated
    public List<Subject> getSubjects() {
        return this.subjects;
    }

    @Generated
    public double getGPA() {
        return this.GPA;
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

