# AWS STS Provider 
Depicts AWS STS interaction via vault, by generating dynamic STS policy generation based on api request params, for acquiring AWS STS federation token . Note can be easily extended to obtain STS tokens for assumeRole requests. Actual Vault service config is not covered.

Usage Scenario : Say, your company provides an AI platform as a service. Say this AI platform, consists of many microservices. Say customer's data is stored on AWS S3 store, which can be accessed by various microservices (within your AI platform), as part of their day to day functioning. Data could be raw data collected from customers, or generated data 
 such as AI models. Such AI platforms can have the requirement of isolation of data access 
across each request, across tenant, across AI features/products/applications. These microservices should be able to access and serve data in AWS S3 store  for the requested application/tenant but guarantee isolation of access per request. No cross contamination of data, should be possible.

Example 1 : During execution of various AI model generation flows, Say a DataLake MicroService may need access to tenantA's data based on an incoming api request. Another consumer, AIOrchestration Microservice may need access to tenantB's data based on an incoming api request. So ideally the STS token generated for honoring tenantA's request, should not work in case an attempt is made to use the same STS token to access tenant B's data. Intent of request should match intent of final access granted and final data location accessed in S3 store buckets. 
Security requirement in this case is that an adversary should not be able to misuse STS tokens in any fashion to cross tenant/user data boundary, during the lieftime of the token.
Also functionally and security wise, a STS token's access levels should be limited to what is requested per api call.

Example 2 : Say a user Bob launches a Jupyter notebook and has a need to launch model generation flows, using the data from his notebook. Another user Alice may launch her Jupyter notebook and launch model generation flows for her data. Modeling micro service may request STS tokens from AWS STSPRovider service to get STS data access token.
Security requirement is to provide ioslation per user's request. STS token generated for honoring Alice's request, should not be usable to access data model generated for Bob, and vice versa. 

Example 3 : AIOrchestration service may run AI flows for two different AI features, say "SomeToneRecognition" and "SomeProductInsights", for a single customer "TenantA". Security requirements in this case is that, intent of request actions should match the actions permitted and performed by micro services.
If an AIOrchestration flow's instance, during runtime, acquires STS token for "SomeToneRecognition" application data for TenantA, then that same STS token should not be usable to access 'ProductInsights' feature data, even if the tenant is same, Tenant A. The AIOrchestration should either launch a new flow instance and/or acquire a different STS token by establishing a new  application context for "ProductInsights". The security and functional requirements is again that of isolation of access per application (even for a single tenant) and not cause cross contamination in any form across different applications. 

Velocity Templates can be evaluated dynamically using parameter substitution to generate the usable AWS identity and resource based policies attached per request. Velocity templates are  used to generate STS policy documents on the fly based on api requests. Validations and concentation of request attributes has to be done. 
More so, the need is to ensure that if the AuthN has granted a JWT token for a specific tenant/application context, then that same tenant and application context gets used while
generating STS policies based on request params. Hence tenant context and application context needs to be passed down to STS policy generator.

     ## capture tenantId, app context from JWT and inject it in policy template prior to final policy generation
     #set($tenant_id = ${tenantId.toLowerCase()})
     #set($app_name_bucket = ${appContext.toLowerCase()})
     {
         "Effect": "Allow",
        "Action": [
           "s3:PutObject",
           "s3:GetObject",
            "s3:ListBucket"
         ],
        "Resource": [
          "arn:aws:s3:::${tenant_id}/${app_name_bucket}/*",
          "arn:aws:s3:::${app_name_bucket}/shared/*"
        ]
      }
     
   * Another Example --
   * Note below example also depicts custom exception handling from within the template.
    
    ## capture service principal name from JWT, inject it in velocity template 
    ## and limit usage of below policy ( and thereby permitted S3 actions) to only certain service principal.
    
    #set ($allowedService = "SomeServiceName")
    #if ($principal && ${principal.toLowerCase()} != $allowedService.toLowerCase()})
      set ($msg = "${principal} is not authorized to use this policy.")
      $templateTaskHandler.logCustomException(${msg})
    #end
    #if (!$userId)
      #set ($msg = "UserId is not specified.")
      $templateTaskHandler.logCustomException(${msg})
    #end
    {
    "Effect": "Allow",
    "Action": [
        "s3:ListBucket",
        "s3:GetObject",
        "s3:DeleteObject"
    ],
    "Resource": [
        "arn:aws:s3:::${service_name}/${userId}/${notebooking_bucket}"
    ]
    }
    
The code gives an example of a backend interface that can be plugged into any spring boot / java based application. Actual Vault configuration is not depicted in the code.

AWSSTSProvider/src/api/AWSStsTokenProvider.java ==>  Interface Definition for acquiring STS Tokens

AWSSTSProvider/src/api/AWSStsTokenProviderImpl ==> Implementation of getSTSToken method 

AWSSTSProvider/src/data/ ==> Data model  

AWSSTSProvider/tree/main/src/vault ==> Custom vault client
