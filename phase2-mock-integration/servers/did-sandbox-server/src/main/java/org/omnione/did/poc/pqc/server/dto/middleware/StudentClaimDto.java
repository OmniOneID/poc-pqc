/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.dto.middleware;

import com.google.gson.annotations.SerializedName;
import lombok.Generated;

public class StudentClaimDto {
    @SerializedName(value="cr.ac.student_id.student_id")
    private String id;
    @SerializedName(value="cr.ac.student_id.photo")
    private String photo;
    @SerializedName(value="cr.ac.student_id.student_firstname")
    private String firstName;
    @SerializedName(value="cr.ac.student_id.student_lastname")
    private String lastName;
    @SerializedName(value="cr.ac.student_id.student_email")
    private String email;
    @SerializedName(value="cr.ac.student_id.id_number")
    private String id_number;
    @SerializedName(value="cr.ac.student_id.emision_date")
    private String emision_date;
    @SerializedName(value="cr.ac.student_id.expiration_date")
    private String expiration_date;
    @SerializedName(value="cr.ac.student_id.carreer")
    private String carreer;
    @SerializedName(value="cr.ac.student_id.university_branch")
    private String university_branch;
    @SerializedName(value="cr.ac.student_id.id_card_photo")
    private String id_card_photo;

    public StudentClaimDto() {
    }

    public StudentClaimDto(String photo, String firstName, String lastName, String email, String id_number, String emision_date, String expiration_date, String carreer, String university_branch, String id_card_photo) {
        this.id = id_number;
        this.photo = photo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.id_number = id_number;
        this.emision_date = emision_date;
        this.expiration_date = expiration_date;
        this.carreer = carreer;
        this.university_branch = university_branch;
        this.id_card_photo = id_card_photo;
    }

    public String toString() {
        return "StudentClaimDto{photo='" + this.photo + "', firstName='" + this.firstName + "', id_number='" + this.id_number + "', emision_date='" + this.emision_date + "', expiration_date='" + this.expiration_date + "', carreer='" + this.carreer + "', university_branch='" + this.university_branch + "'}";
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Generated
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Generated
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Generated
    public void setEmail(String email) {
        this.email = email;
    }

    @Generated
    public void setId_number(String id_number) {
        this.id_number = id_number;
    }

    @Generated
    public void setEmision_date(String emision_date) {
        this.emision_date = emision_date;
    }

    @Generated
    public void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }

    @Generated
    public void setCarreer(String carreer) {
        this.carreer = carreer;
    }

    @Generated
    public void setUniversity_branch(String university_branch) {
        this.university_branch = university_branch;
    }

    @Generated
    public void setId_card_photo(String id_card_photo) {
        this.id_card_photo = id_card_photo;
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public String getPhoto() {
        return this.photo;
    }

    @Generated
    public String getFirstName() {
        return this.firstName;
    }

    @Generated
    public String getLastName() {
        return this.lastName;
    }

    @Generated
    public String getEmail() {
        return this.email;
    }

    @Generated
    public String getId_number() {
        return this.id_number;
    }

    @Generated
    public String getEmision_date() {
        return this.emision_date;
    }

    @Generated
    public String getExpiration_date() {
        return this.expiration_date;
    }

    @Generated
    public String getCarreer() {
        return this.carreer;
    }

    @Generated
    public String getUniversity_branch() {
        return this.university_branch;
    }

    @Generated
    public String getId_card_photo() {
        return this.id_card_photo;
    }
}

