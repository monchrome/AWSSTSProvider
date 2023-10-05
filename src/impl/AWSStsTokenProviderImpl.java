package impl;

import api.AWSStsTokensProvider;
import data.*;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import exceptions.PolicyGenerationException;
import exceptions.PolicyTemplateNotFoundException;
import exceptions.StsOperationException;
import exceptions.VelocityTemplateRaisedException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.vault.support.VaultResponse;
import vault.VaultClient;

import static java.util.stream.Collectors.*;


public class AWSStsTokenProviderImpl implements AWSStsTokensProvider {

    private static final String CUSTOM_EXCEPTION_HANDLER = "customExceptionHandler";

    private static final String FINAL_POLICY_STATEMENT_FORMAT =
            "{\n" +
                    "  \"Version\": \"2023-10-04\",\n" +
                    "  \"Statement\": [\n" +
                    "      %s\n" +
                    "    ]\n" +
                    "}";

    private static final String KMS_TEMPLATE_NAME = "kms";

    private VelocityEngine velocityEngine;
    private VaultClient vaultClient;

    private final String STS_TOKEN_RESPONSE_ACCESS_KEY = "access_key";
    private final String STS_TOKEN_RESPONSE_SECRET_KEY = "secret_key";
    private final String STS_TOKEN_RESPONSE_TOKEN = "security_token";

    public AWSStsTokenProviderImpl() {
        // Ideally setup velocity engine config, startup and initialization
        // via your spring boot / app start up configuration files
        velocityEngine = new VelocityEngine();
        velocityEngine.init();
    }
    public AWSSTSToken getSTSToken(List<PolicyParameters> policyParametersList, int timeToLiveInMinutes, String region)
            throws StsOperationException, PolicyGenerationException
    {
        if (policyParametersList == null || policyParametersList.size() == 0 || timeToLiveInMinutes <= 0 ) {
            // In production standard code, need to throw an exception below
            return null;
        }

        /*  Typically, an authN plus authZ service will establish  application context,
        *  internal/external service or caller identity, tenant/customer/user id (who owns the
        * data in AWS S3 store and is trying to get a token through this code). InjectableAuthContextParams is
        * a data structure used to store populate values from auth context established per request. These represent
        * values that can be used to cross verify STS token request is generated for same customer and same ai product
        * as the initial auth context established.
        * A malicious scenario can be initial request was to say access a model for tenant A, and auth context was
        * hence established for tenant A, but the final STS token request is attempting to acquire a STS token (which
        * is your final data access token), to access tenant B's data in S3 store.
        *
        * Here, for example's sake, I am directly populating an instance of InjectableAuthContextParams.
        * Note depending upon your AI business scenarios, isolation may be required only across tenants  or
        * across users from a single tenant ( in case each user is allowed to have their own personal data models) ,
        * or both. Additionally, data access isolation may be required across different AI products/features
        * even if it's the same tenant's data used for all of those AI products/features.

        * eg. : InjectableAuthContextParams authCtxData = new InjectableAuthContextParams("tenant123",
                "", "datalakeService", "SomeToneRecognitionEngine");

        * eg. : InjectableAuthContextParams authCtxData = new InjectableAuthContextParams("",
                "userA", "modelService", "SomeTopicRecommendationEngine");

        * eg. : InjectableAuthContextParams authCtxData = new InjectableAuthContextParams("tenant333",
                "userB", "orchestrationService", "SomeSalesInsightsGenerator");
        *  Note all product names, service names or app context names, used in sample code above
        *  are fake for example's sake , with no intention to reference any particular product/feature
        *  in the market.
        */
        InjectableAuthContextParams authCtxData = InjectableAuthContextParams.builder()
                .tenantIdFromJwt("tenant123")
                .principal("datalakeService")
                .appContext("SomeToneRecognitionEngine").build();
        try {
            // Create policy based on given list of policies and inject auth
            // context provided claims into the policy doc
            String evaluatedPolicy = createPolicy(policyParametersList, authCtxData);

            VaultResponse vaultResponse = vaultClient.getStsToken(UUID.randomUUID().toString(), evaluatedPolicy,
                    timeToLiveInMinutes * 60);
            Map<String, Object> responseData = vaultResponse.getData();

            return AWSSTSToken.builder()
                    .accessKey(String.valueOf(responseData.get(STS_TOKEN_RESPONSE_ACCESS_KEY)))
                    .secretAccessKey(String.valueOf(responseData.get(STS_TOKEN_RESPONSE_SECRET_KEY)))
                    .sessionToken((String.valueOf(responseData.get(STS_TOKEN_RESPONSE_TOKEN))))
                    .leaseExpiration(vaultResponse.getLeaseDuration())
                    .region(region)
                    .build();


        } catch (StsOperationException ex) {
            throw ex;
        } catch (PolicyGenerationException ex) {
            throw ex;
        }
    }

    private String createPolicy(List<PolicyParameters> policyParametersList, InjectableAuthContextParams authCtxData)
     throws PolicyGenerationException {
        // Step 1 : Inject all auth context values into the policy parameters list
        if (authCtxData != null) {
            policyParametersList = injectAuthDataIntoPolicy(policyParametersList, authCtxData);
        }

        // Step 2 : Merge policy params with the velocity template
        String mergedPolicyTemplate = "";
         try {
             mergedPolicyTemplate = generatePolicyFromVelocityTemplate(policyParametersList);
        } catch (Exception ex) {
            // process exception according to your needs
        }

        // Step 3 : Generate json policy
        String policyJsonString = String.format(FINAL_POLICY_STATEMENT_FORMAT, mergedPolicyTemplate);
        String policyJson = "";
        Policy policy;
        try {
            policy = Policy.fromJson(policyJsonString.trim());
            policyJson = generateOptimizedPolicyJson(policy);

        } catch (Exception ex) {
            throw new PolicyGenerationException("Failed to extract statements and generate policy json"
                    + ex.getMessage(), ex);
        }

        return policyJson;
    }

