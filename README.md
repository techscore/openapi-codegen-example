# 本リポジトリについて

[OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) のカスタマイズのサンプルです。  
OpenAPI Generator のバージョンは 4.2.2、Spring WebFlux のクライアントコードを生成するサンプルとなっています。  
openapi.yml の内容は [Synergy! メールAPI](https://form.synergy-marketing.co.jp/mailapi_v1/openapi3.yml)をベースにした内容となっています。  

## 前提条件

Java と Groovy がインストールされていることを前提としています。  
mac の場合は、 Homebrew で Groovy をインストールすることが可能です。  

```bash
brew install groovy
```

その他のOSの場合は、[こちら](https://groovy-lang.org/install.html) を参照ください。

### 試し方

以下のコマンド実行で、output フォルダにクライアントコードが生成されます。  

```bash
groovy \
  ./techscore-client-codegen.groovy \
  generate \
  -i ./openapi.yml \
  -g TechscoreJavaClientCodegen \
  -o ./output \
  -t ./template \
  --library webclient
```

クライアントコード実行には、Synergy! メールAPI を実行できるAPI認証情報（クライアントID, シークレット）が必要です。  
application.yml の client-id, client-secret を実際の内容に差し替えます。  

```yaml
spring:
  security.oauth2.client:
    provider:
      paas:
        token-uri: https://auth.paas.crmstyle.com/oauth2/token
    registration:
      paas:
        authorization-grant-type: client_credentials
        client-id: your-client-id ★差し替え
        client-secret: your-client-secret ★差し替え
        scope:
          - "mail:send"
          - "mail:result"
        audience: https://mail.paas.crmstyle.com
logging:
  level:
    reactor.netty: DEBUG
```

以下のような main メソッドを持つクラスを output/src/main/java/org/openapitools/client 以下に作成します。（以下はサンプルコードです。）

```java
package org.openapitools.client;

import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.DeliveryRequest;
import org.openapitools.client.model.DeliveryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackages = "org.openapitools.client")
public class SampleApplication implements CommandLineRunner {

    @Autowired
    private DefaultApi defaultApi;

    public static void main(String[]args) throws Exception {
        SpringApplication.run(SampleApplication.class, args);
    }

    //access command line arguments
    @Override
    public void run(String... args) throws Exception {
        String user = "user_example"; // String | user name

        List<Map<String, String>> deliveryData = new ArrayList<>();
        deliveryData.add(new HashMap<String, String>() {
            {put("Customer.synergyid", "1000000");}
            {put("Customer.mailaddress", "<shirakawa.hiroaki@synergy101.jp>");}
            {put("Customer.name", "しらかわひろあき");}
        });

        DeliveryRequest deliveryRequest = new DeliveryRequest()
                .test(false)
                .encode(DeliveryRequest.EncodeEnum.UTF_8)
                .fromAddress("<shirakawa.hiroaki@synergy101.jp>")
                .fromName("カスタマーサポート")
                .replyAddress("<shirakawa.hiroaki@synergy101.jp>")
                .subject("こんにちは($db.Customer.name$)さん")
                .count(1)
                .deliveryMailAddressPropertyName("Customer.mailaddress")
                .bodyText("お問い合わせは次のＵＲＬから <https://www.synergy-marketing.co.jp>")
                .bodyHtml("<html><head></head><body>お問い合わせは次のＵＲＬから<a href=\"https://www.synergy-marketing.co.jp\" symccid=\"0\" symcc=\"true\">フォーム</a></body></html>")
            .deliveryData(deliveryData);
        for (int i = 0; i < 70; i++) {
            try {
                DeliveryResponse result = defaultApi.startMailDelivery(user, deliveryRequest).block();
                System.out.println(result);
            } catch (RestClientException e) {
                System.err.println("Exception when calling DefaultApi#createMailDeliverySetting");
                e.printStackTrace();
            }
            System.out.println("sleep 1 minutes...");
            Thread.sleep(60000L);
        }
    }
}
```

上記で準備完了となります。以下で実行可能となります。

```bash
cd output
chmod +x gradlew
./gradlew bootRun
```
