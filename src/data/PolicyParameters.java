package data;

import java.util.Map;

/* Class representing STS policy template name and parameters to be used to generate the
   final policy instance from the template.
 */
public class PolicyParameters {
    private String policyName;
    private Map<String, Object> parameters;

    public PolicyParameters(Builder build) {
        this.policyName = build.policyName;
        this.parameters = build.parameters;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public static class Builder {
        private String policyName;
        private Map<String, Object> parameters;

        public Builder(String policyName, Map<String, Object> parameters) {
            this.policyName = policyName;
            this.parameters = parameters;
        }

        public PolicyParameters build() {
            return new PolicyParameters(this);
        }
    }
}
