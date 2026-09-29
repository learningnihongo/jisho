const http = require('http');
const https = require('https');
const url = require('url');
const fs = require('fs');
const path = require('path');

const PORT = process.env.DEFAULT_APP_PORT || 3000;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

// Helper to fetch from external HTTPS APIs
function fetchJson(targetUrl, headers = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(targetUrl);
    const options = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'GET',
      headers: {
        'User-Agent': 'LensJisho/1.0',
        ...headers
      }
    };
    https.get(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: data });
        }
      });
    }).on('error', reject);
  });
}

// Google Translate API handler
async function translateText(text, targetLang = 'my') {
  if (!text || !text.trim()) return '';
  const encoded = encodeURIComponent(text.trim());
  const translateUrl = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=ja&tl=${targetLang}&dt=t&q=${encoded}`;
  
  try {
    const res = await fetchJson(translateUrl);
    if (res.data && Array.isArray(res.data) && Array.isArray(res.data[0])) {
      return res.data[0].map(item => item[0]).join('').trim();
    }
  } catch (err) {
    console.error('Translation error:', err);
  }
  return '';
}

// Gemini Helper for AI enhancements
async function callGemini(prompt, inlineImage = null) {
  if (!GEMINI_API_KEY) return null;

  const parts = [{ text: prompt }];
  if (inlineImage) {
    parts.push({
      inlineData: {
        mimeType: inlineImage.mimeType || 'image/jpeg',
        data: inlineImage.data
      }
    });
  }

  const payload = JSON.stringify({
    contents: [{ parts }],
    generationConfig: {
      responseMimeType: "application/json",
      temperature: 0.2
    }
  });

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'generativelanguage.googleapis.com',
      path: `/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          const text = parsed.candidates?.[0]?.content?.parts?.[0]?.text;
          if (text) {
            resolve(JSON.parse(text));
          } else {
            resolve(null);
          }
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

// Enhanced OCR with Google Lens style detected box coordinates
async function analyzeImageWithGemini(base64Image, mimeType = 'image/jpeg') {
  if (!GEMINI_API_KEY) {
    return {
      fullText: "日本語の勉強はとても面白いです。桜の花が綺麗に咲いています。",
      words: ["日本語", "勉強", "面白い", "桜", "花", "綺麗", "咲いて"],
      tokens: [
        { word: "日本語", reading: "にほんご", meaning: "Japanese language", box: { ymin: 15, xmin: 8, ymax: 30, xmax: 38 } },
        { word: "勉強", reading: "べんきょう", meaning: "study", box: { ymin: 15, xmin: 42, ymax: 30, xmax: 60 } },
        { word: "面白い", reading: "おもしろい", meaning: "interesting/fun", box: { ymin: 15, xmin: 68, ymax: 30, xmax: 92 } },
        { word: "桜", reading: "さくら", meaning: "cherry blossom", box: { ymin: 50, xmin: 8, ymax: 65, xmax: 22 } },
        { word: "花", reading: "はな", meaning: "flower", box: { ymin: 50, xmin: 26, ymax: 65, xmax: 38 } },
        { word: "綺麗", reading: "きれい", meaning: "beautiful", box: { ymin: 50, xmin: 44, ymax: 65, xmax: 62 } }
      ],
      translationMy: "ဂျပန်စာလေ့လာခြင်းသည် အလွန်စိတ်ဝင်စားဖို့ကောင်းပါသည်။ ချယ်ရီပန်းများ လှပစွာပွင့်နေကြသည်။",
      translationEn: "Studying Japanese is very interesting. The cherry blossoms are blooming beautifully."
    };
  }

  const prompt = `You are a Japanese OCR and Google Lens AI specialist.
Extract all Japanese text from this image. Return a JSON object with:
- "fullText": complete recognized Japanese text
- "words": array of distinct vocabulary words/compounds (e.g. ["日本語", "勉強", "面白い"])
- "tokens": array of key vocabulary tokens with:
    - "word": string
    - "reading": furigana/hiragana reading
    - "meaning": brief English meaning
    - "burmese": brief Myanmar translation
    - "box": approximate percentage bounding box { ymin, xmin, ymax, xmax } from 0 to 100
- "translationMy": natural, accurate Myanmar (Burmese) translation of the text
- "translationEn": natural English translation of the text
Return ONLY valid JSON.`;

  try {
    const res = await callGemini(prompt, { mimeType, data: base64Image });
    if (res) return res;
  } catch (e) {
    console.error('Gemini OCR error:', e);
  }

  return {
    fullText: "",
    words: [],
    tokens: [],
    translationMy: "",
    translationEn: ""
  };
}

// Detailed Kanji Breakdown endpoint
async function getKanjiBreakdown(kanjiChar) {
  const prompt = `Provide detailed Kanji information for the character "${kanjiChar}".
Return a JSON object with:
- "kanji": "${kanjiChar}"
- "onyomi": array of Katakana readings (e.g. ["ニチ", "ジツ"])
- "kunyomi": array of Hiragana readings (e.g. ["ひ", "か"])
- "meanings": array of English meanings
- "burmeseMeaning": accurate Myanmar translation/explanation
- "strokes": number of strokes (integer)
- "jlpt": JLPT level string ("N5", "N4", "N3", "N2", "N1")
- "radical": radical character and meaning
- "examples": array of 2-3 common compound words with reading and meaning [{ "word": "日本", "reading": "にほん", "meaning": "Japan" }]
Return ONLY valid JSON.`;

  try {
    const res = await callGemini(prompt);
    if (res) return res;
  } catch (e) {
    console.error('Kanji breakdown error:', e);
  }
  return null;
}

// Grammar & Sentence Structure Breakdown
async function analyzeGrammar(japaneseSentence) {
  const prompt = `Analyze this Japanese sentence for language learners: "${japaneseSentence}".
Return a JSON object with:
- "sentence": "${japaneseSentence}"
- "romaji": Romanized transcription
- "furigana": Ruby/furigana annotated HTML or tokens [{ "kanji": "日本", "reading": "にほん" }, { "text": "語" }]
- "grammarPoints": array of grammar explanations [{ "pattern": "〜は", "meaning": "Topic marker particle", "explanation": "Marks the topic of the sentence" }]
- "vocabulary": array of words with part of speech [{ "word": "...", "reading": "...", "pos": "...", "meaning": "..." }]
- "nuance": brief explanation of politeness level (Desu/Masu polite, Plain informal, etc.)
Return ONLY valid JSON.`;

  try {
    const res = await callGemini(prompt);
    if (res) return res;
  } catch (e) {
    console.error('Grammar analysis error:', e);
  }
  return null;
}

// HTTP Server
const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Health check
  if (pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', app: 'LensJisho' }));
    return;
  }

  // Jisho API Proxy
  if (pathname === '/api/jisho') {
    const keyword = parsedUrl.query.keyword;
    if (!keyword) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing keyword' }));
      return;
    }

    try {
      const jishoUrl = `https://jisho.org/api/v1/search/words?keyword=${encodeURIComponent(keyword)}`;
      const jishoRes = await fetchJson(jishoUrl);
      
      // Also get Myanmar translation for the word
      const myanmarMeaning = await translateText(keyword, 'my');
      
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        ...jishoRes.data,
        burmeseMeaning: myanmarMeaning
      }));
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
    }
    return;
  }

  // Kanji Breakdown endpoint
  if (pathname === '/api/kanji') {
    const char = parsedUrl.query.char;
    if (!char) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing char parameter' }));
      return;
    }
    try {
      const kanjiData = await getKanjiBreakdown(char);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(kanjiData || { kanji: char, error: "Not found" }));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // Grammar Analysis endpoint
  if (pathname === '/api/grammar') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', async () => {
      try {
        const { sentence } = JSON.parse(body || '{}');
        const grammarData = await analyzeGrammar(sentence || '');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(grammarData || { error: "Grammar analysis unavailable" }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // Google Translate API
  if (pathname === '/api/translate') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', async () => {
      try {
        const { text, targetLang } = JSON.parse(body || '{}');
        const translated = await translateText(text, targetLang || 'my');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          original: text,
          translated: translated,
          targetLang: targetLang || 'my'
        }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // OCR Endpoint
  if (pathname === '/api/ocr') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', async () => {
      try {
        const { image, mimeType } = JSON.parse(body || '{}');
        const cleanBase64 = image.replace(/^data:image\/\w+;base64,/, '');
        const ocrResult = await analyzeImageWithGemini(cleanBase64, mimeType || 'image/jpeg');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(ocrResult));
      } catch (err) {
        console.error('OCR error:', err);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // Static HTML App
  if (pathname === '/' || pathname === '/index.html') {
    const htmlPath = path.join(__dirname, 'public', 'index.html');
    if (fs.existsSync(htmlPath)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(fs.readFileSync(htmlPath));
      return;
    }
  }

  // Default 404
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`LensJisho server running on port ${PORT}`);
});
