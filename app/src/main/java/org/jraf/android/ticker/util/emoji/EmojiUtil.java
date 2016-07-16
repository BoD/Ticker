package org.jraf.android.ticker.util.emoji;

import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.widget.TextView;

import org.jraf.android.ticker.R;

public class EmojiUtil {
    public static final HashMap<String, Integer> DRAWABLE_MAP = new HashMap<>();

    static {
        DRAWABLE_MAP.put("☀", R.drawable.emoji_u2600);
        DRAWABLE_MAP.put("☁", R.drawable.emoji_u2601);
        DRAWABLE_MAP.put("\uD83C\uDF2B", R.drawable.emoji_u1f32b);
        DRAWABLE_MAP.put("\u26F8", R.drawable.emoji_u26f8);
        DRAWABLE_MAP.put("☂", R.drawable.emoji_u2602);
        DRAWABLE_MAP.put("☃", R.drawable.emoji_u2603);
        DRAWABLE_MAP.put("\u26C8", R.drawable.emoji_u26c8);
        DRAWABLE_MAP.put("\uD83D\uDCA8", R.drawable.emoji_u1f4a8);
        DRAWABLE_MAP.put("\uD83C\uDF19", R.drawable.emoji_u1f319);
        DRAWABLE_MAP.put("\uD83C\uDF21", R.drawable.emoji_u1f321);
        DRAWABLE_MAP.put("\uD83C\uDF24", R.drawable.emoji_u1f324);
    }

    public static Spannable replaceEmojis(CharSequence src, TextView textView) {
        SpannableStringBuilder res = new SpannableStringBuilder(src);

        // Do not do anything if version >= Android 4.4
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) return res;

        for (Map.Entry<String, Integer> entry : DRAWABLE_MAP.entrySet()) {
            int index = -1;
            String key = entry.getKey();
            while ((index = TextUtils.indexOf(src, key, index + 1)) != -1) {
                int drawableResId = entry.getValue();
                Drawable icon = ResourcesCompat.getDrawable(textView.getResources(), drawableResId, null);
                assert icon != null;
                int height = textView.getLineHeight();
                icon.setBounds(0, 0, height, height);
                ImageSpan imageSpan = new ImageSpan(icon, DynamicDrawableSpan.ALIGN_BOTTOM);
                res.setSpan(imageSpan, index, index + key.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        return res;
    }
}
