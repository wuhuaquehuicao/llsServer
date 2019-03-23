package com.lssservlet.datamodel;

public class ADSDbKey {
    public static final String Role_User = "user";
    public static final String Role_Admin = "admin";
    public static final String STORE_MANAGER = "Store Manager";
    public static final String BANK_COMPLIANCE_ANALYST = "Bank Compliance Analyst";
    public static final String BANKING_MANAGER = "Banking Manager";
    public static final String SHIELD_SALES = "Shield Sales";
    public static final String VENDOR = "Vendor";
    public static final String AGENT_IN_CHARGE = "Agent in Charge";
    public static final String CASHIER = "Cashier";
    public static final String SHIELD_COMPLIANCE_OFFICER = "Shield Compliance Officer";
    public static final String BENEFICIAL_OWNER = "Beneficial Owner";

    public static class Column {
        public static final String TINYINT_UNSIGNED_DEFAULT_ZERO = "TINYINT UNSIGNED NOT NULL DEFAULT 0";
        public static final String TINYINT_UNSIGNED_DEFAULT_ONE = "TINYINT UNSIGNED NOT NULL DEFAULT 1";

        public static final String BOOLEAN_DEFAULT_ZERO = "BOOLEAN NOT NULL DEFAULT 0";
        public static final String BOOLEAN_DEFAULT_ONE = "BOOLEAN NOT NULL DEFAULT 1";

        public static final String SMALLINT_UNSIGNED = "SMALLINT UNSIGNED";

        public static final String INT_DEFAULT_NULL = "int DEFAULT NULL";
        public static final String INT_NOT_NULL = "int NOT NULL";
        public static final String INT_DEFAULT_ZERO = "int NOT NULL DEFAULT 0";
        public static final String INT_PRIMARY_KEY = "int NOT NULL PRIMARY KEY AUTO_INCREMENT";

        public static final String BIGINT = "bigint DEFAULT NULL";
        public static final String BIGINT_NOT_NULL = "bigint NOT NULL";
        public static final String BIGINT_DEFAULT_ZERO = "bigint(20) NOT NULL DEFAULT '0'";

        public static final String VARCHAR16 = "varchar(16) DEFAULT NULL";
        public static final String VARCHAR16_NOT_NULL = "varchar(16) NOT NULL";

        public static final String VARCHAR32 = "varchar(32) DEFAULT NULL";
        public static final String VARCHAR32_NOT_NULL = "varchar(32) NOT NULL";
        public static final String VARCHAR64_NOT_NULL_UNIQUE_KEY = "VARCHAR(64) NOT NULL UNIQUE KEY";

        public static final String VARCHAR64 = "varchar(64) DEFAULT NULL";
        public static final String VARCHAR64_NOT_NULL = "varchar(64) NOT NULL";

        public static final String VARCHAR128 = "varchar(128) DEFAULT NULL";
        public static final String VARCHAR128_NOT_NULL = "varchar(128) NOT NULL";

        public static final String VARCHAR256 = "varchar(256) DEFAULT NULL";
        public static final String VARCHAR256_NOT_NULL = "varchar(256) NOT NULL";
        public static final String VARCHAR512 = "varchar(512) DEFAULT NULL";
        public static final String VARCHAR512_NOT_NULL = "varchar(512) NOT NULL";

        public static final String TEXT = "TEXT DEFAULT NULL";
        public static final String TEXT_NOT_NULL = "TEXT NOT NULL";
        public static final String DATETIME = "timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP";// "DATETIME DEFAULT NULL";
        public static final String DATETIME_NOT_NULL = "timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP";;// "DATETIME NOT
                                                                                                       // NULL DEFAULT
                                                                                                       // NOW()";

        public static final String DATE = "DATE DEFAULT NULL";
        public static final String DATE_NOT_NULL = "DATE DEFAULT NOT NULL DEFAULT NOW()";
        public static final String GEO_FIELD = "float(10,7) DEFAULT NULL";

        public static final String TIME = "TIME DEFAULT NULL";

        public static final String FLOAT_RATING = "float(3,1) DEFAULT NULL";
    }

    public enum Type {
        EUser("u"), EAdlist("adl"), ELocation("lo"), EDevice("d"), EAd("ad"), EToken("t"), ELocationAdlist(
                "la"), EAdlistAd("aa"), EExportHistory("eh"), EAdStatic("as");

        private String _type = null;

