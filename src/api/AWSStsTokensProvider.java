package api;

import data.AWSSTSToken;
import data.PolicyParameters;
import exceptions.PolicyGenerationException;
import exceptions.StsOperationException;

import java.util.List;

/* AWS STS Token Provider interface */

public interface AWSStsTokensProvider {
     AWSSTSToken getSTSToken(List<PolicyParameters> policyParametersList, int timeToLiveInMinutes, String region)
             throws StsOperationException, PolicyGenerationException;
}
