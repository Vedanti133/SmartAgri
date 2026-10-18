import os
import re

c_dir = r"c:\Users\khush\Downloads\kk\SmartAgri_3_Language_Updated\SmartAgri_FINAL\SmartAgri_FINAL\backend\smartagri-springboot\src\main\java\com\smartagri\controller"

for fname in sorted(os.listdir(c_dir)):
    if not fname.endswith(".java"):
        continue
    filepath = os.path.join(c_dir, fname)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    c_map = re.findall(r'@RequestMapping\("([^"]+)"\)', content)
    base = c_map[0] if c_map else ""
    methods = re.findall(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\("([^"]*)"\))?', content)
    formatted = [f"{m} {base}{p}" for m, p in methods]
    print(f"{fname:32} -> {', '.join(formatted[:3])}{' ...' if len(formatted) > 3 else ''}")

