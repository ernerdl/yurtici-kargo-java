#!/usr/bin/env bash
# Keşif aracı: ./kesif.sh test|canli ISLEM alan=deger ...  (çıktı kişisel veri maskeli)
cd "$(dirname "$0")"
exec java -cp target/classes:target/test-classes tr.com.sevinclidoga.yurtici.Kesif "$@"
