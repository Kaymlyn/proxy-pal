package com.knightowlgames.proxypal.datatype;

import javax.persistence.*;

@Entity
@Table(name = "COLLECTION")
public class MagicCard {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "QTY_OWNED")
    private Integer owned;

    @Column(name = "QTY_USED")
    private Integer used;

    @Column(name = "SET_NAME")
    private String setName;

    @Column(name = "COLLECTOR_NUMBER")
    private Integer setId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOwned() {
        return owned;
    }

    public void setOwned(Integer owned) {
        this.owned = owned;
    }

    public Integer getUsed() {
        return used;
    }

    public void setUsed(Integer used) {
        this.used = used;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public Integer getSetId() {
        return setId;
    }

    public void setSetId(Integer setId) {
        this.setId = setId;
    }
}
