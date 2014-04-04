package com.metrink.croquet.examples.crm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * An entity that represents a person.
 */
@Entity
@Table(name = "people")
public class PeopleBean implements Serializable, Identifiable {

    private static final long serialVersionUID = 1961422090676348930L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer personId;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String phone;

    @ManyToOne(optional = false)
    @JoinColumn(name = "companyId", nullable = true)
    private CompanyBean company;

    public PeopleBean() {
    }

    @Override
    public Integer getId() {
        return personId;
    }

    public Integer getPersonId() {
        return personId;
    }

    public void setPersonId(final Integer personId) {
        this.personId = personId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public CompanyBean getCompany() {
        return company;
    }

    public void setCompany(final CompanyBean company) {
        this.company = company;
    }
}
