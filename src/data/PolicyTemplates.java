package data;

import exceptions.PolicyTemplateNotFoundException;

/* Enum class to represent policy templates  */

public enum PolicyTemplates {
    ServiceUserTemplate("ServiceUserTemplate", "./exampleTemplates/ServiceUserTemplate.vm"),
    TenantAppTemplate("TenantAppTemplate", "./exampleTemplates/TenantAppTemplate.vm");

    private String templateName;
    private String templatePath;

    private PolicyTemplates(String templateName, String templatePath) {
        this.templateName = templateName;
        this.templatePath = templatePath;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public static  PolicyTemplates fromValue(String policyTemplateName) throws PolicyTemplateNotFoundException {
        for (PolicyTemplates p : PolicyTemplates.values()) {
            if (p.templateName.equals(policyTemplateName)) {
                return p;
            }
        }
        throw new PolicyTemplateNotFoundException(policyTemplateName + " cannot be found.");
    }
}
