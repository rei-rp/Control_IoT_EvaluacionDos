#include <WiFi.h>
#include <WebServer.h>

#define TRIG 5
#define ECHO 18

const char* ssid = "ESP32_INACAP";
const char* password = "12345678";

WebServer server(80);

// Umbral de alerta en cm (la app Android puede cambiarlo con /umbral?cm=XX)
int umbralCm = 20;

// Devuelve la distancia en cm, o -1 si el sensor no recibe eco
float medirDistancia() {
  digitalWrite(TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG, LOW);

  // Timeout de 30 ms (~5 m) para no bloquear el servidor
  long duracion = pulseIn(ECHO, HIGH, 30000);
  if (duracion == 0) return -1;

  return duracion * 0.034 / 2;
}

// ---------- Endpoints para la app Android ----------

// GET /distancia  ->  texto plano, ej: "23.4"  (o "-1.0" sin lectura)
void enviarDistancia() {
  float distancia = medirDistancia();
  server.send(200, "text/plain", String(distancia, 1));
}

// GET /umbral?cm=30  ->  cambia el umbral de alerta
void cambiarUmbral() {
  if (server.hasArg("cm")) {
    int valor = server.arg("cm").toInt();
    if (valor >= 2 && valor <= 400) {
      umbralCm = valor;
    }
  }
  server.send(200, "text/plain", "Umbral: " + String(umbralCm) + " cm");
}

// ---------- Pagina web (se mantiene para probar desde el navegador) ----------

void paginaPrincipal() {
  float distancia = medirDistancia();
  bool hayLectura = (distancia >= 0);

  String pagina = "";
  pagina += "<!DOCTYPE html>";
  pagina += "<html>";
  pagina += "<head>";
  pagina += "<meta charset='UTF-8'>";
  pagina += "<meta http-equiv='refresh' content='2'>"; // recarga la pagina cada 2s
  pagina += "<title>Sensor de Distancia</title>";
  pagina += "<style>";
  pagina += "body{font-family:Arial;text-align:center;margin-top:50px;}";
  pagina += ".valor{font-size:40px;font-weight:bold;}";
  pagina += ".alerta{background:#ff4d4d;color:white;padding:20px;font-size:28px;";
  pagina += "border-radius:10px;margin:20px auto;width:60%;}";
  pagina += "</style>";
  pagina += "</head>";
  pagina += "<body>";
  pagina += "<h1>Sensor de Distancia</h1>";
  pagina += "<p>Distancia</p>";

  if (hayLectura) {
    pagina += "<p class='valor'>" + String(distancia, 1) + " cm</p>";
  } else {
    pagina += "<p class='valor'>-- cm</p>";
  }

  // --- Alerta ---
  if (hayLectura && distancia <= umbralCm) {
    pagina += "<div class='alerta'>⚠️ ¡Objeto muy cerca! (&lt; " + String(umbralCm) + " cm)</div>";
    pagina += "<script>alert('¡Alerta! Objeto detectado a menos de " + String(umbralCm) + " cm.');</script>";
  }

  pagina += "</body>";
  pagina += "</html>";

  server.send(200, "text/html", pagina);
}

void setup() {
  Serial.begin(115200);
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);

  WiFi.softAP(ssid, password);
  Serial.print("IP: ");
  Serial.println(WiFi.softAPIP());

  server.on("/", paginaPrincipal);
  server.on("/distancia", enviarDistancia);
  server.on("/umbral", cambiarUmbral);
  server.begin();
}

void loop() {
  server.handleClient();
}
