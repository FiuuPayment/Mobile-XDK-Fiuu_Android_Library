package com.molpay.molpayxdk.models;

@SuppressWarnings("unused")
public class ConfigurationInfo {
    private final String libAccessKey;
    private final String libSecretKey;
    // private final String googleApiKey;
    private final String googlePlayProjNum;
    private final String attestationHost;
    private final Long attestationRefreshInterval;
    private final Long attestationHostReadTimeout;
    private final Long attestationConnectionTimeout;
    private final Boolean isAttestationStrictHttp;
    private final String attestationHostCertPinning;
    private final String keyLoadingHost;
    private final String keyLoadingHostCertificate;
    private final String keyLoadingCACert;
    private final String mPosHost;
    private final String mPosHostCertPinning;
    private final Long mPosConnectionTimeout;
    private final String uniqueID;
    private final String developerID;
    private final Boolean isProduction;

    private ConfigurationInfo(Builder builder) {
        libAccessKey = builder.libAccessKey;
        libSecretKey = builder.libSecretKey;
        googlePlayProjNum = builder.googlePlayProjNum;
        // googleApiKey = builder.googleApiKey;
        attestationHost = builder.attestationHost;
        attestationRefreshInterval = builder.attestationRefreshInterval;
        attestationHostReadTimeout = builder.attestationHostReadTimeout;
        attestationConnectionTimeout = builder.attestationConnectionTimeout;
        isAttestationStrictHttp = builder.isAttestationStrictHttp;
        attestationHostCertPinning = builder.attestationHostCertPinning;
        keyLoadingHost = builder.keyLoadingHost;
        keyLoadingHostCertificate = builder.keyLoadingHostCertificate;
        keyLoadingCACert = builder.keyLoadingCACert;
        mPosHost = builder.mPosHost;
        mPosHostCertPinning = builder.mPosHostCertPinning;
        mPosConnectionTimeout = builder.mPosConnectionTimeout;
        uniqueID = builder.uniqueID;
        developerID = builder.developerID;
        isProduction = builder.isProduction;
    }

    public String getAccessKey() {
        return libAccessKey;
    }

    public String getSecretKey() {
        return libSecretKey;
    }

       public String getGooglePlayNum() {
        return googlePlayProjNum;
    }

    // public String getGoogleApiKey() {
    //     return googleApiKey;
    // }

    public String getAttestationHost() {
        return attestationHost;
    }

    public Long getAttestationRefreshInterval() {
        return attestationRefreshInterval;
    }

    public Long getAttestationHostReadTimeout() {
        return attestationHostReadTimeout;
    }

    public Long getAttestationConnectionTimeout() {
        return attestationConnectionTimeout;
    }

    public Boolean getAttestationStrictHttp() {
        return isAttestationStrictHttp;
    }

    public String getAttestationHostCertPinning() {
        return attestationHostCertPinning;
    }

    public String getKeyLoadingHost() {
        return keyLoadingHost;
    }

    public String getKeyLoadingHostCertificate() {
        return keyLoadingHostCertificate;
    }

    public String getKeyLoadingCACert() {
        return keyLoadingCACert;
    }

    public String getmPosHost() {
        return mPosHost;
    }

    public String getmPosHostCertPinning() {
        return mPosHostCertPinning;
    }

    public Long getmPosConnectionTimeout() {
        return mPosConnectionTimeout;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public String getDeveloperID() {
        return developerID;
    }

    public Boolean getIsProduction() {
        return isProduction;
    }

    public static final class Builder {
        private String libAccessKey;
        private String libSecretKey;
        private String googlePlayProjNum;
        private String attestationHost;
        private Long attestationRefreshInterval;
        private Long attestationHostReadTimeout;
        private Long attestationConnectionTimeout;
        private Boolean isAttestationStrictHttp;
        private String attestationHostCertPinning;
        private String keyLoadingHost;
        private String keyLoadingHostCertificate;
        private String keyLoadingCACert;
        private String mPosHost;
        private String mPosHostCertPinning;
        private Long mPosConnectionTimeout;
        private String uniqueID;
        private String developerID;
        private Boolean isProduction;

        public Builder() {
        }

        public Builder setLibAccessKey(String val) {
            libAccessKey = val;
            return this;
        }

        public Builder setLibSecretKey(String val) {
            libSecretKey = val;
            return this;
        }

        // public Builder setGoogleApiKey(String val) {
        //     googleApiKey = val;
        //     return this;
        // }

             public Builder setgooglePlayProjNum(String val) {
            googlePlayProjNum = val;
            return this;
        }

        public Builder setAttestationHost(String val) {
            attestationHost = val;
            return this;
        }

        public Builder setAttestationRefreshInterval(Long val) {
            attestationRefreshInterval = val;
            return this;
        }

        public Builder setAttestationHostReadTimeout(Long val) {
            attestationHostReadTimeout = val;
            return this;
        }

        public Builder setAttestationConnectionTimeout(Long val) {
            attestationConnectionTimeout = val;
            return this;
        }

        public Builder setIsAttestationStrictHttp(Boolean val) {
            isAttestationStrictHttp = val;
            return this;
        }

        public Builder setAttestationHostCertPinning(String val) {
            attestationHostCertPinning = val;
            return this;
        }

        public Builder setKeyLoadingHost(String val) {
            keyLoadingHost = val;
            return this;
        }

        public Builder setKeyLoadingHostCertificate(String val) {
            keyLoadingHostCertificate = val;
            return this;
        }

        public Builder setKeyLoadingCACert(String val) {
            keyLoadingCACert = val;
            return this;
        }

        public Builder setMPosHost(String val) {
            mPosHost = val;
            return this;
        }

        public Builder setMPosHostCertPinning(String val) {
            mPosHostCertPinning = val;
            return this;
        }

        public Builder setMPosConnectionTimeout(Long val) {
            mPosConnectionTimeout = val;
            return this;
        }

        public Builder setUniqueID(String val) {
            uniqueID = val;
            return this;
        }

        public Builder setDeveloperID(String val) {
            developerID = val;
            return this;
        }

        public Builder setIsProduction(Boolean val) {
            isProduction = val;
            return this;
        }

        public ConfigurationInfo build() {
            return new ConfigurationInfo(this);
        }
    }
}
