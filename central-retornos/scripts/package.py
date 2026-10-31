from pathlib import Path
import json,shutil,zipfile
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'manifest.json').read_text())
out=root/'dist';(out/'ui').mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/'src/code.js',out/'code.js')
for name in ['index.html','ui.js','style.css']:shutil.copyfile(root/'src/ui'/name,out/'ui'/name)
release=root/'release';release.mkdir(exist_ok=True)
dest=release/f"S-Retorno-Central-{manifest['version']}.zip"
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
    for path in [root/'manifest.json',root/'README.md',root/'LICENSE',*out.rglob('*')]:
        if path.is_file():z.write(path,path.relative_to(root))
print(dest)
