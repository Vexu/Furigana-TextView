package ;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vexu on 1.8.2017.
 */

public class FuriganaView extends TextView {

    private static final String FURIGANA_START = "{"; 
    private static final String FURIGANA_MIDDLE = ";";
    private static final String FURIGANA_END = "}";
    private static final String ITALIC_START = "<i>";
    private static final String ITALIC_END = "</i>";
    private static final String BOLD_START = "<b>";
    private static final String BOLD_END = "</b>";
    private static final String UNDERLINE_START = "<u>";
    private static final String UNDERLINE_END = "</u>";
    private static final String LINE_BREAK = "<br>";
    private static final String BREAK_REGEX = "(<br>|\n)"; //remove line breaks from kanji and furigana
    private static final String TAG_REGEX = "(<i>|</i>|<b>|</b>|<u>|</u>)"; //remove tags from the text and furigana kanji combinations

    private int mTextAlignment = TEXT_ALIGNMENT_TEXT_START;
    private int mMaxLines = -1;
    private int mSideMargins = 0;

    private float mMaxLineWidth;
    private float mLineHeight;
    private float mTextSize;
    private float mFuriganaSize;
    private float mLineSpacing;


    private TextPaint mNormalPaint;
    private TextPaint mFuriganaPaint;
    private List<Line> mLines;
    private String mText;


    public FuriganaView(Context context) {
        super(context);
        initialize(context, null);
    }

    public FuriganaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public FuriganaView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TextPaint textPaint = getPaint();
        mTextSize = textPaint.getTextSize();

