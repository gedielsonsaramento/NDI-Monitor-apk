#!/usr/bin/env python3
"""Extract the unchanged native runtime used by S-Retorno 1.5.1/1.5.2."""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

EXPECTED = {
    "lib/arm64-v8a/libndi.so": "09848dc0a66c79fb4918193517b65ad374af9a4e393db6bfb0c4f4c93ee4fccb",
    "lib/arm64-v8a/libndimonitor.so": "4ee2f3c32c142d21de55584967c8fec6305c1e45ce718848f27367c9d677fd67",
    "lib/armeabi-v7a/libndi.so": "7061bbd25a71ec1538b73bd665bb316d1056d224913ffbce3f3a62b0f71d3679",
    "lib/armeabi-v7a/libndimonitor.so": "6bc0db11e956a2cc768744bc51e006cdad1196dae8b1391c1cf77d9759c6d3d8"
}
ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'local-native')
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as archive:
        data = {}
        for name, expected in EXPECTED.items():
            if archive.getinfo(name).file_size > 32 * 1024 * 1024:
                raise SystemExit('Biblioteca maior que o esperado.')
            value = archive.read(name)
            if hashlib.sha256(value).hexdigest() != expected:
                raise SystemExit('O runtime deste APK não corresponde à revisão 1.5.1/1.5.2: ' + name)
            data[name] = value
        if archive.getinfo('assets/libndi_licenses.txt').file_size > 256 * 1024:
            raise SystemExit('Avisos de licença inválidos.')
        notice = archive.read('assets/libndi_licenses.txt')
        profile = {}
        profile_name = 'assets/html-compat/manifest.json'
        if profile_name in archive.namelist():
            if archive.getinfo(profile_name).file_size > 32768:
                raise SystemExit('Manifesto HTML inválido.')
            manifest = archive.read(profile_name)
            description = json.loads(manifest)
            if description.get('schema') != 1 or len(description.get('resources', [])) != 2:
                raise SystemExit('Perfil HTML desconhecido.')
            profile['html-compat/manifest.json'] = manifest
            for item in description['resources']:
                name = item['asset']
                if name not in ['html-compat/spresenter-legacy.js', 'html-compat/spresenter-legacy.css']:
                    raise SystemExit('Recurso HTML inesperado.')
                if archive.getinfo('assets/' + name).file_size > 2 * 1024 * 1024:
                    raise SystemExit('Recurso HTML grande demais.')
                value = archive.read('assets/' + name)
                if hashlib.sha256(value).hexdigest() != item['assetSha256']:
                    raise SystemExit('Integridade HTML divergente.')
                profile[name] = value
            if archive.getinfo('assets/html-compat/NOTICE.txt').file_size > 256 * 1024:
                raise SystemExit('Avisos HTML inválidos.')
            profile['html-compat/NOTICE.txt'] = archive.read('assets/html-compat/NOTICE.txt')
    for name, value in data.items():
        destination = args.output / name.removeprefix('lib/')
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(value)
    assets = ROOT / 'app/src/main/assets'
    assets.mkdir(parents=True, exist_ok=True)
    (assets / 'libndi_licenses.txt').write_bytes(notice)
    for name, value in profile.items():
        destination = assets / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(value)
    if not profile:
        print('Este APK não contém o perfil HTML. Prepare tools/html-compat antes de empacotar para TV.')
    print('Bibliotecas ARM32 e ARM64 preparadas em ' + str(args.output.resolve()))
    print('Compile: ./gradlew :app:assembleTvboxTest -PmonitorPrebuiltNativeDir=../local-native')

if __name__ == '__main__':
    main()
