"""
Step 6 — README.md 릴리즈 노트 주입 + 버전 배지 업데이트

환경변수:
  CURRENT_TAG : 현재 태그 (e.g. "v1.3.0")

입력:  /tmp/release_notes.md  (gen_release_notes.py 출력)
       README.md               (업데이트 대상)

README.md에 아래 두 마커가 반드시 존재해야 합니다:
  <!-- CHANGELOG_START -->
  <!-- CHANGELOG_END -->
"""
import os
import re
import sys

MARKER_START = "<!-- CHANGELOG_START -->"
MARKER_END   = "<!-- CHANGELOG_END -->"

tag     = os.environ["CURRENT_TAG"]   # e.g. "v1.3.0"
version = tag.lstrip("v")             # e.g. "1.3.0"

with open("README.md", encoding="utf-8") as f:
    content = f.read()

# ① 플레이스홀더 존재 확인
missing = [m for m in (MARKER_START, MARKER_END) if m not in content]
if missing:
    print("ERROR: README.md에 아래 마커가 없습니다:")
    for m in missing:
        print(f"  {m}")
    sys.exit(1)

# ② 새 릴리즈 노트를 MARKER_START 바로 뒤에 최신순으로 누적 삽입
with open("/tmp/release_notes.md", encoding="utf-8") as f:
    new_notes = f.read()

insert_pos = content.index(MARKER_START) + len(MARKER_START)
end_pos    = content.index(MARKER_END)
old_logs   = content[insert_pos:end_pos]

content = (
    content[:insert_pos]
    + "\n"
    + new_notes
    + old_logs.lstrip("\n")
    + content[end_pos:]
)

# ③ 버전 배지 치환
#    ![Version](https://img.shields.io/badge/version-X.Y.Z-COLOR)
content = re.sub(
    r"badge/version-\d+\.\d+\.\d+",
    f"badge/version-{version}",
    content,
)

# ④ HTML 주석 마커 치환
#    <!-- APP_VERSION: vX.Y.Z -->
content = re.sub(
    r"<!-- APP_VERSION: v\d+\.\d+\.\d+ -->",
    f"<!-- APP_VERSION: {tag} -->",
    content,
)

with open("README.md", "w", encoding="utf-8") as f:
    f.write(content)

print("README.md 업데이트 완료!")
