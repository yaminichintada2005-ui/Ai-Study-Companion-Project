# AI Study Companion — Groq + Hugging Face setup

Ollama is gone. The app now calls Groq first and can fall back to Hugging Face.
Your database, JWT, spaces, projects, uploads and PDF extraction are untouched.

---

## 1. Get your two API keys

**Groq** — https://console.groq.com/keys → Create API Key → copy the `gsk_...` value.

**Hugging Face** — https://huggingface.co/settings/tokens → New token →
choose **Fine-grained** → tick **"Make calls to Inference Providers"** → copy `hf_...`.

That tick matters. A plain read token returns 401 or 403.

Hugging Face is optional while `ai.provider=groq`. Get it when you turn on fallback.

---

## 2. Export the keys

```bash
export GROQ_API_KEY=gsk_paste_your_real_key_here
export HF_API_KEY=hf_paste_your_real_token_here
```

To make it permanent:

```bash
echo 'export GROQ_API_KEY=gsk_paste_your_real_key_here' >> ~/.bashrc
echo 'export HF_API_KEY=hf_paste_your_real_token_here' >> ~/.bashrc
source ~/.bashrc
```

Verify:

```bash
echo $GROQ_API_KEY
```

You must see your key. If nothing prints, the backend will start but every AI
call fails with "no API key configured".

---

## 3. Build the backend

```bash
cd backend
./mvnw clean package -DskipTests
```

**Expected:** `BUILD SUCCESS`.

This downloads dependencies fresh, because `spring-ai-bom` and
`spring-ai-starter-model-ollama` were removed from `pom.xml`. The first build
will take a couple of minutes.

---

## 4. Run the backend

Run it from the **same terminal** where you exported the keys.

```bash
./mvnw spring-boot:run
```

**Expected in the startup log:**

```
AI provider selected: GROQ
Groq API key present: true
Hugging Face API key present: true
```

If either says `false`, the export did not reach this terminal.

---

## 5. Test Groq on its own, before touching the app

This proves the key and the model work independently of your code:

```bash
curl https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"openai/gpt-oss-20b","messages":[{"role":"user","content":"Say hello in five words."}]}'
```

**Expected:** JSON containing a `"content"` field with a short greeting, in about a second.

- `401` → key wrong or not exported.
- `403` → your account cannot use that model. Try `openai/gpt-oss-120b` in
  `groq.model` inside `application.properties`.

---

## 6. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, log in, pick a project and material, then press
**Generate 5-Question Quiz**.

**Expected:** "Generating Quiz..." for a few seconds, then five questions with
four options each, the correct one highlighted green, and an explanation below.

---

## 7. If something fails

Every error now returns readable JSON instead of a blank 403. Check the browser
console and the backend terminal together.

| What you see | What it means |
|---|---|
| `AI_SERVICE_ERROR` 502 | Groq rejected us. Backend log has the real reason. |
| `AI_SERVICE_ERROR` 503 | Rate limit, timeout, or Groq is down. Try again. |
| `BAD_REQUEST` "No extracted content" | The PDF is a scanned image. Upload a text PDF. |
| `NOT_FOUND` | That material or project does not belong to your account. |
| `ACCESS_DENIED` 403 | A genuine ownership problem in your own code. |

The key point: an AI failure can no longer come back as 403. Before this change,
Groq's 403 looked identical to a Spring Security 403, which is exactly what made
the old bug so confusing.

---

## 8. Turning on the fallback

Once the basic flow works, in `backend/src/main/resources/application.properties`:

```properties
ai.provider=auto
```

Groq is tried first; Hugging Face takes over on rate limits, timeouts and outages.
It deliberately does **not** fall back on a bad model name or a bad key, because
hiding those makes them very hard to find.

---

## 9. Before you push to GitHub

```bash
grep -rnE "gsk_[A-Za-z0-9]|hf_[A-Za-z0-9]{10}" . \
  --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target
```

**Expected:** no output.

If a real key appears, **revoke it immediately** in the Groq or Hugging Face
console. A key that has ever been committed stays in the repository history even
after you delete the line.

Note that `application.properties` still contains your PostgreSQL password in
plain text. That was already the case before this change, but it is worth moving
to an environment variable the same way the API keys work, before the repo goes public.

---

## 10. What changed

### New files

```
backend/src/main/java/com/aiproof/studycompanion/ai/
    AiProvider.java                 the interface everything above talks to
    AiException.java                typed failures, with safe user messages
    OpenAiCompatibleProvider.java   the only class that makes an HTTP call
    GroqAiService.java              primary provider
    HuggingFaceAiService.java       fallback provider
    AiProviderRouter.java           picks the provider, handles fallback

backend/src/main/java/com/aiproof/studycompanion/service/
    AiPromptFactory.java            all prompts, plus PDF size limiting
    QuizJsonFormatter.java          reshapes AI output into the React schema

backend/src/main/java/com/aiproof/studycompanion/exception/
    GlobalExceptionHandler.java     every error becomes readable JSON

.env.example
```

### Replaced

```
backend/pom.xml                     spring-ai + ollama starter removed
backend/src/main/resources/application.properties
backend/.../service/MaterialAiService.java
backend/.../service/AiService.java
backend/.../controller/AiController.java      now grounded on material
backend/.../dto/AiRequest.java                now carries projectId + materialId
backend/.../config/SecurityConfig.java        JSON 401/403 instead of blank body
frontend/src/App.jsx                          reads the 403 body before throwing
```

### Untouched

Entities, repositories, JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
AuthService, AuthController, MaterialService, MaterialUploadService, FileStorageService,
ProjectService, SpaceService, and all controllers except AiController.

### Architecture

```
React
  |
  v
MaterialAiController / AiController
  |
  v
MaterialAiService / AiService      ownership check, content validation
  |
  v
AiPromptFactory                    builds prompt, limits PDF size
  |
  v
AiProviderRouter                   picks provider, handles fallback
  |
  +--> GroqAiService ---------+
  |                           |--> OpenAiCompatibleProvider  (only HTTP code)
  +--> HuggingFaceAiService --+
  |
  v
QuizJsonFormatter                  reshapes into the JSON React expects
```

`MaterialAiService` has no idea Groq exists. Adding a third provider later means
one new class and one line in the router.
