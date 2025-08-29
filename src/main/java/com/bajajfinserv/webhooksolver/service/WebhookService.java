package com.bajajfinserv.webhooksolver.service;

import com.bajajfinserv.webhooksolver.dto.WebhookRequest;
import com.bajajfinserv.webhooksolver.dto.WebhookResponse;
import com.bajajfinserv.webhooksolver.dto.SolutionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WebhookService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final WebClient webClient;
    
    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private static final String SUBMIT_SOLUTION_URL = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
    
    // SQL Query for the problem - highest salary not paid on 1st of month
    private static final String SQL_SOLUTION = 
        "SELECT p.AMOUNT AS SALARY, " +
        "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
        "FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365.25) AS AGE, " +
        "d.DEPARTMENT_NAME " +
        "FROM PAYMENTS p " +
        "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
        "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
        "WHERE DAY(p.PAYMENT_TIME) != 1 " +
        "ORDER BY p.AMOUNT DESC " +
        "LIMIT 1;";

    @Autowired
    public WebhookService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting webhook solver application...");
        
        try {
            // Step 1: Generate webhook
            WebhookResponse webhookResponse = generateWebhook();
            
            if (webhookResponse != null) {
                logger.info("Webhook generated successfully");
                logger.info("Webhook URL: {}", webhookResponse.getWebhook());
                
                // Step 2: Submit solution
                submitSolution(webhookResponse.getAccessToken());
            }
        } catch (Exception e) {
            logger.error("Error in webhook solver process: ", e);
        }
    }

    private WebhookResponse generateWebhook() {
        try {
            WebhookRequest request = new WebhookRequest(
                "John Doe", 
                "REG12347", // Odd number - will get Question 1
                "john@example.com"
            );

            WebhookResponse response = webClient.post()
                .uri(GENERATE_WEBHOOK_URL)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .block();

            logger.info("Webhook generation response received");
            return response;
        } catch (Exception e) {
            logger.error("Error generating webhook: ", e);
            return null;
        }
    }

    private void submitSolution(String accessToken) {
        try {
            SolutionRequest solution = new SolutionRequest(SQL_SOLUTION);

            String response = webClient.post()
                .uri(SUBMIT_SOLUTION_URL)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(solution)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            logger.info("Solution submitted successfully");
            logger.info("Response: {}", response);
        } catch (Exception e) {
            logger.error("Error submitting solution: ", e);
        }
    }
}
