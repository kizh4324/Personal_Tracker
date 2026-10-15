import json
from pathlib import Path
from graphify.detect import detect

result = detect(Path('.'))
Path('graphify-out/.graphify_detect.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print("Detection complete:", result['total_files'], "files,", result['total_words'], "words")