    private List<PolicyParameters> injectAuthDataIntoPolicy(List<PolicyParameters> policyParametersList,
                                                            InjectableAuthContextParams authCtxData) {
        policyParametersList.forEach( pol -> {
            Map<String, Object> newParamsMap = new HashMap<>();
            newParamsMap.put(AuthContextIdentifier.SERVICE_PRINCIPAL_IDENTIFIER, authCtxData.getPrincipal());
            newParamsMap.put(AuthContextIdentifier.APP_CONTEXT_IDENTIFIER, authCtxData.getPrincipal());
            newParamsMap.put(AuthContextIdentifier.TENANT_ID_IDENTIFIER, authCtxData.getTenantIdFromJwt());
            newParamsMap.put(AuthContextIdentifier.USER_ID_IDENTIFIER, authCtxData.getUserIdFromJwt());

            //Merge original and injected params
            newParamsMap.putAll(pol.getParameters());
            pol.setParameters(newParamsMap);
        });

        return policyParametersList;
    }

    private String generatePolicyFromVelocityTemplate(List<PolicyParameters> policyParametersList) {
        String mergedStatements = "";
        try {
            mergedStatements = policyParametersList.stream().map( p -> {
                return mergeParametersInTemplate(p, p.getPolicyName());
            }).collect(joining(","));
        } catch (Exception e) {
            // handle exception
        }
        return mergedStatements;
    }

    private  String mergeParametersInTemplate(PolicyParameters policyParameters, String policyName) {
        StringWriter writer = new StringWriter();

        try {
            PolicyTemplates policyTemplates = PolicyTemplates.fromValue(policyName);
            Template template = velocityEngine.getTemplate(policyTemplates.getTemplatePath());
            VelocityContext context = new VelocityContext(policyParameters.getParameters());
            context.put(CUSTOM_EXCEPTION_HANDLER, new VelocityTemplateRaisedException());
            template.merge(context, writer);
        } catch (PolicyTemplateNotFoundException ex) {
           //  log / rethrow
        }
        catch (VelocityTemplateRaisedException ex) {
            // log / rethrow
        }

        return writer.toString();
    }

    private String generateOptimizedPolicyJson(Policy policy) throws PolicyGenerationException{
        String optimizedPolicyJsonString;
        try {
            List<Statement> optimizedStatements = getOptimizedPolicyStatements(policy);
            policy.setStatements(optimizedStatements);
            policy.getStatements().forEach(statement -> statement.setId(null));
            optimizedPolicyJsonString = policy.toJson();
        }
        catch (Exception ex) {
            throw new PolicyGenerationException("Failed to generate policy json  . " + ex.getMessage(), ex);
        }
        return optimizedPolicyJsonString;
    }

    private static <T> Predicate<T> uniqueByStatementKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private List<Statement> getOptimizedPolicyStatements(Policy policy) {
        Collection<Statement> policyStatements = policy.getStatements();
        AtomicReference<Map<StatementKey, Statement>> statementKeyToStatementMap = new AtomicReference<>();
        // filter and collect unique statements
        List<Statement> filteredStatements = policyStatements.stream()
                .filter(uniqueByStatementKey(StatementKey::new))
                .collect(collectingAndThen(toList(), statementList -> {
                    policyStatements.removeAll(statementList);
                    statementKeyToStatementMap.set(policyStatements.stream().collect(toMap(StatementKey::new, statement -> statement)));
                    return statementList;
                })).stream().map(statement -> {
                    StatementKey statementKey = new StatementKey(statement);
                    if (statementKeyToStatementMap.get().get(statementKey) != null) {
                        statement.getResources().addAll(statementKeyToStatementMap.get().get(statementKey).getResources());
                    }
                    return statement;
                }).collect(toList());
        return filteredStatements;
    }

    private class StatementKey {
        private String effectName;
        private String conditions;
        private String principals;
        private String actions;

        public StatementKey(final Statement statement) {
            this.effectName = statement.getEffect().name();
            this.conditions = statement.getConditions().stream().map(condition -> new StringBuilder()
                    .append(condition.getType())
                    .append(condition.getConditionKey())
                    .append(condition.getValues().stream().collect(joining(","))).toString()).collect(joining(","));
            this.principals = statement.getPrincipals().stream().map(principal -> new  StringBuilder()
                    .append(principal.getProvider())
                    .append(principal.getId()).toString()).collect(joining(","));
            this.actions = statement.getActions().stream().map(Action::getActionName).collect(joining(","));
        }
    }

    // Attach kms policy to grant allow access to kms keys
    String attachKmsPolicy() throws PolicyTemplateNotFoundException {
        StringWriter writer = new StringWriter();
        PolicyTemplates policyTemplates = PolicyTemplates.fromValue(KMS_TEMPLATE_NAME);
        Template template = velocityEngine.getTemplate(policyTemplates.getTemplatePath());
        template.merge(new VelocityContext(), writer);
        return writer.toString();
    }
}
