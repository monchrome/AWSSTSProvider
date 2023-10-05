package vault;

import com.amazonaws.util.StringUtils;
import exceptions.StsOperationException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

public class VaultClient {
    public static final String AWS_PATH_FOR_ROLE = "aws-sts/roles/";
    public static final String AWS_PATH_FOR_TOKENS = "aws-sts/sts/";

    private VaultOperations vaultOperations;

    public VaultClient(VaultOperations vaultOperations) {
        this.vaultOperations = vaultOperations;
    }

    public VaultResponse getStsToken(String roleId, String evaluatedPolicy, int ttlInSeconds) throws StsOperationException{
        createAwsRole(roleId, evaluatedPolicy);

        String awsStsTokenPath = new StringBuilder().append(AWS_PATH_FOR_TOKENS).append(roleId).toString();
        Map<String, Integer> tokenRequestBody = new HashMap<>();
        tokenRequestBody.put("ttl", ttlInSeconds);

        try {
            VaultResponse vaultResponse = vaultOperations.write(awsStsTokenPath, tokenRequestBody);
            return vaultResponse;
        } catch (Exception ex) {
            throw new StsOperationException(ex.getMessage());
        } finally {
            deleteAwsRole(roleId);
        }
    }

    private void createAwsRole(String roleId, String evaluatedPolicy) {
        String awsStsRolePath = new StringBuilder().append(AWS_PATH_FOR_ROLE).append(roleId).toString();
        Map<String, String> requestBody = createRequestBody(evaluatedPolicy);
        vaultOperations.write(awsStsRolePath, requestBody);
    }

    private void deleteAwsRole(String roleId) throws StsOperationException {
        try {
            String awsStsRolePath = new StringBuilder().append(AWS_PATH_FOR_ROLE).append(roleId).toString();
            vaultOperations.delete(awsStsRolePath);
        } catch (Exception e) {
            String errorMessage = "Failed to delete AWS role with id =" + roleId;
            throw new StsOperationException(e.getMessage());
        }
    }

    private Map<String, String> createRequestBody(String evaluatedPolicy) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("credential_type", "federation_token");
        if (!StringUtils.isNullOrEmpty(evaluatedPolicy)) {
            requestBody.put("policy_document",  evaluatedPolicy);
        }
        return requestBody;
    }
}
