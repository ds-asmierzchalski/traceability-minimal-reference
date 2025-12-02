package net.catenax.traceability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class EdcSetup {

   private static final Logger log = LoggerFactory.getLogger(EdcSetup.class);

   private static final String ASSETS_PATH = "/v3/assets";
   private static final String POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
   private static final String CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
   private static final String CONTENT_TYPE_JSON = "application/json";
   private static final String POLICY_ID = "traceability-policy";

   private static final String INVESTIGATION_RECEIVE_CONTRACT_ID = "investigation-receive-contract-definition";
   private static final String ALERT_RECEIVE_CONTRACT_ID = "alert-receive-contract-definition";
   private static final String INVESTIGATION_UPDATE_CONTRACT_ID = "investigation-update-contract-definition";
   private static final String ALERT_UPDATE_CONTRACT_ID = "alert-update-contract-definition";

   private static final String INVESTIGATION_RECEIVE_ID = "qualityinvestigationnotification-receive";
   private static final String INVESTIGATION_RECEIVE_TYPE_ID = "cx-taxo:ReceiveQualityInvestigationNotification";
   private static final String ALERT_RECEIVE_ID = "qualityalertnotification-receipt";
   private static final String ALERT_RECEIVE_TYPE_ID = "cx-taxo:ReceiveQualityAlertNotification";
   private static final String INVESTIGATION_UPDATE_ID = "qualityinvestigationnotification-update";
   private static final String INVESTIGATION_UPDATE_TYPE_ID = "cx-taxo:UpdateQualityInvestigationNotification";
   private static final String ALERT_UPDATE_ID = "qualityalertnotification-update";
   private static final String ALERT_UPDATE_TYPE_ID = "cx-taxo:UpdateQualityAlertNotification";

   @Value("${app.base-url}")
   private String baseUrl;

   @Value("${app.api.key}")
   private String apiKey;

   @Value("${app.edc.management-url:https://cac-testbed-edc.int.catena-x.net/management}")
   private String edcManagementUrl;

   @Value("${app.edc.management-api-key}")
   private String edcManagementApiKey;

   private final HttpClient httpClient;

   public EdcSetup() {
      this.httpClient = HttpClient.newBuilder().build();
   }

   /**
    * Sets up the EDC offer by creating asset, policy, and contract definitions
    */
   public void setupTraceabilityEdcOffer() {
      log.info("Setting up traceability EDC offer...");
      try {
         createAsset(INVESTIGATION_RECEIVE_ID, baseUrl + "/api/traceability/qualitynotifications/receive",
               INVESTIGATION_RECEIVE_TYPE_ID);
         createAsset(ALERT_RECEIVE_ID, baseUrl + "/api/traceability/qualitynotifications/receive",
               ALERT_RECEIVE_TYPE_ID);
         createAsset(INVESTIGATION_UPDATE_ID, baseUrl + "/api/traceability/qualitynotifications/update",
               INVESTIGATION_UPDATE_TYPE_ID);
         createAsset(ALERT_UPDATE_ID, baseUrl + "/api/traceability/qualitynotifications/update",
               ALERT_UPDATE_TYPE_ID);
         createPolicyDefinition();
         createContractDefinition(INVESTIGATION_RECEIVE_CONTRACT_ID, INVESTIGATION_RECEIVE_ID);
         createContractDefinition(ALERT_RECEIVE_CONTRACT_ID, ALERT_RECEIVE_ID);
         createContractDefinition(INVESTIGATION_UPDATE_CONTRACT_ID, INVESTIGATION_UPDATE_ID);
         createContractDefinition(ALERT_UPDATE_CONTRACT_ID, ALERT_UPDATE_ID);
      } catch (final IOException e) {
         log.error("I/O error setting up traceability EDC offer: {}", e.getMessage(), e);
      } catch (final InterruptedException e) {
         log.error("Process interrupted while setting traceability up EDC offer: {}", e.getMessage(), e);
         Thread.currentThread().interrupt();
      } catch (final Exception e) {
         log.error("Unexpected error setting traceability up EDC offer: {}", e.getMessage(), e);
      }
   }

   private void createAsset(final String assetId, final String baseUrl, final String dctTypeId)
         throws IOException, InterruptedException {
      final String assetJson = getAssetJson(assetId, baseUrl, dctTypeId);
      final HttpResponse<String> response = sendJsonRequest(ASSETS_PATH, assetJson);
      log.info("Asset creation response: {} - {}", response.statusCode(), response.body());
   }

   private void createPolicyDefinition() throws IOException, InterruptedException {
      final String policyJson = getPolicyDefinitionJson();
      final HttpResponse<String> response = sendJsonRequest(POLICY_DEFINITIONS_PATH, policyJson);
      log.info("Policy definition response: {} - {}", response.statusCode(), response.body());
   }

   private void createContractDefinition(final String contractId, final String assetId)
         throws IOException, InterruptedException {
      final String contractJson = getContractDefinitionJson(contractId, assetId);
      final HttpResponse<String> response = sendJsonRequest(CONTRACT_DEFINITIONS_PATH, contractJson);
      log.info("Contract definition response: {} - {}", response.statusCode(), response.body());
   }

   private HttpResponse<String> sendJsonRequest(final String path, final String jsonBody)
         throws IOException, InterruptedException {
      final HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(edcManagementUrl + path))
                                             .header("Content-Type", CONTENT_TYPE_JSON)
                                             .header("X-API-KEY", edcManagementApiKey)
                                             .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                             .build();
      log.info("Sending request to {}{}", edcManagementUrl, path);
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
   }

   private String getAssetJson(final String assetId, final String baseUrl, final String dctTypeId) {
      return """
            {
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "cx-common": "https://w3id.org/catenax/ontology/common#",
                "cx-taxo": "https://w3id.org/catenax/taxonomy#",
                "dct": "http://purl.org/dc/terms/"
              },
              "@type": "Asset",
              "@id": "%s",
              "dataAddress": {
                "@type": "DataAddress",
                "method": "POST",
                "type": "HttpData",
                "baseUrl": "%s",
                "proxyMethod": "true",
                "proxyBody": "true"
              },
              "properties": {
                 "policy-id": "%s",
                 "dct:type": {
                   "@id": "%s"
                 },
              "description": "CAC test asset",
              "contenttype": "application/json",
              "cx-common:version": "1.2"
             },
              "privateProperties": {
                "header:X-API-KEY": "%s"
              }
            }
            """.formatted(assetId, baseUrl, POLICY_ID, dctTypeId, apiKey);
   }

   private String getPolicyDefinitionJson() {
      return """
            {
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "odrl": "http://www.w3.org/ns/odrl/2/",
                "cx-policy": "https://w3id.org/catenax/policy/"
              },
              "@id": "%s",
              "policy": {
                "@type": "odrl:Set",
                "odrl:permission": {
                   "odrl:action": {
                      "odrl:type": {
                        "@id": "http://www.w3.org/ns/odrl/2/use"
                      }
                    },
                   "odrl:constraint": {
                     "odrl:and": [
                       {
                         "odrl:leftOperand": "cx-policy:FrameworkAgreement",
                         "odrl:operator": {
                           "@id": "odrl:eq"
                         },
                         "odrl:rightOperand": "traceability:1.0"
                       },
                       {
                         "odrl:leftOperand": "cx-policy:UsagePurpose",
                         "odrl:operator": {
                           "@id": "odrl:eq"
                         },
                         "odrl:rightOperand": "cx.core.industrycore:1"
                       }
                     ]
                   }
                 },
                "prohibition": [],
                "obligation": []
              }
            }
            """.formatted(POLICY_ID);
   }

   private String getContractDefinitionJson(final String contractDefinitionId, final String assetId) {
      return """
            {
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "@id": "%s",
              "accessPolicyId": "%s",
              "contractPolicyId": "%s",
              "assetsSelector":
              {
                "@type": "CriterionDto",
                "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
                "operator": "=",
                "operandRight": "%s"
              }
            }
            """.formatted(contractDefinitionId, POLICY_ID, POLICY_ID, assetId);
   }
}