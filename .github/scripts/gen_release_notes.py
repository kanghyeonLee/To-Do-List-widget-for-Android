"""
Step 5 — AI API 호출 → 릴리즈 노트 생성

환경변수:
  LLM_API_KEY  : GitHub Secret
  LLM_PROVIDER : 'openai' | 'gemini' (미설정 시 기본값: gemini)
  CURRENT_TAG  : 현재 태그 (e.g. "v1.3.0")

입력:  /tmp/commits.txt  (이전 태그 ~ 현재 태그 사이 커밋 메시지)
출력:  /tmp/release_notes.md
"""
import os
import json
import sys
import urllib.request
import urllib.error
from datetime import datetime, timezone

api_key  = os.environ["LLM_API_KEY"]
provider = (os.environ.get("LLM_PROVIDER") or "gemini").strip().lower()
tag      = os.environ["CURRENT_TAG"]

with open("/tmp/commits.txt", encoding="utf-8") as f:
    commits = f.read().strip()

# ── 프롬프트 ──────────────────────────────────────────────────
PROMPT = f"""당신은 안드로이드 앱 릴리즈 노트 작성 전문가입니다.
아래 git 커밋 메시지들을 분석하여 한국어로 사용자 친화적인 릴리즈 노트를 작성해 주세요.

[커밋 메시지]
{commits}

[출력 형식] — 해당 항목이 없는 섹션은 생략하세요.
### 🚀 새로운 기능
- 항목

### 🐛 버그 수정
- 항목

### 🛠️ 개선 사항
- 항목

### 📦 기타 변경
- 항목

규칙:
- 각 항목은 간결하고 사용자 관점에서 이해하기 쉽게 작성
- 기술적 세부사항보다 기능 변화에 초점
- 출력은 섹션 헤더(### ...)와 항목(-)만으로 구성
- 추가 설명문, 인사말, 전후 문단 없이 바로 출력"""


def call_openai(prompt: str, key: str) -> str:
    url     = "https://api.openai.com/v1/chat/completions"
    payload = json.dumps({
        "model":       "gpt-4o-mini",
        "messages":    [{"role": "user", "content": prompt}],
        "max_tokens":  1024,
        "temperature": 0.3,
    }).encode()
    req = urllib.request.Request(url, data=payload, headers={
        "Content-Type":  "application/json",
        "Authorization": f"Bearer {key}",
    })
    with urllib.request.urlopen(req, timeout=30) as res:
        data = json.loads(res.read())
    return data["choices"][0]["message"]["content"].strip()


def call_gemini(prompt: str, key: str) -> str:
    model = "gemini-2.5-flash"
    url   = (
        "https://generativelanguage.googleapis.com/v1beta"
        f"/models/{model}:generateContent?key={key}"
    )
    payload = json.dumps({
        "contents":         [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.3, "maxOutputTokens": 1024},
    }).encode()
    req = urllib.request.Request(url, data=payload, headers={
        "Content-Type": "application/json",
    })
    with urllib.request.urlopen(req, timeout=30) as res:
        data = json.loads(res.read())
    return data["candidates"][0]["content"]["parts"][0]["text"].strip()


try:
    print(f"Provider: {provider}", flush=True)
    summary = call_openai(PROMPT, api_key) if provider == "openai" else call_gemini(PROMPT, api_key)
except urllib.error.HTTPError as e:
    body = e.read().decode()
    print(f"API Error {e.code}: {body}", flush=True)
    sys.exit(1)

# ── 최종 릴리즈 블록 (날짜 + AI 요약 + 구분선) ──────────────
date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")
block    = f"## {tag} ({date_str})\n\n{summary}\n\n---\n\n"

with open("/tmp/release_notes.md", "w", encoding="utf-8") as f:
    f.write(block)

print("=== Generated Release Notes ===")
print(block)
