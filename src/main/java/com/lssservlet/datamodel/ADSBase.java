package com.lssservlet.datamodel;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

import com.lssservlet.core.DataManager;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class ADSBase extends ADSData {

    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "created_at", columnDefinition = ADSDbKey.Column.DATETIME_NOT_NULL)
    public Long created_at;

    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "updated_at", columnDefinition = ADSDbKey.Column.DATETIME)
    public Long updated_at;

    @Override
    public Long getCreatedTime() {
        return created_at;
    }

    @Override
    public void delete(boolean save) {
        updated_at = DataManager.getInstance().dbtime();
        super.delete(save);
    }
}