        Type(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public Integer getSortOrder() {
            return this.ordinal();
        }

        // when server start, check if we need load the data from db
        public Boolean loadCache() {
            switch (this) {
            default:
                return true;
            }
        }

        // check db changed in background, will load it
        public boolean checkDatabaseChanged() {
            switch (this) {
            case EUser:
            case EDevice:
            case ELocation:
            case EAdlist:
            case EAd:
            case EExportHistory:
                return true;
            default: {
                return false;
            }
            }
        }

        public static Type fromString(String type) {
            for (Type t : Type.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("unknown Table: " + type);
        }

        public CacheType getCacheType() {
            switch (this) {
            case EUser:
            case EDevice:
            case EAdlist:
            case EAd:
            case ELocation:
            case ELocationAdlist:
                return CacheType.EKeyMap;
            default:
                return CacheType.EExpireMap;
            }
        }
    };

    public enum CacheType {
        EKeyMap, EExpireMap
    }

    public enum Client {
        EPortal("portal"), EiOS("iOS"), EAndroid("android");
        private String _type = null;

        Client(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static Client fromString(String type) {
            for (Client t : Client.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            return null;
        }
    }

    public enum APIDeviceType {
        EInvalid("invalid"), EAndroid("android"), EIOS("ios");
        private String type;

        private APIDeviceType(String type) {
            this.type = type;
        }

        public String getValue() {
            return type;
        }

        public static APIDeviceType getType(String type) {
            for (APIDeviceType deviceType : APIDeviceType.values()) {
                if (deviceType.getValue().equals(type)) {
                    return deviceType;
                }
            }

            return null;
        }
    }

    public enum APIDeployType {
        EInvalid("invalid"), EDeveloper("developer"), ETestProduct("testproduct"), EProduct("product");
        private String type;

        private APIDeployType(String type) {
            this.type = type;
        }

        public String getValue() {
            return type;
        }

        public static APIDeployType getType(String type) {
            for (APIDeployType deployType : APIDeployType.values()) {
                if (deployType.getValue().equals(type)) {
                    return deployType;
                }
            }
            return null;
        }
    }

    public static enum BatteryHealth {
        EGood("good"), EOverHeat("overheat"), EDead("dead"), EOverVoltage("voltage");

        private String _type = null;

        private BatteryHealth(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static BatteryHealth fromString(String type) {
            for (BatteryHealth t : BatteryHealth.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            return null;
        }
    }

    public static enum BatteryStatus {
        ECharging("charging"), EDischarging("discharging"), ENotCharging("not charging"), EFull("full");

        private String _type = null;

        private BatteryStatus(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static BatteryStatus fromString(String type) {
            for (BatteryStatus t : BatteryStatus.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            return null;
        }
    }

    public enum AdSType {
        ECreatedAt("ca"), ERunningTime("rt"), ETotalTimes("tt"), ETotalDevices("td"), ETotalLocations(
                "tl"), ETotalPauses("tp");
        private String _type = null;

        AdSType(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static AdSType fromString(String type) {
            for (AdSType t : AdSType.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            return null;
        }
    }

    public static class TBase {
        public static final String ID = "id";
        public static final String DATA_TYPE = "data_type";
        public static final String FLAG = "flag";
    }

    public static final String EQUAL = "=";
    public static final String NON_EQUAL = "<>";
    public static final String AND = " AND ";
    public static final String OR = " OR ";

    enum EQueryType {
        EASC("ASC"), EDESC("DESC");
        private String _type;

        private EQueryType(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static EQueryType getOrderType(String type) {
            for (EQueryType t : EQueryType.values()) {
                if (t._type.equals(type)) {
                    return t;
                }
            }

            return null;
        }
    }

    // public enum Provider {
    // EYELP("Yelp"), EGOOGLE("Google"), EFACEBOOK("Facebook");
    // private String _type;
    //
    // private Provider(String type) {
    // _type = type;
    // }
    //
    // public String getValue() {
    // return _type;
    // }
    //
    // public static Provider getType(String type) {
    // for (Provider t : Provider.values()) {
    // if (t._type.equals(type)) {
    // return t;
    // }
    // }
    //
    // return null;
    // }
    // }
    //
    // public enum SqlActionType {
    // EINSERT("insert"), EUPDATE("update"), ESELECT("select");
    // private String _type;
    //
    // private SqlActionType(String type) {
    // _type = type;
    // }
    //
    // public String getValue() {
    // return _type;
    // }
    //
    // public static SqlActionType fromString(String type) {
    // for (SqlActionType t : SqlActionType.values()) {
    // if (t.getValue().equals(type)) {
    // return t;
    // }
    // }
    // return null;
    // }
    // }

    // public static Type TableToModalType(String table) throws DataException {
    // if (table == null || table.isEmpty())
    // throw new DataException(ErrorCode.BAD_REQUEST, "No modal type.");
    // if (table.equals("t_abuseinfo"))
    // return Type.EAbuseInfo;
    // else if (table.equals("t_deal")) {
    // return Type.EDeal;
    // } else if (table.equals("t_likeinfo")) {
    // return Type.ELikeInfo;
    // } else if (table.equals("t_merchant")) {
    // return Type.EMerchant;
    // } else if (table.equals("t_package")) {
    // return Type.EPackage;
    // } else if (table.equals("t_recommendation")) {
    // return Type.ERecommendation;
    // } else if (table.equals("t_review")) {
    // return Type.EReview;
    // } else if (table.equals("t_sharlink")) {
    // return Type.ESharelink;
    // } else if (table.equals("t_transaction")) {
    // return Type.ETransaction;
    // } else if (table.equals("t_user")) {
    // return Type.EUser;
    // } else if (table.equals("t_userdeal")) {
    // return Type.EUserDeal;
    // } else if (table.equals("t_userfollowinfo")) {
    // return Type.EUserFollowInfo;
    // } else {
    // throw new DataException(ErrorCode.BAD_REQUEST, "No found modal type: " + table);
    // }
    // }

    public enum EventType {
        ERunningTime("running_time"), //
        EAppUpgraded("app_ungraded"), EAdlistDownloaded("adlist_downloaded"), EDeviceLocationChanged(
                "device_location_changed"), EDevicePasswordChanged(
                        "device_pwd_changed"), ELocationActiveAdlistChanged("location_adlist_changed");
        private String _type = null;

        EventType(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static EventType fromString(String type) {
            for (EventType t : EventType.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("unknown EventType: " + type);
        }
    }
}
