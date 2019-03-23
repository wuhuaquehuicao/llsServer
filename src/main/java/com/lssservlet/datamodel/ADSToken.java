package com.lssservlet.datamodel;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.Codec;
import com.lssservlet.utils.JCModel;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.JsonField;

@SuppressWarnings("serial")
@Entity
@Access(AccessType.FIELD)
@Table(name = "t_token")
@JCModel(type = Type.EToken)
public class ADSToken extends ADSData {
    public static class TokenValue implements Serializable {
        public String user_id;
    }

    @Converter(autoApply = true)
    public static class TokenValueConverter implements AttributeConverter<TokenValue, String> {
        @Override
        public String convertToDatabaseColumn(TokenValue attribute) {
            return Json.encodeNonNull(attribute);
        }

        @Override
        public TokenValue convertToEntityAttribute(String dbData) {
            try {
                return Json.mapper.readValue(dbData, TokenValue.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Convert(converter = TokenValueConverter.class)
    @Column(columnDefinition = ADSDbKey.Column.TEXT)
    public TokenValue value;

    // @Column(name = "uuid", columnDefinition = ADSDbKey.Column.VARCHAR64)
    // public String uuid;

    @Column(name = "client", columnDefinition = ADSDbKey.Column.VARCHAR32)
    public String client;

    @Transient
    public String token;

    @JsonField(responseFilter = true)
    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "created_at", columnDefinition = ADSDbKey.Column.DATETIME_NOT_NULL)
    public Long created_at;

    @JsonField(responseFilter = true)
    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "updated_at", columnDefinition = ADSDbKey.Column.DATETIME)
    public Long updated_at;

    @JsonField(responseFilter = true)
    @Convert(converter = DateConverter.class, disableConversion = false)
    @Column(name = "expired_at", columnDefinition = ADSDbKey.Column.DATETIME_NOT_NULL)
    public Long expired_at;

    public static String getCacheKey(String id) {
        return getCacheKey(Type.EToken, id);
    }

    public static ADSToken getToken(String id) {
        return getCache(Type.EToken, id);
    }

    @Override
    public void initialize(String createdBy) {
        id = Codec.md5(AlphaId.getUniqueId(Config.getInstance().getServerId()).toString());
        created_at = DataManager.getInstance().dbtime();
    }

    public boolean isExpired() {
        return DataManager.getInstance().time() > expired_at;
    }

    public void setUserId(String user_id) {
        value.user_id = user_id;
    }

    public String getUserId() {
        return value.user_id;
    }

    public ADSUser getUser() {
        try {
            return ADSUser.getUser(value.user_id);
        } catch (Exception e) {
            return null;
        }
    }

    // public HashSet<String> getRoles() {
    // FUser user = getUser();
    // if (user != null) {
    // HashSet<String> roles = user.getRoles();
    // return roles;
    // } else if (id.equals(Config.getInstance().getDefaultAccessToken())) {
    // HashSet<String> roles = new HashSet<>();
    // roles.add(FDbKey.SUPER_ADMIN);
    // return roles;
    // }
    //
    // return null;
    // }

    @Override
    protected void addCacheRelationship() {
        super.addCacheRelationship();

        // if (!hasExpired()) {
        // FUser user = getUser();
        // if (user != null) {
        // // user.token = id;
        // // user.update(true);
        // }
        // }
    }

    @Override
    protected void removeCacheRelationship() {
        super.removeCacheRelationship();

        // FUser user = getUser();
        // if (user != null) {
        // // user.token = null;
        // // user.update(true);
        // }
    }

    public String getDisplayInfo() {
        // if (getUserId() != null) {
        // FUser user = getUser();
        // // if (user != null)
        // // return "user:" + user.nickname + "[" + user.getId() + "]";
        // }
        // if (getId().equals(Config.getInstance().getDefaultAccessToken())) {
        // return "default";
        // }
        return "unknown";
    }

    // @Override
    // public String entityName() {
    // return "token";
    // };
}
