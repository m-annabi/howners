#!/bin/bash
cd "$(dirname "$0")"

echo "🚀 Démarrage du backend avec configuration SMTP + DocuSign..."

# Exporter les variables d'environnement SMTP
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="annabimountassar@gmail.com"
export SMTP_PASSWORD="mzcelhewcmogcctu"
export EMAIL_FROM="annabimountassar@gmail.com"
export EMAIL_FROM_NAME="Howners"

# Exporter les variables DocuSign
export DOCUSIGN_INTEGRATION_KEY="b40ece38-edbe-4da2-be8d-996f14b15659"
export DOCUSIGN_USER_ID="6736dd95-bc44-4ed8-a282-fae9b2f68d0f"
export DOCUSIGN_ACCOUNT_ID="59d8ff4a-92a9-4790-b3a1-6bf6a1335101"
export DOCUSIGN_BASE_PATH="https://demo.docusign.net/restapi"
export DOCUSIGN_OAUTH_BASE_PATH="https://account-d.docusign.com"
export DOCUSIGN_PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----
MIIEogIBAAKCAQEAgnav/NFwdJsEdcXWnEAqag4kgoFn7LVMMPmB29nKvMxr8Drl
TbubHDVNv2sb8IaJ/2rFYf6P7wDDwMF3LxqjNYznmj1aFLu2wU35TEknL2UFX1gP
s12+NWNG+ZH8V+kGhAoyMAOFJuIPh9E2cVt7pXMALhox9JgrKLc4OlMM0yTWo6Xo
aSrANVwaGSk1hpnddNf/8Fnl1AAGH/QNxhgYx7behBVYElRNXngpICoCvZsM+MdG
3A+bVEWcEtDPoFctmxmc6vWWzcfKXF7UKoO95dFNEVFqt6+tI4MaU+BHNyHhpNdp
yRSoRJZd84RbR6tHcJt0uYCp3/qOvOTg7kko+QIDAQABAoIBAAPgu0Jsh0cwBGXr
x4FFdv1IBlXKuugSS315LR73zkrmWJ7/YSl6VhX0j6cM6/uuRBPPfKZ5BpY4E5l7
JqvDwu4ojgnLoQ3Uto3wX2ShGAUUn6BB4dWXB7xWgxmP+UwzpPnvw6YhtKd/OK70
zQV5jqdQT8UPCkUW8NF5ALbbGRp9hvK5wHTfYqB3hj8AposXwUhpgeVj3RSg/eWC
LRaPPSRpl+HemLT24QGaw4ITcX6jljSkj/pg7kdOlRnsG6Gxj6+LV2WIgJO53GbO
kUDtBEARdIAFCH5s+CBMUCSmNf6oIFMdzK7jxffsNJvDGbTKH7HB/eU61Mmxnn2U
BJYkE8ECgYEAwHxCcjf//iF64s0a+v/1YEu/AKYkXyWzIIuWdHlbcgS11jRmC5wJ
j5tP1xXkYvq334jFC4qwaGV+s+i6SY8sPVJyKbbksOepH1MuD4YGu6vvyafAnpdm
+BUXrFUsaFFpeUNi5b8puzCz7fl86ekd7G3JrJ3ARmn7sCgbMlaL4SUCgYEArYNJ
z7x9Wopl2nKTBPENDB6+R3OBbFUwtF0pN23PnVh1sR7/Yx+cfIQbjug9y6TIQeiM
Zo/OjdeI0xJGJ7xg5q0vNhxsnM0X0VJkzR6ro3aiW8sjZB12oXCpZoJV9FXvNgQi
AyjfP6W7/m/RGyyi2CKp4GQyuAY837zvPt4XckUCgYAV00EZgF4Ha7ZqVwVTJoil
FZGfujbvju/DpfA2XrhOslgH5MMZV+UBooOoCLRvwKvuraxiBaTBAZkfPyk4RAKp
JVsKfdFqeaoEgkL7wZ+r0fALdnjxwosLYCDiCnpRjcGe3VQuZsGNlxUfy6UtRKB+
+WffqbduhH/kVnfW+lc7/QKBgBnTAndSnG2PT511lAAWSFTXdoZ34HlZLIiLnQjM
5SuL8OhNrsD4AXheySM52Yinm86DZ/IT0TA3NfwEKkDbvqhWekJZdKjZtYPH+Yy1
1eHcVEnJMBlEcVWRyQSivVxCNfoaH13NBX0Zk6NIzRx4RjxY3GOzRgExnHnISutW
brLpAoGAWMgpU65u1jECh8izxlWoeNgVoLKBktClGE+q/8o1gx0E2Ur35lvyXeXr
gycMrZP77q26Xv+WCqN/Xg5QUGOohXWC/sRR1q1eOW3ZwkW/HNWGP5pP9NouiEL/
pecrr8ZPRBxva+WXnD/apWbIcdF26375i9pIdBSxCtPDSSrt6oM=
-----END RSA PRIVATE KEY-----"

echo "📧 SMTP Host: $SMTP_HOST"
echo "📧 SMTP User: $SMTP_USERNAME"
echo "🔐 DocuSign Integration Key: $DOCUSIGN_INTEGRATION_KEY"
echo "🔐 DocuSign Account ID: $DOCUSIGN_ACCOUNT_ID"
echo ""

# Utiliser le Maven wrapper
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run
else
    echo "❌ Maven wrapper non trouvé"
    exit 1
fi
