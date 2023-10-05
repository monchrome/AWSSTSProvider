package data;

public class InjectableAuthContextParams {
    String tenantIdFromJwt;
    String userIdFromJwt;
    String principal;
    String appContext;

    private InjectableAuthContextParams(InjectableAuthContextParams.Builder builder) {
        this.tenantIdFromJwt = builder.tenantIdFromJwt;
        this.userIdFromJwt = builder.userIdFromJwt;
        this.principal = builder.principal;
        this.appContext = builder.appContext;
    }
    public String getTenantIdFromJwt() {
        return this.tenantIdFromJwt;
    }

    public String getPrincipal() {
        return this.principal;
    }

    public String getAppContext() {
        return this.appContext;
    }

    public String getUserIdFromJwt() {
        return this.userIdFromJwt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        String tenantIdFromJwt;
        String userIdFromJwt;
        String principal;
        String appContext;

        public Builder() {
        }

        public Builder tenantIdFromJwt(String tenantIdFromJwt) {
            this.tenantIdFromJwt = tenantIdFromJwt;
            return this;
        }

        public Builder userIdFromJwt(String userIdFromJwt) {
            this.userIdFromJwt = userIdFromJwt;
            return this;
        }

        public Builder principal(String principal) {
            this.principal = principal;
            return this;
        }

        public Builder appContext(String appContext) {
            this.appContext = appContext;
            return this;
        }
        public InjectableAuthContextParams build() {
            return new InjectableAuthContextParams(this);
        }
    }
}


