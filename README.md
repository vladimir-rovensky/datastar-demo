# datastar-demo
PoC trade booking app using only DataStar and Spring Boot.

# development
HTTPS recommended for development, run the following command to generate a keystore.
Make sure JDK bin is on PATH
keytool -genkeypair -alias bookie -keyalg RSA -keysize 2048 -validity 3650 -keystore src/main/resources/keystore.p12 -storetype PKCS12 -storepass changeit -dname "CN=localhost" -ext "SAN=DNS:localhost,IP:127.0.0.1"

# docker
docker build -t bookie .

docker run -p 8080:8080 bookie

# hosting
https://datastar-demo-production.up.railway.app - railway

https://datastar-demo.onrender.com - render

https://p01--datastar-demo--btj9mtm8rgjs.code.run - northflank

https://containers.back4app.com/ - temporary URL only (60 mins)