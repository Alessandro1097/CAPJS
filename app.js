// cap-server.js

import express from "express";
import Cap from "@cap.js/server";
import bodyParser from "body-parser";
import cors from "cors";


const app = express();
const port = 3001;

app.use(cors());
// Middleware
app.use(bodyParser.json());

// Inizializza il server Cap con storage locale
const cap = new Cap({
  tokens_store_path: ".cap_tokens.json"
});

// Endpoints richiesti dal widget
app.post("/apichallenge", (req, res) => {
  const challenge = cap.createChallenge();
  res.json(challenge);
});

app.post("/apiredeem", async (req, res) => {
  try {
    const result = await cap.redeemChallenge(req.body);
    res.json(result);
  } catch (error) {
    res.status(400).json({ success: false, error: error.message });
  }
});

app.listen(port, () => {
  console.log(`Cap server listening at http://localhost:${port}/api`);
});
