const http = require("node:http");

const PORT = process.env.PORT || 9000;

const server = http.createServer((req, res) => {
  let body = "";
  req.on("data", (chunk) => {
    body += chunk;
  });
  req.on("end", () => {
    console.log(JSON.stringify({
      timestamp: new Date().toISOString(),
      level: "info",
      service: "webhook-receiver",
      message: "alert webhook received",
      payload: body.slice(0, 4000),
    }));
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ received: true }));
  });
});

server.listen(PORT, () => {
  console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    level: "info",
    service: "webhook-receiver",
    message: `listening on port ${PORT}`,
  }));
});
