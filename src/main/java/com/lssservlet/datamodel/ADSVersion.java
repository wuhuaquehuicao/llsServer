package com.lssservlet.datamodel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ADSVersion {
    protected static final Logger log = LogManager.getLogger(ADSVersion.class);

    private int _major = -1;
    private int _minor = -1;
    private int _revision = -1;
    private int _build = -1;

    public ADSVersion(int major, int minor, int revision, int build) {
        _major = major;
        _minor = minor;
        _revision = revision;
        _build = build;
    }

    public static ADSVersion createVersion(String value) {
        ADSVersion version = null;
        if (value != null && value.length() > 0) {
            try {
                String[] contents = value.split("\\.");
                if (contents.length >= 3) {
                    if (contents.length == 3)
                        version = new ADSVersion(Integer.parseInt(contents[0]), Integer.parseInt(contents[1]), 0,
                                Integer.parseInt(contents[2]));
                    else
                        version = new ADSVersion(Integer.parseInt(contents[0]), Integer.parseInt(contents[1]),
                                Integer.parseInt(contents[2]), Integer.parseInt(contents[3]));
                }
            } catch (Exception e) {
                log.warn("invalid version number:" + value);
            }
        }

        return version;
    }

    public String version() {
        return toString();
    }

    public int getMajor() {
        return _major;
    }

    public int getMinor() {
        return _minor;
    }

    public int getRevision() {
        return _revision;
    }

    public int getBuild() {
        return _build;
    }

    public boolean lessThanVersion(ADSVersion version) {
        boolean result = true;
        if (version != null) {
            if (_major > version.getMajor() || (_major == version.getMajor() && _minor > version.getMinor())
                    || (_major == version.getMajor() && _minor == version.getMinor()
                            && _revision >= version.getRevision())
                    || (_major == version.getMajor() && _minor == version.getMinor()
                            && _revision == version.getRevision() && _build >= version.getBuild())) {
                result = false;
            }
        }
        return result;
    }

    public boolean needUpgrade(ADSVersion version) {
        return forceUpgrade(version._major, version._minor, version._revision, version._build);
    }

    private boolean forceUpgrade(int major, int minor, int revision, int build) {
        if (major > _major) {
            return false;
        } else if (major < _major) {
            return true;
        } else {
            if (minor > _minor) {
                return false;
            } else if (minor < _minor) {
                return true;
            } else {
                if (revision > _revision) {
                    return false;
                } else if (revision < _revision) {
                    return true;
                } else {
                    if (build >= _build) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "" + _major + "." + _minor + "." + _revision + "." + _build;
    }
}
