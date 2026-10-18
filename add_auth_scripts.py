import os
import re

html_dir = r"c:\Users\khush\Downloads\kk\SmartAgri_3_Language_Updated\SmartAgri_FINAL\SmartAgri_FINAL\frontend\HTML"
target_files = [
    'admin-returns.html',
    'admin-support.html',
    'customer-invoice.html',
    'customer-order-tracking.html',
    'fulfillment.html',
    'procurement.html',
    'returns.html',
    'support.html',
    'transfers.html',
    'warehouses.html'
]

for filename in target_files:
    filepath = os.path.join(html_dir, filename)
    if not os.path.exists(filepath):
        print(f"File not found: {filename}")
        continue
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    if "auth.js" in content:
        print(f"Already contains auth.js: {filename}")
        continue
    
    # Try inserting before the first script src="../JS/
    match = re.search(r'(<script\s+src=[\'"]\.\./JS/)', content, re.IGNORECASE)
    if match:
        idx = match.start()
        new_content = content[:idx] + '<script src="../JS/auth.js"></script>\n    ' + content[idx:]
    else:
        # Insert before </body>
        idx = content.lower().rfind('</body>')
        if idx != -1:
            new_content = content[:idx] + '    <script src="../JS/auth.js"></script>\n' + content[idx:]
        else:
            new_content = content + '\n<script src="../JS/auth.js"></script>'
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(new_content)
    print(f"Added auth.js to: {filename}")
