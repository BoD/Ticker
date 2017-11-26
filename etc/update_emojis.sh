#!/usr/bin/env bash
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u1f4a8.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u1f32b.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u1f319.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u1f321.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u1f324.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u26c8.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u26f8.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u2601.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u2602.svg
wget https://raw.githubusercontent.com/googlei18n/noto-emoji/master/svg/emoji_u2603.svg
rsvg-convert -h 256 emoji_u1f4a8.svg > ../app/src/main/res/drawable-nodpi/emoji_u1f4a8.png
rsvg-convert -h 256 emoji_u1f32b.svg > ../app/src/main/res/drawable-nodpi/emoji_u1f32b.png
rsvg-convert -h 256 emoji_u1f319.svg > ../app/src/main/res/drawable-nodpi/emoji_u1f319.png
rsvg-convert -h 256 emoji_u1f321.svg > ../app/src/main/res/drawable-nodpi/emoji_u1f321.png
rsvg-convert -h 256 emoji_u1f324.svg > ../app/src/main/res/drawable-nodpi/emoji_u1f324.png
rsvg-convert -h 256 emoji_u26c8.svg > ../app/src/main/res/drawable-nodpi/emoji_u26c8.png
rsvg-convert -h 256 emoji_u26f8.svg > ../app/src/main/res/drawable-nodpi/emoji_u26f8.png
rsvg-convert -h 256 emoji_u2601.svg > ../app/src/main/res/drawable-nodpi/emoji_u2601.png
rsvg-convert -h 256 emoji_u2602.svg > ../app/src/main/res/drawable-nodpi/emoji_u2602.png
rsvg-convert -h 256 emoji_u2603.svg > ../app/src/main/res/drawable-nodpi/emoji_u2603.png
rm *.svg