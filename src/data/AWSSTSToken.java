package data;

/* Represents AWS STS Token Format
*  Ref: https://docs.aws.amazon.com/STS/latest/APIReference/API_Credentials.html */

public class AWSSTSToken {
    String accessKey;
    String secretAccessKey;
    String sessionToken;
    String region;
    long leaseExpiration;

    private AWSSTSToken(AWSSTSToken.Builder builder) {
        this.accessKey = builder.accessKey;
        this.secretAccessKey = builder.secretAccessKey;
        this.sessionToken = builder.sessionToken;
        this.region = builder.region;
        this.leaseExpiration = builder.leaseExpiration;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        String accessKey;
        String secretAccessKey;
        String sessionToken;
        String region;
        long leaseExpiration;

        public Builder() {

        }

        public Builder accessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        public Builder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }

        public Builder leaseExpiration(long leaseExpiration) {
            this.leaseExpiration = leaseExpiration;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public AWSSTSToken build() {
            return new AWSSTSToken(this);
        }
    }
}
