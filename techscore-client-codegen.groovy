@Grab(group = 'org.openapitools', module = 'openapi-generator-cli', version = '4.2.2')
import org.openapitools.codegen.*
import org.openapitools.codegen.languages.*
import io.swagger.v3.oas.models.security.*;

class TechscoreJavaClientCodegen extends JavaClientCodegen {

  static main(String[] args) {
    OpenAPIGenerator.main(args)
  }

  TechscoreJavaClientCodegen() {
    super()
  }

  String name = "techscore-codegen"
  
  @Override
  public void processOpts() {
    final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
    final String apiFolder = (sourceFolder + '/' + apiPackage).replace(".", "/");
    super.processOpts()

    if (WEBCLIENT.equals(getLibrary())) {
      // add WebClientConfig
      supportingFiles.add(new SupportingFile("WebClientConfig.mustache", invokerFolder, "WebClientConfig.java"))
      // add application.yml
      supportingFiles.add(new SupportingFile("application.yml.mustache", projectFolder + '/resources', "application.yml"))
    }
  }

  @Override
  @SuppressWarnings("static-method")
  public List<CodegenSecurity> fromSecurity(Map<String, SecurityScheme> securitySchemeMap) {
      if (securitySchemeMap == null) {
          return Collections.emptyList();
      } 
      List<CodegenSecurity> codegenSecurities = new ArrayList<CodegenSecurity>(securitySchemeMap.size());
      for (String key : securitySchemeMap.keySet()) {
          final SecurityScheme securityScheme = securitySchemeMap.get(key); 
          CodegenSecurity cs = CodegenModelFactory.newInstance(CodegenModelType.SECURITY);
          cs.name = key;
          cs.type = securityScheme.getType().toString();
          cs.isCode = cs.isPassword = cs.isApplication = cs.isImplicit = false;
          cs.isBasicBasic = cs.isBasicBearer = false;
          cs.scheme = securityScheme.getScheme(); 
          if (SecurityScheme.Type.APIKEY.equals(securityScheme.getType())) {
              cs.isBasic = cs.isOAuth = false;
              cs.isApiKey = true;
              cs.keyParamName = securityScheme.getName();
              cs.isKeyInHeader = securityScheme.getIn() == SecurityScheme.In.HEADER;
              cs.isKeyInQuery = securityScheme.getIn() == SecurityScheme.In.QUERY;
              cs.isKeyInCookie = securityScheme.getIn() == SecurityScheme.In.COOKIE;  //it assumes a validation step prior to generation. (cookie-auth supported from OpenAPI 3.0.0)
          } else if (SecurityScheme.Type.HTTP.equals(securityScheme.getType())) {
              cs.isKeyInHeader = cs.isKeyInQuery = cs.isKeyInCookie = cs.isApiKey = cs.isOAuth = false;
              cs.isBasic = true;
              if ("basic".equals(securityScheme.getScheme())) {
                  cs.isBasicBasic = true;
              } else if ("bearer".equals(securityScheme.getScheme())) {
                  cs.isBasicBearer = true;
                  cs.bearerFormat = securityScheme.getBearerFormat();
              }
          } else if (SecurityScheme.Type.OAUTH2.equals(securityScheme.getType())) {
              cs.isKeyInHeader = cs.isKeyInQuery = cs.isKeyInCookie = cs.isApiKey = cs.isBasic = false;
              cs.isOAuth = true;
              final OAuthFlows flows = securityScheme.getFlows();
              if (securityScheme.getFlows() == null) {
                  throw new RuntimeException("missing oauth flow in " + cs.name);
              }
              if (flows.getPassword() != null) {
                  setOauth2Info(cs, flows.getPassword());
                  cs.isPassword = true;
                  cs.flow = "password";
              } else if (flows.getImplicit() != null) {
                  setOauth2Info(cs, flows.getImplicit());
                  cs.isImplicit = true;
                  cs.flow = "implicit";
              } else if (flows.getClientCredentials() != null) {
                  setOauth2Info(cs, flows.getClientCredentials());
                  cs.isApplication = true;
                  cs.flow = "application";
              } else if (flows.getAuthorizationCode() != null) {
                  setOauth2Info(cs, flows.getAuthorizationCode());
                  cs.isCode = true;
                  cs.flow = "accessCode";
              } else {
                  throw new RuntimeException("Could not identify any oauth2 flow in " + cs.name);
              }
          } 
          codegenSecurities.add(cs);
      } 
      // sort auth methods to maintain the same order
      Collections.sort(codegenSecurities, new Comparator<CodegenSecurity>() {
          @Override
          public int compare(CodegenSecurity one, CodegenSecurity another) {
              return ObjectUtils.compare(one.name, another.name);
          }
      });
      // set 'hasMore'
      Iterator<CodegenSecurity> it = codegenSecurities.iterator();
      while (it.hasNext()) {
          final CodegenSecurity security = it.next();
          security.hasMore = it.hasNext();
      } 
      return codegenSecurities;
  }

  private void setOauth2Info(CodegenSecurity codegenSecurity, OAuthFlow flow) {
        codegenSecurity.authorizationUrl = flow.getAuthorizationUrl();
        codegenSecurity.tokenUrl = flow.getTokenUrl();

        if (flow.getScopes() != null && !flow.getScopes().isEmpty()) {
            List<Map<String, Object>> scopes = new ArrayList<Map<String, Object>>();
            int count = 0, numScopes = flow.getScopes().size();
            for (Map.Entry<String, String> scopeEntry : flow.getScopes().entrySet()) {
                Map<String, Object> scope = new HashMap<String, Object>();
                scope.put("scope", scopeEntry.getKey());
                scope.put("description", escapeText(scopeEntry.getValue()));

                count += 1;
                if (count < numScopes) {
                    scope.put("hasMore", "true");
                } else {
                    scope.put("hasMore", null);
                }

                scopes.add(scope);
            }
            codegenSecurity.scopes = scopes;
        }

        if (flow.getExtensions() != null) {
          Map<String, Object> extensions = flow.getExtensions();
          codegenSecurity.vendorExtensions = extensions;
        }
    }
}
