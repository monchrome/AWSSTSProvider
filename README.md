# AWS STS Provider 
Depicts AWS STS interaction via vault client using dynamic policy generation for AWS STS federation token by generating resource based policies dynamically. Note can be easily extended to obtain STS tokens for assumeRole requests. 

Say if your company provides an AI platform as a service. Say this AI platform, consists of many microservices. Say customer's data is stored on AWS S3 store, which can be accessed by various microservices (within your AI platform), as part of their day to day functioning. Such AI platforms can have the requirement of isolation of data access 
across each request, across tenant, across AI features/products/applications supported by platform. These microservices should be able to access data in AWS s3 store as per incoming requests from individual users/tenants. Cross contamination ( either for read/write operations) should not be possible as a general security requirement.

Example 1 : During execution of various AI model generation flows, Say a DataLake MicroService may need access to tenantA's data based on an incoming api request. Another consumer, AIOrchestration Microservice needs access to tenantB's data based on an incoming api request.. So ideally the STS token generated for honring tenantA's request, should not work in case an attempt is made to use the same STS token to access tenant B's data. Intent of request should match intent of final access granted and final data location accessed in S3 store buckets. 
Security requirement in this case is that if an adversary should not be able to misuse STS tokens in any fashion to cross tenant/user data boundary.
Also functionally and security wise, a STS token's access levels should be limited to what is requested per api call.

Example 2 : Say a user Bob launches a jupyter notebook and launches model generation flows for models to be generated from his data. Another user ALice can launch her jupyter notebook and launches model generation flows for models to be generated from her notebook's data. Security requirements in this case is that Modeling Micro Service should  be granted a STS token that allows it to access user Alice's model. 
Security requirement is to provide ioslation per user's request. STS token generated for honoring Alic's request, should not be usable to access data model generated for Bob, even if the accessing service principal is same "Modeling Micro Service", thereby guaranteeing isolation across requests.

Example 3 : AIOrchestration service may run AI flows for two different AI features, say SomeToneRecognition and SomeProductInsights, in parallel for a single Customer A. Security requirements in this case is that, intent of request actions should match the actions permitted and performed by micro services.
If AIOrchestration flow's instance during runtime acquires STS token for TonalRecognition feature data for Customer A, then that same STS token should not be usable to access 'ProductInsights' feature data, even though AIOrchestration service principal may own the S3 store for both while honoring API request or executing a flow for a given application. AIOrchestration may acquire admin access a separate request to perform administrative actions like adding extra metadata attribute per S3 object. 

Owing to various scenarios cited above, Velocity template policies can be created based per microservice's needs. Note AWS supports both identity and resource based policies.  Velocity Templates can be evaluated dynamically using parameter substitution to generate the usable AWS identity and resource based policies attached per request. Below is an example for a velocity template used for AWS resource access policy generation.

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
    
Note configuring vault for your application (spring boot or otherwise) is not depicted in this repo. 
