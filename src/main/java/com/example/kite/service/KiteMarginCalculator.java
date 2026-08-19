package com.example.kite.service;


import com.example.kite.dto.MarginData;
import com.example.kite.dto.MarginResponse;
import com.example.kite.dto.MarginResult;
import com.example.kite.exception.KiteClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Component
@Slf4j
public class KiteMarginCalculator {


    private static final String MARGIN_URL =
            "https://api.kite.trade/margins/orders";


    private final KiteConnect kite;


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConfigManagerService configManagerService;


    public KiteMarginCalculator(KiteConnect kite, ConfigManagerService configManagerService) {
        this.kite = kite;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.configManagerService = configManagerService;
    }


    public MarginResult calculateMISMargin(
            String exchange,
            String tradingSymbol,
            String transactionType,
            int quantity,
            double price
    ) {

        try {
            String json = """
                    [
                      {
                        "exchange": "%s",
                        "tradingsymbol": "%s",
                        "transaction_type": "%s",
                        "variety": "regular",
                        "product": "MIS",
                        "order_type": "LIMIT",
                        "quantity": %d,
                        "price": %.2f,
                        "trigger_price": 0
                      }
                    ]
                    """.formatted(
                    exchange,
                    tradingSymbol,
                    transactionType,
                    quantity,
                    price
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MARGIN_URL))
                    .header("X-Kite-Version", "3")
                    .header("Authorization",
                            "token " + kite.getApiKey() + ":" + kite.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                double totalMargin = price / 5;
                double marginPerShare = totalMargin / quantity;

                log.error("Invalid Margin price response, hence returning default margin");
                return new MarginResult(
                        totalMargin,
                        marginPerShare,
                        5,
                        0,
                        0
                );
            }
            log.info("Successful response received from the kite");
            //JsonNode root = objectMapper.readTree(response.body());
            MarginResponse marginResponse = objectMapper.readValue(response.body(), MarginResponse.class);

            if (marginResponse == null || marginResponse.getData() == null || marginResponse.getData().isEmpty()) {

                log.warn("Margin data not found, hence returning default response");
                double totalMargin = price / 5;
                double marginPerShare = totalMargin / quantity;


                return new MarginResult(
                        totalMargin,
                        marginPerShare,
                        5,
                        0,
                        0
                );
            }

            MarginData data = marginResponse.getData().get(0);
            double totalMargin = data.getTotal().doubleValue();
            double leverage = data.getLeverage().doubleValue();
            double var = data.getVar().doubleValue();
            double exposure = data.getExposure().doubleValue();
            double marginPerShare =
                    totalMargin / quantity;

            return new MarginResult(
                    totalMargin,
                    marginPerShare,
                    leverage,
                    var,
                    exposure
            );
        } catch(Exception e) {
         throw new KiteClientException(e.getMessage());
        }
    }
}