        mNormalPaint = new TextPaint(textPaint);
        mFuriganaPaint = new TextPaint(textPaint);
        mFuriganaSize = mTextSize / 2.0f;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FuriganaView);

            mText = a.getString(R.styleable.FuriganaView_android_text);
            mMaxLines = a.getInt(R.styleable.FuriganaView_android_maxLines, -1);

            mTextAlignment = a.getInt(R.styleable.FuriganaView_android_textAlignment, 2);
            mFuriganaSize = a.getDimension(R.styleable.FuriganaView_furiganaSize, mFuriganaSize);
            mLineSpacing = a.getDimension(R.styleable.FuriganaView_android_lineSpacingExtra, mFuriganaSize / 2.0f);

            float marginLeftRight = a.getDimension(R.styleable.FuriganaView_android_layout_marginLeft, 0) + a.getDimension(R.styleable.FuriganaView_android_layout_marginRight, 0);
            float marginEndStart = a.getDimension(R.styleable.FuriganaView_android_layout_marginEnd, 0) + a.getDimension(R.styleable.FuriganaView_android_layout_marginStart, 0);
            mSideMargins = (int) Math.ceil(Math.max(marginEndStart, marginLeftRight));

            a.recycle();
        }

        mFuriganaPaint.setTextSize(mFuriganaSize);
        mLineHeight = mTextSize + mFuriganaSize + mLineSpacing;
    }


    public void setText(String text) {
        mText = text;
        mLines = null;
        requestLayout();
    }

    public void setTextAlignment(int textAlignment) {
        mTextAlignment = textAlignment;
        invalidate();
    }

    public void setTextSize(float size) {
        mNormalPaint.setTextSize(size);
        requestLayout();
    }

    public void setFuriganaSize(float size) {
        mFuriganaPaint.setTextSize(size);
        requestLayout();
    }

    public void setLineSpacing(float spacing) {
        mLineSpacing = spacing;
        mLineHeight = mTextSize + mFuriganaSize + mLineSpacing;
        requestLayout();
    }

    public String getText() {
        return mText;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mLines != null) {
            float y = mLineHeight;
            for (int i = 0; i < mLines.size(); i++) {
                mLines.get(i).onDraw(canvas, y);
                y += mLineHeight;
                if (mMaxLines != -1 && i == mMaxLines - 1)
                    break;
            }
        } else
            super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY && widthSize != 0)
            width = widthSize;
        else if (widthMode == MeasureSpec.AT_MOST)
            width = measureWidth(widthSize - mSideMargins);
        else
            width = measureWidth(-1);

        mMaxLineWidth = (float) width;
        if (width > 0)
            handleText();

        int maxHeight = mLines != null ? (int) Math.ceil((float) mLines.size() * mLineHeight) : 0;

        if (mMaxLines != -1)
            maxHeight = mMaxLines * (int) Math.ceil(mLineHeight);

        if (heightMode == MeasureSpec.EXACTLY)
            height = heightSize;
        else if (heightMode == MeasureSpec.AT_MOST)
            height = maxHeight < heightSize ? maxHeight : heightSize;
        else
            height = maxHeight;

        if (heightMode != MeasureSpec.UNSPECIFIED && maxHeight > heightSize)
            height |= MEASURED_STATE_TOO_SMALL;

        setMeasuredDimension(width, height);
    }

    //measures the longest line in the text
    private int measureWidth(int width) {
        if (mText == null || mText.isEmpty())
            return 0;

        String text = mText.replaceAll(TAG_REGEX, "");
        String normal = "";
        float maxLength = 0.0f;
        float length = 0.0f;

        while (!text.isEmpty()) {
            if (text.indexOf(LINE_BREAK) == 0 || text.indexOf("\n") == 0) {
                length += mNormalPaint.measureText(normal);
                maxLength = length > maxLength ? length : maxLength;
                length = 0.0f;
                text = text.substring(1);
                normal = "";
            } else if (text.indexOf(FURIGANA_START) == 0) {
                if (!text.contains(FURIGANA_MIDDLE) || !text.contains(FURIGANA_END)) {
                    text = text.substring(1);
                    continue;
                }
                int middle = text.indexOf(FURIGANA_MIDDLE);
                int end = text.indexOf(FURIGANA_END);
                if (end < middle) {
                    text = text.substring(1);
                    continue;
                }
                float kanji = mNormalPaint.measureText(text.substring(1, middle));
                float kana = mFuriganaPaint.measureText(text.substring(middle + 1, end));
                text = text.substring(text.indexOf(FURIGANA_END) + 1);
                length += Math.max(kanji, kana);

            } else {
                normal += text.substring(0, 1);
                text = text.substring(1);
            }
        }

        length += mNormalPaint.measureText(normal);
        maxLength = length > maxLength ? length : maxLength;

        int result = (int) Math.ceil(maxLength);
        if (width < 0)
            return result;
        return result < width ? result : width;
    }

    //breaks the text into lines shorter than the maximum length
    private void handleText() {
        if (mText == null || mText.isEmpty())
            return;

        String text = mText;
        mLines = new ArrayList<>();
        boolean isBold = false;
        boolean isItalic = false;
        boolean isUnderlined = false;
        Line line = new Line();
        NormalTextHolder normalHandler = null;

        while (!text.isEmpty()) {
            if (text.indexOf(BOLD_START) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isBold = true;
                text = text.substring(3);

            } else if (text.indexOf(BOLD_END) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isBold = false;
                text = text.substring(4);

            } else if (text.indexOf(ITALIC_START) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isItalic = true;
                text = text.substring(3);

            } else if (text.indexOf(ITALIC_END) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isItalic = false;
                text = text.substring(4);

            } else if (text.indexOf(UNDERLINE_START) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isUnderlined = true;
                text = text.substring(3);

            } else if (text.indexOf(UNDERLINE_END) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                isUnderlined = false;
                text = text.substring(4);

            } else if (text.indexOf(LINE_BREAK) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                text = text.substring(4);
                mLines.add(line);
                line = new Line();

            } else if (text.indexOf("\n") == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                text = text.substring(1);
                mLines.add(line);
                line = new Line();

            } else if (text.indexOf(FURIGANA_START) == 0) {
                if (normalHandler != null)
                    line.add(normalHandler.endText());
                if (!text.contains(FURIGANA_MIDDLE) || !text.contains(FURIGANA_END)) {
                    text = text.substring(1);
                    continue;
                }
                int middle = text.indexOf(FURIGANA_MIDDLE);
                int end = text.indexOf(FURIGANA_END);
                if (end < middle) {
                    text = text.substring(1);
                    continue;
                }

                String kanji = text.substring(1, middle).replaceAll(BREAK_REGEX, "").replaceAll(TAG_REGEX, ""); //remove all tags and line breaks
                String kana = text.substring(middle + 1, end).replaceAll(BREAK_REGEX, "").replaceAll(TAG_REGEX, "");
                text = text.substring(text.indexOf(FURIGANA_END) + 1);

                PairedText pair = new PairedText(kanji, kana, isBold, isItalic, isUnderlined);
                if ((pair.width() + line.width()) > mMaxLineWidth) {
                    if (!line.isEmpty())
                        mLines.add(line);
                    line = new Line();
                }
                line.add(pair);

            } else {
                if (normalHandler == null)
                    normalHandler = new NormalTextHolder();

                if (normalHandler.test(text.substring(0, 1), line)) {
                    line.add(normalHandler.endText());
                    mLines.add(line);
                    line = new Line();
                }

                normalHandler.expand(text.substring(0, 1), isBold, isItalic, isUnderlined);
                text = text.substring(1);
            }
        }
        if (normalHandler != null)
            line.add(normalHandler.endText());

        if (!line.isEmpty())
            mLines.add(line);
    }

    //sets how much space should be in the start of the line
    private float handleNewline(float width) {
        float remainder = mMaxLineWidth - width;
        if (remainder > 0)
            switch (mTextAlignment) {
                case TEXT_ALIGNMENT_CENTER:
                    return remainder / 2.0f;
                case TEXT_ALIGNMENT_TEXT_END:
                    return remainder;
            }
        return 0.0f;
    }

    private class NormalTextHolder {
        private String normal;
        private boolean bold, italic, underlined;

        NormalTextHolder() {
            normal = "";
            bold = false;
            italic = false;
            underlined = false;
        }

        boolean test(String test, Line line) {
            float width = mNormalPaint.measureText(normal + test) + line.width();
            if (test.equals("。")) //reduces lines with just a period
                width -= mTextSize * 0.7f;
            return width > mMaxLineWidth;
        }

        void expand(String text, boolean isBold, boolean isItalic, boolean isUnderlined) {
            normal += text;
            bold = isBold;
            italic = isItalic;
            underlined = isUnderlined;
        }

        PairedText endText() {
            PairedText pair = new PairedText(normal, null, bold, italic, underlined);
            normal = "";
            return pair;
        }
    }

    private class Line {
        private float width;
        private List<PairedText> pairs;

        Line() {
            width = 0.0f;
            pairs = new ArrayList<>();
        }

        boolean isEmpty() {
            return pairs.isEmpty();
        }

        float width() {
            return width;
        }

        void add(PairedText pairText) {
            pairs.add(pairText);
            width += pairText.width();
        }

        void onDraw(Canvas canvas, float y) {
            float x = handleNewline(width);
            for (int i = 0; i < pairs.size(); i++) {
                pairs.get(i).onDraw(canvas, x, y);
                x += pairs.get(i).width();
            }
        }
    }

    private class PairedText {
        private String normalText, furiganaText;
        private float width, normalWidth, furiganaWidth, offset;
        private TextPaint normalPaint, furiganaPaint;


        PairedText(String normal, String furigana, boolean isBold, boolean isItalic, boolean isUnderlined) {
            normalText = normal;
            furiganaText = furigana;
            setPaint(isBold, isItalic, isUnderlined);
        }

        //set paint and calculate spacing between characters
        private void setPaint(boolean bold, boolean italic, boolean underlined) {
            normalPaint = new TextPaint(mNormalPaint);
            normalPaint.setFakeBoldText(bold);
            normalPaint.setUnderlineText(underlined);
            if (italic)
                normalPaint.setTextSkewX(-0.35f);

            normalWidth = normalPaint.measureText(normalText);

            if (furiganaText != null) {
                furiganaPaint = new TextPaint(mFuriganaPaint);
                furiganaPaint.setFakeBoldText(bold);
                if (italic)
                    furiganaPaint.setTextSkewX(-0.35f);

                furiganaWidth = furiganaPaint.measureText(furiganaText);

                if (normalWidth < furiganaWidth) {
                    offset = (furiganaWidth - normalWidth) / (normalText.length() + 1);
                } else {
                    offset = (normalWidth - furiganaWidth) / (furiganaText.length() + 1);
                }

                width = Math.max(normalWidth, furiganaWidth);
            } else
                width = normalWidth;
        }

        float width() {
            return width;
        }

        void onDraw(Canvas canvas, float x, float y) {
            y = y - mLineSpacing;
            if (furiganaText == null) {
                normalPaint.setColor(getCurrentTextColor());
                canvas.drawText(normalText, 0, normalText.length(), x, y, normalPaint);
            } else {
                normalPaint.setColor(getCurrentTextColor());
                furiganaPaint.setColor(getCurrentTextColor());

                //draw kanji and kana and apply spacing
                if(normalWidth < furiganaWidth) {
                    float offsetX = x + offset;
                    for (int i = 0; i < normalText.length(); i++) {
                        canvas.drawText(normalText, i, i + 1, offsetX, y, normalPaint);
                        offsetX += normalPaint.measureText(normalText.substring(i, i + 1)) + offset;
                    }

                    canvas.drawText(furiganaText, 0, furiganaText.length(), x, y - mTextSize, furiganaPaint);
                } else{
                    float offsetX = x + offset;
                    for (int i = 0; i < furiganaText.length(); i++) {
                        canvas.drawText(furiganaText, i, i + 1, offsetX, y - mTextSize, furiganaPaint);
                        offsetX += furiganaPaint.measureText(furiganaText.substring(i, i + 1)) + offset;
                    }

                    canvas.drawText(normalText, 0, normalText.length(), x, y, normalPaint);
                }
            }
        }
    }
}
